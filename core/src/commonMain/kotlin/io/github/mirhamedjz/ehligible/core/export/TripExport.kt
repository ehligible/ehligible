package io.github.mirhamedjz.ehligible.core.export

import io.github.mirhamedjz.ehligible.core.presence.TimelineTrip
import kotlinx.datetime.LocalDate

/**
 * Exports trips with the columns IRCC asks for when listing absences: date left, date returned,
 * destination, reason, and full days absent.
 */
object TripExport {
    data class Labels(
        val departure: String,
        val returnDate: String,
        val destination: String,
        val reason: String,
        val daysAbsent: String,
        val ongoing: String,
    )

    /** Past and current trips that end on or after [since], oldest first. Planned trips are left out. */
    fun select(trips: List<TimelineTrip>, since: LocalDate): List<TimelineTrip> =
        trips.filter { !it.isPlanned && (it.returnDate == null || it.returnDate >= since) }.sortedBy { it.departure }

    fun csv(trips: List<TimelineTrip>, labels: Labels, countryName: (String) -> String): String = buildString {
        appendCsvRow(listOf(labels.departure, labels.returnDate, labels.destination, labels.reason, labels.daysAbsent))
        for (trip in trips) {
            appendCsvRow(
                listOf(
                    trip.departure.toString(),
                    trip.returnDate?.toString() ?: labels.ongoing,
                    destination(trip, countryName),
                    trip.reason,
                    trip.absentDays.toString(),
                ),
            )
        }
    }

    /** A plain-text list for sharing in a message or note. */
    fun text(
        trips: List<TimelineTrip>,
        labels: Labels,
        countryName: (String) -> String,
        formatDate: (LocalDate) -> String,
    ): String = trips.joinToString(separator = "\n") { trip ->
        val back = trip.returnDate?.let(formatDate) ?: labels.ongoing
        val parts = listOfNotNull(
            "${formatDate(trip.departure)} → $back",
            destination(trip, countryName).takeIf { it.isNotBlank() },
            trip.reason.takeIf { it.isNotBlank() },
            "${labels.daysAbsent}: ${trip.absentDays}",
        )
        parts.joinToString(" · ")
    }

    fun destination(trip: TimelineTrip, countryName: (String) -> String): String =
        trip.destination.ifBlank { trip.countries.sorted().joinToString(", ") { countryName(it) } }

    private fun StringBuilder.appendCsvRow(cells: List<String>) {
        cells.joinTo(this, separator = ",") { cell ->
            if (cell.needsQuoting()) "\"${cell.replace("\"", "\"\"")}\"" else cell
        }
        append("\r\n")
    }

    private fun String.needsQuoting(): Boolean = any { it == ',' || it == '"' || it == '\n' || it == '\r' }
}
