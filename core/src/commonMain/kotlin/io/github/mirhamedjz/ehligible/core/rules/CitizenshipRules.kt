package io.github.mirhamedjz.ehligible.core.rules

/**
 * Constants from the Citizenship Act, s. 5(1)(c) and s. 5(1.001).
 * See docs/research/citizenship-rules.md for sources.
 */
object CitizenshipRules {
    /** Days of physical presence needed in the window. */
    const val REQUIRED_DAYS: Int = 1_095

    /** Maximum credit (in days) for time in Canada before becoming a permanent resident. */
    const val MAX_PRE_PR_CREDIT_DAYS: Int = 365

    /** Length of the eligibility window, in calendar years before the application date. */
    const val WINDOW_YEARS: Int = 5
}
