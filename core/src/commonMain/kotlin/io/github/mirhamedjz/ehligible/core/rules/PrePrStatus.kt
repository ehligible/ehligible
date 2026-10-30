package io.github.mirhamedjz.ehligible.core.rules

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * Time in Canada before becoming a permanent resident that earns half-day credit (Citizenship Act s. 5(1.001)):
 * days as a temporary resident (visitor, student, worker, permit holder) or protected person.
 *
 * [firstStatusDate] is when that status began (for protected persons: the approval date).
 * [exclusions] are periods without status, or spent waiting on a refugee claim, which never count.
 */
@Serializable
data class PrePrStatus(val firstStatusDate: LocalDate? = null, val exclusions: List<DateRange> = emptyList()) {
    /** Whether [date], a day before the PR date, was spent with qualifying status. */
    fun hasStatusOn(date: LocalDate): Boolean =
        firstStatusDate != null && date >= firstStatusDate && exclusions.none { date in it }

    companion object {
        val NONE = PrePrStatus()
    }
}
