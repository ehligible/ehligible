package io.github.mirhamedjz.ehligible.core.rules

/**
 * Days of physical presence in one eligibility window.
 *
 * Each PR day is worth one day; each pre-PR status day is worth half a day, capped at
 * [CitizenshipRules.MAX_PRE_PR_CREDIT_DAYS]. Arithmetic is done in half days so nothing is ever rounded up.
 */
data class Credit(val prDays: Int, val statusDays: Int) {
    val halfDays: Int get() = 2 * prDays + minOf(statusDays, 2 * CitizenshipRules.MAX_PRE_PR_CREDIT_DAYS)

    /** Credited days, rounded down (a trailing half day is shown by [hasHalfDay]). */
    val creditedDays: Int get() = halfDays / 2

    val hasHalfDay: Boolean get() = halfDays % 2 == 1

    fun meets(requiredDays: Int = CitizenshipRules.REQUIRED_DAYS): Boolean = halfDays >= 2 * requiredDays
}
