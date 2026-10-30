package io.github.mirhamedjz.ehligible.core.rules

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

private fun d(iso: String) = LocalDate.parse(iso)

private fun calculator(
    pr: String,
    firstStatus: String? = null,
    exclusions: List<Pair<String, String>> = emptyList(),
    trips: List<Pair<String, String>> = emptyList(),
) = EligibilityCalculator(
    prDate = d(pr),
    prePrStatus = PrePrStatus(
        firstStatusDate = firstStatus?.let(::d),
        exclusions = exclusions.map { (start, end) -> DateRange(d(start), d(end)) },
    ),
    presence = TripPresence(trips.map { (left, back) -> TripDates(d(left), d(back)) }),
)

class EligibilityWindowTest {
    @Test
    fun windowIsFiveCalendarYearsEndingTheDayBeforeTheApplication() {
        // CIT 0407: applying April 1, 2018 -> April 1, 2013 to March 31, 2018, 1,826 days.
        val window = EligibilityWindow.forApplicationDate(d("2018-04-01"))
        assertEquals(DateRange(d("2013-04-01"), d("2018-03-31")), window)
        assertEquals(1826, window.lengthInDays)
    }

    @Test
    fun windowCanHoldTwoLeapDays() {
        // CIT 0407: "The maximum number of days you can enter in Box C is 1827."
        assertEquals(1827, EligibilityWindow.forApplicationDate(d("2021-01-01")).lengthInDays)
    }

    @Test
    fun applyingOnALeapDayStartsTheWindowOnMarchFirst() {
        // Conservative choice: the shorter window.
        val window = EligibilityWindow.forApplicationDate(d("2024-02-29"))
        assertEquals(DateRange(d("2019-03-01"), d("2024-02-28")), window)
    }
}

class EligibilityCalculatorTest {
    // --- IRCC program delivery instructions, worked examples (typos corrected) ---

    @Test
    fun irccExample1_maximumPrePrCreditPlus730PrDays() {
        val calc = calculator(pr = "2016-04-01", firstStatus = "2014-04-01")
        val credit = calc.creditFor(d("2018-04-01"))
        assertEquals(730, credit.prDays)
        assertEquals(731, credit.statusDays)
        assertEquals(1095, credit.creditedDays)
        assertTrue(credit.meets())
        // "applying 2018-03-31 gives 1,094"
        val dayEarlier = calc.creditFor(d("2018-03-31"))
        assertEquals(1094, dayEarlier.creditedDays)
        assertFalse(dayEarlier.meets())
    }

    @Test
    fun irccExample3_prePrCreditIsCappedSo730PrDaysAreAlwaysNeeded() {
        val calc = calculator(pr = "2017-04-01", firstStatus = "2014-04-01")
        val credit = calc.creditFor(d("2018-04-01"))
        assertEquals(365, credit.prDays)
        assertEquals(1096, credit.statusDays)
        assertEquals(730, credit.creditedDays)
        assertFalse(credit.meets())
        assertEquals(d("2019-04-01"), calc.firstEligibleDate(from = d("2018-04-01")))
    }

    @Test
    fun irccExample5_prOnArrival() {
        val credit = calculator(pr = "2014-04-01").creditFor(d("2018-04-01"))
        assertEquals(1461, credit.prDays)
        assertEquals(0, credit.statusDays)
        assertEquals(1461, credit.creditedDays)
    }

    @Test
    fun irccExample6_refugeeClaimWaitingTimeAndPrePrAbsencesAreExcluded() {
        // Status (TR) from 2012-04-01; claim 2015-04-01; protected 2015-08-16; PR 2016-01-01.
        // The PDI prints "PR = April 1, 2016" but its arithmetic uses January 1, 2016.
        val calc = calculator(
            pr = "2016-01-01",
            firstStatus = "2012-04-01",
            exclusions = listOf("2015-04-01" to "2015-08-15"),
            trips = listOf("2014-03-01" to "2014-03-31", "2014-12-15" to "2014-12-31"),
        )
        val credit = calc.creditFor(d("2018-04-01"))
        assertEquals(824, credit.statusDays) // 1005 - 44 absent - 137 ineligible (PDI typo "365 + 366 + 275")
        assertEquals(821, credit.prDays)
        assertEquals(1186, credit.creditedDays)
        assertTrue(credit.meets())
    }

    @Test
    fun irccExample4_withoutTheSentence_prAbsenceIsDeducted() {
        // Example 4 also has a sentence (out of scope for the app); without it: 365 + (821 - 44) = 1,142.
        val calc = calculator(
            pr = "2016-01-01",
            firstStatus = "2012-04-01",
            exclusions = listOf("2015-04-01" to "2015-08-15"),
            trips = listOf(
                "2014-03-01" to "2014-03-31",
                "2014-12-15" to "2014-12-31",
                "2017-12-01" to "2018-01-15", // 44 days absent
            ),
        )
        val credit = calc.creditFor(d("2018-04-01"))
        assertEquals(777, credit.prDays)
        assertEquals(1142, credit.creditedDays)
    }

    // --- Day counting ---

    @Test
    fun prOnlyNeeds1095DaysAfterThePrDate() {
        assertEquals(d("2022-12-31"), calculator(pr = "2020-01-01").firstEligibleDate(from = d("2020-01-01")))
    }

    @Test
    fun onlyFullDaysAbroadCount() {
        // Leave June 1, back June 11: June 2..10 = 9 absent days.
        val calc = calculator(pr = "2020-01-01", trips = listOf("2021-06-01" to "2021-06-11"))
        assertEquals(d("2023-01-09"), calc.firstEligibleDate(from = d("2020-01-01")))
    }

    @Test
    fun sameDayAndNextDayTripsCostNothing() {
        val calc = calculator(
            pr = "2020-01-01",
            trips = listOf("2021-03-05" to "2021-03-05", "2021-07-10" to "2021-07-11"),
        )
        assertEquals(d("2022-12-31"), calc.firstEligibleDate(from = d("2020-01-01")))
    }

    @Test
    fun theApplicationDayItselfIsNotCounted() {
        val calc = calculator(pr = "2020-01-01")
        assertEquals(1094, calc.creditFor(d("2022-12-30")).creditedDays)
        assertEquals(1095, calc.creditFor(d("2022-12-31")).creditedDays)
    }

    @Test
    fun halfDayCreditIsNeverRoundedUp() {
        // 151 status days = 75.5 days of credit, so 1,020 PR days are needed (1,019 + 75.5 < 1,095).
        val calc = calculator(pr = "2017-06-01", firstStatus = "2017-01-01")
        val first = calc.firstEligibleDate(from = d("2017-06-01"))
        assertEquals(d("2017-06-01").plus(1020, DateTimeUnit.DAY), first)
        val credit = calc.creditFor(first!!)
        assertEquals(1020, credit.prDays)
        assertEquals(151, credit.statusDays)
        assertEquals(1095, credit.creditedDays) // 1,095.5 shown as 1,095
    }

    @Test
    fun statusExclusionsRemoveDaysWithoutStatus() {
        // 365 status days after removing a year without status -> 182.5 credit -> 913 PR days needed.
        val calc = calculator(
            pr = "2017-01-01",
            firstStatus = "2015-01-01",
            exclusions = listOf("2015-06-01" to "2016-05-31"),
        )
        assertEquals(d("2017-01-01").plus(913, DateTimeUnit.DAY), calc.firstEligibleDate(from = d("2017-01-01")))
    }

    @Test
    fun tripsBeforePrReduceTheStatusCredit() {
        // 731 status days - 29 absent = 702 -> 351 credit -> 744 PR days needed.
        val calc = calculator(
            pr = "2017-01-01",
            firstStatus = "2015-01-01",
            trips = listOf("2015-03-01" to "2015-03-31"),
        )
        assertEquals(d("2017-01-01").plus(744, DateTimeUnit.DAY), calc.firstEligibleDate(from = d("2017-01-01")))
    }

    @Test
    fun statusDaysOutsideTheWindowDoNotCount() {
        // Only status days from 2012-12-31 onwards are inside the window ending 2017-12-30.
        val calc = calculator(pr = "2016-01-01", firstStatus = "2012-01-01")
        assertEquals(d("2017-12-31"), calc.firstEligibleDate(from = d("2016-01-01")))
        assertEquals(1096, calc.creditFor(d("2017-12-31")).statusDays)
    }

    @Test
    fun oldPresenceRollsOutOfTheWindow() {
        // Away 2016-01-01..2019-01-01; only days after coming back can reach 1,095.
        val calc = calculator(pr = "2015-01-01", trips = listOf("2016-01-01" to "2019-01-01"))
        assertEquals(d("2021-12-31"), calc.firstEligibleDate(from = d("2019-01-01")))
    }

    @Test
    fun longAgoPrWithRecentLongAbsenceStartsAlmostOver() {
        val calc = calculator(pr = "2010-01-01", trips = listOf("2019-01-01" to "2024-01-01"))
        assertEquals(d("2026-12-31"), calc.firstEligibleDate(from = d("2024-06-01")))
    }

    @Test
    fun plannedTripsPushTheDateOut() {
        // A planned July trip (July 2..30 abroad = 29 days).
        val calc = calculator(pr = "2020-01-01", trips = listOf("2022-07-01" to "2022-07-31"))
        assertEquals(d("2023-01-29"), calc.firstEligibleDate(from = d("2021-01-01")))
    }

    @Test
    fun alreadyEligibleMeansToday() {
        val calc = calculator(pr = "2010-01-01")
        assertEquals(d("2026-10-29"), calc.firstEligibleDate(from = d("2026-10-29")))
    }

    @Test
    fun extraRequiredDaysGiveTheSaferDate() {
        val calc = calculator(pr = "2020-01-01")
        assertEquals(d("2023-01-07"), calc.firstEligibleDate(from = d("2020-01-01"), requiredDays = 1095 + 7))
    }

    @Test
    fun returnsNullWhenNotReachableBeforeTheHorizon() {
        val calc = calculator(pr = "2020-01-01", trips = listOf("2020-01-01" to "2050-01-01"))
        assertNull(calc.firstEligibleDate(from = d("2021-01-01")))
    }
}

class EligibilityProjectionTest {
    @Test
    fun projectionCountsTodayAndGivesBothDates() {
        val calc = calculator(pr = "2020-01-01")
        val projection = calc.project(today = d("2021-01-01"), marginDays = 7)
        assertEquals(367, projection.creditToDate.creditedDays) // 2020-01-01..2021-01-01 inclusive
        assertEquals(d("2022-12-31"), projection.firstEligibleDate)
        assertEquals(d("2023-01-07"), projection.saferApplyDate)
        assertEquals(729, projection.daysUntilEligible)
        assertEquals(736, projection.daysUntilSaferDate)
        assertFalse(projection.isEligibleToday)
    }

    @Test
    fun eligibleTodayHasZeroDaysLeft() {
        val projection = calculator(pr = "2010-01-01").project(today = d("2026-10-29"), marginDays = 0)
        assertEquals(0, projection.daysUntilEligible)
        assertTrue(projection.isEligibleToday)
    }
}

class ApplicationRequirementsTest {
    @Test
    fun taxYearsAreTheCalendarYearsInTheWindowExceptTheApplicationYear() {
        // IRCC: applying June 1, 2020 -> 2015 to 2019.
        assertEquals(2015..2019, ApplicationRequirements.taxYearsToConsider(d("2020-06-01")))
        assertEquals(2016..2020, ApplicationRequirements.taxYearsToConsider(d("2021-01-01")))
    }

    @Test
    fun languageAndTestApplyFrom18To54() {
        val a = d("2026-06-15")
        assertEquals(
            Requirement.REQUIRED,
            ApplicationRequirements.languageAndTest(birthYear = 1990, applicationDate = a),
        )
        assertEquals(
            Requirement.NOT_REQUIRED,
            ApplicationRequirements.languageAndTest(birthYear = 1960, applicationDate = a),
        )
        assertEquals(Requirement.MAYBE, ApplicationRequirements.languageAndTest(birthYear = 2008, applicationDate = a))
        assertEquals(Requirement.MAYBE, ApplicationRequirements.languageAndTest(birthYear = 1971, applicationDate = a))
        assertEquals(
            Requirement.UNKNOWN,
            ApplicationRequirements.languageAndTest(birthYear = null, applicationDate = a),
        )
    }
}
