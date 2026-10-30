package io.github.mirhamedjz.ehligible.core.presence

import io.github.mirhamedjz.ehligible.core.rules.TripDates
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/** A trip the user entered: a past trip, the current one, or a planned future trip. */
@Serializable
data class Trip(
    val id: String,
    val departure: LocalDate,
    val returnDate: LocalDate,
    val destination: String = "",
    val reason: String = "",
) {
    init {
        require(returnDate >= departure) { "Return $returnDate is before departure $departure" }
    }

    val dates: TripDates get() = TripDates(departure, returnDate)

    fun isAbsentOn(date: LocalDate): Boolean = dates.isAbsentOn(date)

    fun covers(date: LocalDate): Boolean = date >= departure && date <= returnDate
}

/** Destination/reason the user added to a detected trip, keyed by its departure date. */
@Serializable
data class TripNote(val destination: String = "", val reason: String = "")

enum class TripSource { MANUAL, DETECTED }

/** A trip as shown in the journal and the export. */
data class TimelineTrip(
    val departure: LocalDate,
    /** Null while the trip is still going on. */
    val returnDate: LocalDate?,
    val source: TripSource,
    val manualId: String? = null,
    val countries: Set<String> = emptySet(),
    val destination: String = "",
    val reason: String = "",
    /** Full days outside Canada (days needing review included); for ongoing trips, so far. */
    val absentDays: Int,
    val needsReview: Boolean = false,
    val isPlanned: Boolean = false,
)
