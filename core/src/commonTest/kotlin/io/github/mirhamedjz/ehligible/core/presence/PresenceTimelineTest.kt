package io.github.mirhamedjz.ehligible.core.presence

import io.github.mirhamedjz.ehligible.core.presence.CheckKind.BORDER
import io.github.mirhamedjz.ehligible.core.presence.CheckKind.BORDER_FOREIGN
import io.github.mirhamedjz.ehligible.core.presence.CheckKind.INSIDE
import io.github.mirhamedjz.ehligible.core.presence.CheckKind.OUTSIDE
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate

private fun oct(day: Int) = LocalDate(2026, 10, day)

/** Builds a day log from checks in the order they happened; outside checks are in France unless stated. */
private fun log(vararg checks: CheckKind, country: String = "FR"): DayLog =
    checks.fold(DayLog()) { acc, kind -> acc.record(kind, if (kind.isCanadaSide) null else country) }

private fun timeline(
    logs: Map<LocalDate, DayLog>,
    today: LocalDate = oct(20),
    trackingStart: LocalDate = oct(1),
    manualTrips: List<Trip> = emptyList(),
    userVerdicts: Map<LocalDate, UserVerdict> = emptyMap(),
) = PresenceTimeline(trackingStart, today, logs, manualTrips, userVerdicts)

private fun homeEveryDay(except: Set<Int> = emptySet(), until: Int = 20): MutableMap<LocalDate, DayLog> =
    (1..until).filter { it !in except }.associate { oct(it) to log(INSIDE) }.toMutableMap()

class DayVerdictTest {
    @Test
    fun aCheckInCanadaMakesTheDayPresent() {
        assertEquals(DayVerdict.PRESENT, timeline(homeEveryDay()).verdictOn(oct(5)))
    }

    @Test
    fun onlyChecksAbroadMakeTheDayAbsent() {
        val logs = homeEveryDay(except = setOf(5)).apply { put(oct(5), log(OUTSIDE, OUTSIDE)) }
        assertEquals(DayVerdict.ABSENT, timeline(logs).verdictOn(oct(5)))
    }

    @Test
    fun anyCheckInCanadaWinsOverChecksAbroad() {
        val logs = homeEveryDay(except = setOf(5)).apply { put(oct(5), log(INSIDE, OUTSIDE)) }
        val verdict = timeline(logs).verdictOn(oct(5))
        assertEquals(DayVerdict.TRIP_DAY, verdict)
        assertTrue(verdict.countsAsPresent)
    }

    @Test
    fun nearTheBorderWithACanadianNetworkCountsAsPresent() {
        val logs = homeEveryDay(except = setOf(5)).apply { put(oct(5), log(BORDER, BORDER)) }
        assertEquals(DayVerdict.PRESENT, timeline(logs).verdictOn(oct(5)))
    }

    @Test
    fun nearTheBorderOnAForeignNetworkNeedsReview() {
        val logs = homeEveryDay(except = setOf(5)).apply { put(oct(5), log(BORDER_FOREIGN)) }
        val verdict = timeline(logs).verdictOn(oct(5))
        assertEquals(DayVerdict.NEEDS_REVIEW, verdict)
        assertFalse(verdict.countsAsPresent)
    }

    @Test
    fun abroadAndNearTheBorderWithoutACanadianCheckNeedsReview() {
        val logs = homeEveryDay(except = setOf(5)).apply { put(oct(5), log(OUTSIDE, BORDER)) }
        assertEquals(DayVerdict.NEEDS_REVIEW, timeline(logs).verdictOn(oct(5)))
    }
}

class GapInferenceTest {
    @Test
    fun aGapBetweenDaysInCanadaIsAssumedPresent() {
        val t = timeline(homeEveryDay(except = setOf(7, 8)))
        assertEquals(DayVerdict.ASSUMED_PRESENT, t.verdictOn(oct(7)))
        assertEquals(DayVerdict.ASSUMED_PRESENT, t.verdictOn(oct(8)))
    }

    @Test
    fun aGapNextToAbroadNeedsReview() {
        val logs = homeEveryDay(except = setOf(5, 6, 7, 8)).apply {
            put(oct(5), log(INSIDE, OUTSIDE)) // left in the evening
            put(oct(8), log(OUTSIDE)) // abroad, phone silent on 6 and 7
        }
        val t = timeline(logs)
        assertEquals(DayVerdict.NEEDS_REVIEW, t.verdictOn(oct(6)))
        assertEquals(DayVerdict.NEEDS_REVIEW, t.verdictOn(oct(7)))
        assertEquals(DayVerdict.ABSENT, t.verdictOn(oct(8)))
        assertEquals(listOf(oct(6), oct(7)), t.daysNeedingReview())
    }

    @Test
    fun anOvernightGapBetweenATripDayEndingAbroadAndOneStartingAbroadNeedsReview() {
        // Left on the 5th (last check abroad), back on the 9th (first check abroad), silent in between.
        val logs = homeEveryDay(except = setOf(5, 6, 7, 8, 9)).apply {
            put(oct(5), log(INSIDE, OUTSIDE))
            put(oct(9), log(OUTSIDE, INSIDE))
        }
        val t = timeline(logs)
        assertEquals(DayVerdict.NEEDS_REVIEW, t.verdictOn(oct(7)))
    }

    @Test
    fun aGapRunningToTodayAfterCanadaIsAssumedPresent() {
        val t = timeline(homeEveryDay(until = 17))
        assertEquals(DayVerdict.ASSUMED_PRESENT, t.verdictOn(oct(19)))
        assertEquals(DayVerdict.ASSUMED_PRESENT, t.verdictOn(oct(20)))
    }

    @Test
    fun aGapRunningToTodayAfterAbroadNeedsReview() {
        val logs = homeEveryDay(until = 17).apply { put(oct(18), log(OUTSIDE)) }
        assertEquals(DayVerdict.NEEDS_REVIEW, timeline(logs).verdictOn(oct(20)))
    }

    @Test
    fun noChecksSinceTrackingStartedIsAssumedPresent() {
        assertEquals(DayVerdict.ASSUMED_PRESENT, timeline(emptyMap()).verdictOn(oct(10)))
    }
}

class PrecedenceTest {
    @Test
    fun theUsersAnswerWins() {
        val logs = homeEveryDay(except = setOf(5)).apply { put(oct(5), log(BORDER_FOREIGN)) }
        val t = timeline(logs, userVerdicts = mapOf(oct(5) to UserVerdict.PRESENT))
        assertEquals(DayVerdict.PRESENT, t.verdictOn(oct(5)))
        assertTrue(t.daysNeedingReview().isEmpty())
    }

    @Test
    fun aManualTripOverridesDetection() {
        val trip = Trip(id = "t1", departure = oct(3), returnDate = oct(6))
        val t = timeline(homeEveryDay(), manualTrips = listOf(trip))
        assertEquals(DayVerdict.TRIP_DAY, t.verdictOn(oct(3)))
        assertEquals(DayVerdict.ABSENT, t.verdictOn(oct(4)))
        assertEquals(DayVerdict.ABSENT, t.verdictOn(oct(5)))
        assertEquals(DayVerdict.TRIP_DAY, t.verdictOn(oct(6)))
    }

    @Test
    fun beforeTrackingOnlyEnteredTripsCount() {
        val trip = Trip(id = "t1", departure = LocalDate(2025, 7, 1), returnDate = LocalDate(2025, 7, 10))
        val t = timeline(emptyMap(), manualTrips = listOf(trip))
        assertEquals(DayVerdict.ASSUMED_PRESENT, t.verdictOn(LocalDate(2025, 6, 30)))
        assertEquals(DayVerdict.ABSENT, t.verdictOn(LocalDate(2025, 7, 5)))
    }

    @Test
    fun futureDaysArePresentExceptPlannedTrips() {
        val planned = Trip(id = "p1", departure = LocalDate(2027, 7, 1), returnDate = LocalDate(2027, 7, 31))
        val t = timeline(homeEveryDay(), manualTrips = listOf(planned))
        assertEquals(DayVerdict.ASSUMED_PRESENT, t.verdictOn(LocalDate(2027, 6, 30)))
        assertEquals(DayVerdict.ABSENT, t.verdictOn(LocalDate(2027, 7, 15)))
        assertFalse(t.isPresent(LocalDate(2027, 7, 15)))
    }
}

class DetectedTripsTest {
    @Test
    fun aDetectedTripRunsFromTheDepartureDayToTheReturnDay() {
        val logs = homeEveryDay(except = setOf(5, 6, 7, 8, 9, 10)).apply {
            put(oct(5), log(INSIDE, OUTSIDE))
            (6..9).forEach { put(oct(it), log(OUTSIDE)) }
            put(oct(10), log(OUTSIDE, INSIDE))
        }
        val trip = timeline(logs).trips().single()
        assertEquals(oct(5), trip.departure)
        assertEquals(oct(10), trip.returnDate)
        assertEquals(4, trip.absentDays)
        assertEquals(setOf("FR"), trip.countries)
        assertEquals(TripSource.DETECTED, trip.source)
        assertFalse(trip.needsReview)
    }

    @Test
    fun aMissedDepartureCheckUsesTheLastDayInCanada() {
        val logs = homeEveryDay(except = setOf(15, 16)).apply {
            put(oct(15), log(OUTSIDE, country = "US"))
            put(oct(16), log(OUTSIDE, country = "US"))
        }
        val trip = timeline(logs).trips().single()
        assertEquals(oct(14), trip.departure)
        assertEquals(oct(17), trip.returnDate)
        assertEquals(2, trip.absentDays)
        assertEquals(setOf("US"), trip.countries)
    }

    @Test
    fun aDayTripIsListedWithZeroDaysAbsent() {
        val logs = homeEveryDay(except = setOf(12)).apply { put(oct(12), log(INSIDE, OUTSIDE, INSIDE, country = "US")) }
        val trip = timeline(logs).trips().single()
        assertEquals(oct(12), trip.departure)
        assertEquals(oct(12), trip.returnDate)
        assertEquals(0, trip.absentDays)
    }

    @Test
    fun anOvernightTripAcrossTwoDaysIsOneTrip() {
        val logs = homeEveryDay(except = setOf(12, 13)).apply {
            put(oct(12), log(INSIDE, OUTSIDE, country = "US"))
            put(oct(13), log(OUTSIDE, INSIDE, country = "US"))
        }
        val trip = timeline(logs).trips().single()
        assertEquals(oct(12), trip.departure)
        assertEquals(oct(13), trip.returnDate)
        assertEquals(0, trip.absentDays)
    }

    @Test
    fun dailyCrossBorderCommutesAreSeparateDayTrips() {
        val logs = homeEveryDay(except = setOf(12, 13)).apply {
            put(oct(12), log(INSIDE, OUTSIDE, INSIDE, country = "US"))
            put(oct(13), log(INSIDE, OUTSIDE, INSIDE, country = "US"))
        }
        assertEquals(listOf(oct(12), oct(13)), timeline(logs).trips().map { it.departure })
    }

    @Test
    fun anOngoingTripHasNoReturnDateYet() {
        val logs = homeEveryDay(until = 18).apply {
            put(oct(19), log(OUTSIDE, country = "MX"))
            put(oct(20), log(OUTSIDE, country = "MX"))
        }
        val trip = timeline(logs).trips().single()
        assertEquals(oct(18), trip.departure)
        assertNull(trip.returnDate)
        assertEquals(2, trip.absentDays)
    }

    @Test
    fun tripsWithReviewDaysAreFlagged() {
        val logs = homeEveryDay(except = setOf(5, 6, 7, 8)).apply {
            put(oct(5), log(INSIDE, OUTSIDE))
            put(oct(8), log(OUTSIDE, INSIDE))
        }
        val trip = timeline(logs).trips().single()
        assertEquals(oct(5), trip.departure)
        assertEquals(oct(8), trip.returnDate)
        assertTrue(trip.needsReview)
        assertEquals(2, trip.absentDays)
    }

    @Test
    fun manualTripsAreListedAndHideOverlappingDetection() {
        val manual =
            Trip(id = "m", departure = oct(5), returnDate = oct(10), destination = "Paris", reason = "Vacation")
        val logs = homeEveryDay(except = setOf(6, 7)).apply {
            put(oct(6), log(OUTSIDE))
            put(oct(7), log(OUTSIDE))
        }
        val trip = timeline(logs, manualTrips = listOf(manual)).trips().single()
        assertEquals(TripSource.MANUAL, trip.source)
        assertEquals("m", trip.manualId)
        assertEquals(4, trip.absentDays)
        assertEquals("Paris", trip.destination)
    }

    @Test
    fun notesAnnotateDetectedTrips() {
        val logs = homeEveryDay(except = setOf(15, 16)).apply {
            put(oct(15), log(OUTSIDE, country = "US"))
            put(oct(16), log(OUTSIDE, country = "US"))
        }
        val trips = timeline(
            logs,
        ).trips(notes = mapOf(oct(14) to TripNote(destination = "New York", reason = "Family")))
        assertEquals("New York", trips.single().destination)
        assertEquals("Family", trips.single().reason)
    }

    @Test
    fun plannedTripsAreMarkedAsPlanned() {
        val planned = Trip(id = "p", departure = LocalDate(2027, 7, 1), returnDate = LocalDate(2027, 7, 31))
        val trip = timeline(homeEveryDay(), manualTrips = listOf(planned)).trips().single()
        assertTrue(trip.isPlanned)
        assertEquals(29, trip.absentDays)
    }
}
