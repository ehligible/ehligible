package io.github.mirhamedjz.ehligible.core.rules

import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.serialization.Serializable

/** An inclusive range of calendar dates. */
@Serializable
data class DateRange(val start: LocalDate, val endInclusive: LocalDate) {
    init {
        require(start <= endInclusive) { "Range start $start is after its end $endInclusive" }
    }

    operator fun contains(date: LocalDate): Boolean = date >= start && date <= endInclusive

    val lengthInDays: Int get() = start.daysUntil(endInclusive) + 1
}
