package io.github.mirhamedjz.ehligible.core.rules

import kotlinx.datetime.LocalDate

enum class Requirement { REQUIRED, NOT_REQUIRED, MAYBE, UNKNOWN }

/** The non-presence requirements shown on the "Ready to apply?" checklist. */
object ApplicationRequirements {
    /** Returns must be filed (where required) for at least this many of [taxYearsToConsider]. */
    const val TAX_YEARS_NEEDED: Int = 3

    /** Language proof (CLB/NCLC 4) and the citizenship test apply from this age… */
    const val LANGUAGE_AND_TEST_MIN_AGE: Int = 18

    /** …up to and including this age, on the application date. */
    const val LANGUAGE_AND_TEST_MAX_AGE: Int = 54

    const val LANGUAGE_BENCHMARK_LEVEL: Int = 4

    /** Calendar years fully or partially in the window; the application year itself is not counted. */
    fun taxYearsToConsider(applicationDate: LocalDate): IntRange =
        EligibilityWindow.forApplicationDate(applicationDate).start.year..(applicationDate.year - 1)

    /** Whether language proof and the test apply, knowing only the birth year. */
    fun languageAndTest(birthYear: Int?, applicationDate: LocalDate): Requirement {
        if (birthYear == null) return Requirement.UNKNOWN
        val ageIfBirthdayPassed = applicationDate.year - birthYear
        val ageIfBirthdayAhead = ageIfBirthdayPassed - 1
        val ages = LANGUAGE_AND_TEST_MIN_AGE..LANGUAGE_AND_TEST_MAX_AGE
        val passed = ageIfBirthdayPassed in ages
        val ahead = ageIfBirthdayAhead in ages
        return when {
            passed && ahead -> Requirement.REQUIRED
            !passed && !ahead -> Requirement.NOT_REQUIRED
            else -> Requirement.MAYBE
        }
    }
}
