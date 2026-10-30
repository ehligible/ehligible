package io.github.mirhamedjz.ehligible.core.rules

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.minus

/** The five years immediately before the application date. The application day itself is not counted. */
object EligibilityWindow {
    fun forApplicationDate(applicationDate: LocalDate): DateRange {
        val startYear = applicationDate.year - CitizenshipRules.WINDOW_YEARS
        val isLeapDay = applicationDate.month == Month.FEBRUARY && applicationDate.day == 29
        // Five years before Feb 29 has no Feb 29; start on Mar 1, the conservative (shorter) choice.
        val start = if (isLeapDay) {
            LocalDate(startYear, Month.MARCH, 1)
        } else {
            LocalDate(startYear, applicationDate.month, applicationDate.day)
        }
        return DateRange(start, applicationDate.minus(1, DateTimeUnit.DAY))
    }
}
