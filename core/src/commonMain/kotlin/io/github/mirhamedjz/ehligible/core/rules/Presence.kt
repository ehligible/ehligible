package io.github.mirhamedjz.ehligible.core.rules

import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

/** Answers whether the person was (or is assumed to be) physically present in Canada on a calendar day. */
fun interface Presence {
    fun isPresent(date: LocalDate): Boolean
}

/**
 * A trip outside Canada. IRCC counts the departure day and the return day as days in Canada:
 * only the full days in between are absences.
 */
data class TripDates(val departure: LocalDate, val returnDate: LocalDate) {
    init {
        require(returnDate >= departure) { "Return $returnDate is before departure $departure" }
    }

    val absentDays: Int get() = maxOf(0, departure.daysUntil(returnDate) - 1)

    fun isAbsentOn(date: LocalDate): Boolean = date > departure && date < returnDate
}

/** Presence derived from a list of trips: every day is present unless a trip makes it a full day abroad. */
class TripPresence(private val trips: List<TripDates>) : Presence {
    override fun isPresent(date: LocalDate): Boolean = trips.none { it.isAbsentOn(date) }
}
