package io.github.mirhamedjz.ehligible.core.presence

import io.github.mirhamedjz.ehligible.core.rules.Presence
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * Decides, for every calendar day, whether the person was in Canada (docs/SPEC.md, "Day verdict").
 *
 * Precedence: the user's answer, then entered trips, then detection (tracking period only), then
 * "present" for days before tracking started and for future days.
 */
class PresenceTimeline(
    private val trackingStart: LocalDate,
    private val today: LocalDate,
    private val logs: Map<LocalDate, DayLog>,
    private val manualTrips: List<Trip>,
    private val userVerdicts: Map<LocalDate, UserVerdict>,
) : Presence {
    private enum class Side { CANADA, FOREIGN }

    private class Day(
        val date: LocalDate,
        val verdict: DayVerdict,
        val firstSide: Side,
        val lastSide: Side,
        val log: DayLog?,
    ) {
        val touchesForeign: Boolean get() = !verdict.countsAsPresent || log?.hasForeignChecks == true
    }

    private val tracked: List<Day> = computeTrackedDays()

    fun verdictOn(date: LocalDate): DayVerdict = trackedDay(date)?.verdict ?: knownDay(date, log = null)!!.verdict

    override fun isPresent(date: LocalDate): Boolean = verdictOn(date).countsAsPresent

    /** Days in the tracking period counted as absent until the user confirms them. */
    fun daysNeedingReview(): List<LocalDate> = tracked.filter { it.verdict == DayVerdict.NEEDS_REVIEW }.map { it.date }

    /** Entered trips plus trips detected in the tracking period, oldest first. */
    fun trips(notes: Map<LocalDate, TripNote> = emptyMap()): List<TimelineTrip> =
        (manualTrips.map(::manualTimelineTrip) + detectedTrips(notes)).sortedBy { it.departure }

    private fun trackedDay(date: LocalDate): Day? {
        if (tracked.isEmpty() || date < trackingStart || date > today) return null
        return tracked[trackingStart.daysUntil(date)]
    }

    /** A day decided without gap inference: by the user, an entered trip, detection, or the default. */
    private fun knownDay(date: LocalDate, log: DayLog?): Day? {
        when (userVerdicts[date]) {
            UserVerdict.PRESENT -> return Day(date, DayVerdict.PRESENT, Side.CANADA, Side.CANADA, log)
            UserVerdict.ABSENT -> return Day(date, DayVerdict.ABSENT, Side.FOREIGN, Side.FOREIGN, log)
            null -> Unit
        }
        if (manualTrips.any { it.isAbsentOn(date) }) {
            return Day(date, DayVerdict.ABSENT, Side.FOREIGN, Side.FOREIGN, log)
        }
        val leaving = manualTrips.any { it.departure == date && it.returnDate != date }
        val returning = manualTrips.any { it.returnDate == date && it.departure != date }
        if (leaving || returning || manualTrips.any { it.departure == date }) {
            val first = if (returning) Side.FOREIGN else Side.CANADA
            val last = if (leaving) Side.FOREIGN else Side.CANADA
            return Day(date, DayVerdict.TRIP_DAY, first, last, log)
        }
        if (log != null && !log.isEmpty) {
            return Day(date, verdictFromChecks(log), log.first!!.side, log.last!!.side, log)
        }
        val inTrackingPeriod = date in trackingStart..today
        return if (inTrackingPeriod) null else Day(date, DayVerdict.ASSUMED_PRESENT, Side.CANADA, Side.CANADA, log)
    }

    private fun verdictFromChecks(log: DayLog): DayVerdict = when {
        log.inside > 0 -> if (log.hasForeignChecks) DayVerdict.TRIP_DAY else DayVerdict.PRESENT

        !log.hasForeignChecks -> DayVerdict.PRESENT

        // only near-border checks on a Canadian network
        log.border == 0 && log.borderForeign == 0 -> DayVerdict.ABSENT

        else -> DayVerdict.NEEDS_REVIEW
    }

    private fun computeTrackedDays(): List<Day> {
        if (today < trackingStart) return emptyList()
        val count = trackingStart.daysUntil(today) + 1
        val known = MutableList(count) { i ->
            val date = trackingStart.plus(i, DateTimeUnit.DAY)
            knownDay(date, logs[date])
        }
        var i = 0
        while (i < count) {
            if (known[i] != null) {
                i++
                continue
            }
            var end = i
            while (end < count && known[end] == null) end++
            val sideBefore = if (i == 0) {
                knownDay(trackingStart.minus(1, DateTimeUnit.DAY), log = null)!!.lastSide
            } else {
                known[i - 1]!!.lastSide
            }
            val sideAfter = if (end == count) null else known[end]!!.firstSide
            val assumePresent = sideBefore == Side.CANADA && (sideAfter == null || sideAfter == Side.CANADA)
            for (k in i until end) {
                val date = trackingStart.plus(k, DateTimeUnit.DAY)
                known[k] = if (assumePresent) {
                    Day(date, DayVerdict.ASSUMED_PRESENT, Side.CANADA, Side.CANADA, log = null)
                } else {
                    Day(date, DayVerdict.NEEDS_REVIEW, Side.FOREIGN, Side.FOREIGN, log = null)
                }
            }
            i = end
        }
        return known.map { it!! }
    }

    private fun manualTimelineTrip(trip: Trip) = TimelineTrip(
        departure = trip.departure,
        returnDate = trip.returnDate,
        source = TripSource.MANUAL,
        manualId = trip.id,
        destination = trip.destination,
        reason = trip.reason,
        absentDays = countAbsentDaysBetween(trip.departure, trip.returnDate),
        isPlanned = trip.departure > today,
    )

    private fun detectedTrips(notes: Map<LocalDate, TripNote>): List<TimelineTrip> {
        val trips = mutableListOf<TimelineTrip>()
        fun covered(day: Day) = manualTrips.any { it.covers(day.date) }
        var i = 0
        while (i < tracked.size) {
            val start = tracked[i]
            if (covered(start) || !start.touchesForeign) {
                i++
                continue
            }
            var end = i
            while (end + 1 < tracked.size && !covered(tracked[end + 1]) &&
                overnightAbroad(tracked[end], tracked[end + 1])
            ) {
                end++
            }
            val last = tracked[end]
            val departure = if (start.verdict.countsAsPresent) start.date else start.date.minus(1, DateTimeUnit.DAY)
            val returnDate = when {
                last.verdict.countsAsPresent -> last.date
                last.date < today -> last.date.plus(1, DateTimeUnit.DAY)
                else -> null
            }
            val chain = tracked.subList(i, end + 1)
            val note = notes[departure]
            trips += TimelineTrip(
                departure = departure,
                returnDate = returnDate,
                source = TripSource.DETECTED,
                countries = chain.flatMap { it.log?.countries.orEmpty() }.toSet(),
                destination = note?.destination.orEmpty(),
                reason = note?.reason.orEmpty(),
                absentDays = chain.count { !it.verdict.countsAsPresent },
                needsReview = chain.any { it.verdict == DayVerdict.NEEDS_REVIEW },
            )
            i = end + 1
        }
        return trips
    }

    /** Whether the person was abroad across the night between [day] and [next]. */
    private fun overnightAbroad(day: Day, next: Day): Boolean =
        (!day.verdict.countsAsPresent || day.lastSide == Side.FOREIGN) &&
            (!next.verdict.countsAsPresent || next.firstSide == Side.FOREIGN)

    private fun countAbsentDaysBetween(departure: LocalDate, returnDate: LocalDate): Int {
        var count = 0
        var day = departure.plus(1, DateTimeUnit.DAY)
        while (day < returnDate) {
            if (!isPresent(day)) count++
            day = day.plus(1, DateTimeUnit.DAY)
        }
        return count
    }

    private val CheckKind.side: Side get() = if (isCanadaSide) Side.CANADA else Side.FOREIGN
}
