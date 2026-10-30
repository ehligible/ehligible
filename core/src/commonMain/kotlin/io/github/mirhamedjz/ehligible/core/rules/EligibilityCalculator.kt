package io.github.mirhamedjz.ehligible.core.rules

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus

/**
 * Applies the physical presence rules (docs/research/citizenship-rules.md) to one person's history.
 *
 * [presence] must already include planned trips and the assumption that future days are spent in Canada.
 */
class EligibilityCalculator(
    private val prDate: LocalDate,
    private val prePrStatus: PrePrStatus,
    private val presence: Presence,
) {
    /** Credit someone signing on [applicationDate] would have. */
    fun creditFor(applicationDate: LocalDate): Credit {
        val window = EligibilityWindow.forApplicationDate(applicationDate)
        var prDays = 0
        var statusDays = 0
        var day = window.start
        while (day <= window.endInclusive) {
            if (isPrDay(day)) prDays++
            if (isStatusDay(day)) statusDays++
            day = day.plus(1, DateTimeUnit.DAY)
        }
        return Credit(prDays, statusDays)
    }

    /**
     * The earliest application date on or after [from] with at least [requiredDays] credited days,
     * or null if none exists within [horizonYears]. Searched day by day because old presence rolls out
     * of the window as it moves, so eligibility isn't monotonic.
     */
    fun firstEligibleDate(
        from: LocalDate,
        requiredDays: Int = CitizenshipRules.REQUIRED_DAYS,
        horizonYears: Int = HORIZON_YEARS,
    ): LocalDate? {
        val lastApplicationDate = from.plus(horizonYears, DateTimeUnit.YEAR)
        val origin = EligibilityWindow.forApplicationDate(from).start
        val size = origin.daysUntil(lastApplicationDate)
        val prPrefix = IntArray(size + 1)
        val statusPrefix = IntArray(size + 1)
        var day = origin
        for (i in 0 until size) {
            prPrefix[i + 1] = prPrefix[i] + if (isPrDay(day)) 1 else 0
            statusPrefix[i + 1] = statusPrefix[i] + if (isStatusDay(day)) 1 else 0
            day = day.plus(1, DateTimeUnit.DAY)
        }

        var applicationDate = from
        while (applicationDate <= lastApplicationDate) {
            val window = EligibilityWindow.forApplicationDate(applicationDate)
            val first = origin.daysUntil(window.start)
            val last = origin.daysUntil(window.endInclusive)
            val credit = Credit(
                prDays = prPrefix[last + 1] - prPrefix[first],
                statusDays = statusPrefix[last + 1] - statusPrefix[first],
            )
            if (credit.meets(requiredDays)) return applicationDate
            applicationDate = applicationDate.plus(1, DateTimeUnit.DAY)
        }
        return null
    }

    /** Where someone stands on [today] and when they can apply, with a safety margin of [marginDays]. */
    fun project(today: LocalDate, marginDays: Int): EligibilityProjection = EligibilityProjection(
        today = today,
        creditToDate = creditFor(today.plus(1, DateTimeUnit.DAY)),
        firstEligibleDate = firstEligibleDate(today),
        saferApplyDate = firstEligibleDate(today, CitizenshipRules.REQUIRED_DAYS + marginDays),
    )

    private fun isPrDay(day: LocalDate): Boolean = day >= prDate && presence.isPresent(day)

    private fun isStatusDay(day: LocalDate): Boolean =
        day < prDate && prePrStatus.hasStatusOn(day) && presence.isPresent(day)

    private companion object {
        const val HORIZON_YEARS = 15
    }
}

/** A person's standing on [today]. [creditToDate] counts every day up to and including today. */
data class EligibilityProjection(
    val today: LocalDate,
    val creditToDate: Credit,
    val firstEligibleDate: LocalDate?,
    val saferApplyDate: LocalDate?,
) {
    val daysUntilEligible: Int? get() = firstEligibleDate?.let { today.daysUntil(it) }
    val daysUntilSaferDate: Int? get() = saferApplyDate?.let { today.daysUntil(it) }
    val isEligibleToday: Boolean get() = firstEligibleDate == today
}
