package io.github.mirhamedjz.ehligible.core.model

import io.github.mirhamedjz.ehligible.core.export.TripExport
import io.github.mirhamedjz.ehligible.core.geo.LocationVerdict
import io.github.mirhamedjz.ehligible.core.presence.Check
import io.github.mirhamedjz.ehligible.core.presence.CheckClassifier
import io.github.mirhamedjz.ehligible.core.presence.CheckKind
import io.github.mirhamedjz.ehligible.core.presence.TimelineTrip
import io.github.mirhamedjz.ehligible.core.presence.Trip
import io.github.mirhamedjz.ehligible.core.presence.TripSource
import io.github.mirhamedjz.ehligible.core.presence.UserVerdict
import io.github.mirhamedjz.ehligible.core.rules.DateRange
import io.github.mirhamedjz.ehligible.core.rules.PrePrStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

class CheckClassifierTest {
    @Test
    fun locationDecidesWhenItIsClear() {
        assertEquals(Check(CheckKind.INSIDE), CheckClassifier.classify(LocationVerdict.InCanada, "us"))
        assertEquals(Check(CheckKind.OUTSIDE, "FR"), CheckClassifier.classify(LocationVerdict.Abroad("FR"), "ca"))
    }

    @Test
    fun theNetworkNamesTheCountryWhenLocationCannot() {
        assertEquals(Check(CheckKind.OUTSIDE, "US"), CheckClassifier.classify(LocationVerdict.Abroad(null), "us"))
    }

    @Test
    fun nearTheBorderTheNetworkBreaksTheTie() {
        assertEquals(Check(CheckKind.BORDER), CheckClassifier.classify(LocationVerdict.NearBorder, "ca"))
        assertEquals(Check(CheckKind.BORDER), CheckClassifier.classify(LocationVerdict.NearBorder, null))
        assertEquals(Check(CheckKind.BORDER_FOREIGN, "US"), CheckClassifier.classify(LocationVerdict.NearBorder, "US"))
    }

    @Test
    fun withoutLocationTheNetworkAloneDecides() {
        assertEquals(Check(CheckKind.INSIDE), CheckClassifier.classify(null, "ca"))
        assertEquals(Check(CheckKind.OUTSIDE, "MX"), CheckClassifier.classify(null, "mx"))
        assertNull(CheckClassifier.classify(null, ""))
        assertNull(CheckClassifier.classify(null, null))
    }
}

class AppDataTest {
    private val montreal = TimeZone.of("America/Toronto")
    private val tokyo = TimeZone.of("Asia/Tokyo")

    @Test
    fun checksAreFiledUnderTheLocalDateWhereThePhoneWas() {
        // 2026-10-29T03:00Z is still Oct 28 in Montreal but already Oct 29 in Tokyo.
        val at = Instant.parse("2026-10-29T03:00:00Z")
        val home = AppData().recordCheck(Check(CheckKind.INSIDE), at, montreal)
        assertTrue(LocalDate(2026, 10, 28) in home.dayLogs)
        val away = AppData().recordCheck(Check(CheckKind.OUTSIDE, "JP"), at, tokyo)
        assertEquals(setOf("JP"), away.dayLogs.getValue(LocalDate(2026, 10, 29)).countries)
        assertEquals(CheckKind.OUTSIDE, away.lastCheck?.kind)
    }

    @Test
    fun backupsRoundTrip() {
        val data = AppData(
            profile = Profile(
                prDate = LocalDate(2024, 5, 1),
                prePrStatus = PrePrStatus(
                    LocalDate(2021, 9, 1),
                    listOf(DateRange(LocalDate(2022, 1, 1), LocalDate(2022, 2, 1))),
                ),
                trackingStart = LocalDate(2026, 10, 29),
                birthYear = 1995,
            ),
            trips = listOf(Trip("t", LocalDate(2025, 7, 1), LocalDate(2025, 7, 20), "Paris", "Vacation")),
            userVerdicts = mapOf(LocalDate(2026, 10, 30) to UserVerdict.PRESENT),
        ).recordCheck(Check(CheckKind.OUTSIDE, "US"), Instant.parse("2026-10-29T15:00:00Z"), montreal)
        assertEquals(data, AppDataJson.decode(AppDataJson.encode(data)))
    }

    @Test
    fun garbageIsRejectedAsABackup() {
        assertFailsWith<InvalidBackupException> { AppDataJson.decode("{ not json") }
        assertFailsWith<InvalidBackupException> { AppDataJson.decode("""{"schemaVersion": 99}""") }
    }
}

class DashboardTest {
    private val now = Instant.parse("2026-10-29T16:00:00Z")
    private val today = LocalDate(2026, 10, 29)

    private fun data(prDate: LocalDate, target: CountdownTarget = CountdownTarget.EXACT) = AppData(
        profile = Profile(prDate = prDate, trackingStart = LocalDate(2026, 10, 1)),
        settings = Settings(marginDays = 7, countdownTarget = target),
    )

    @Test
    fun countsDownToTheExactDateByDefault() {
        val dashboard = data(LocalDate(2025, 1, 1)).dashboard(today, now)!!
        assertEquals(LocalDate(2028, 1, 1), dashboard.countdownDate) // no leap day in 2025-2027
        assertEquals(429, dashboard.daysLeft)
        assertEquals(667, dashboard.creditedDays) // 2025-01-01..2026-10-29
    }

    @Test
    fun canCountDownToTheSaferDate() {
        val dashboard = data(LocalDate(2025, 1, 1), CountdownTarget.SAFER).dashboard(today, now)!!
        assertEquals(LocalDate(2028, 1, 8), dashboard.countdownDate)
    }

    @Test
    fun statusComesFromARecentCheck() {
        val zone = TimeZone.of("America/Toronto")
        val abroad = data(LocalDate(2025, 1, 1)).recordCheck(Check(CheckKind.OUTSIDE, "US"), now, zone)
        assertEquals(CurrentStatus.Abroad("US"), abroad.dashboard(today, now)!!.currentStatus)
        val stale = abroad.dashboard(LocalDate(2026, 11, 3), Instant.parse("2026-11-03T16:00:00Z"))!!
        assertEquals(CurrentStatus.Unknown, stale.currentStatus)
    }

    @Test
    fun noDashboardBeforeOnboarding() {
        assertNull(AppData().dashboard(today, now))
    }

    @Test
    fun milestonesFireOnceAndOnlyTheLatestIsCelebrated() {
        val start = data(LocalDate(2024, 1, 1)) // 1,033 days in: past three quarters, 63 days left
        val dashboard = start.dashboard(today, now)!!
        val (milestone, after) = start.nextCelebration(dashboard)
        assertEquals(Milestone.DAYS_LEFT_100, milestone)
        assertTrue(after.celebrated.containsAll(listOf("QUARTER", "HALFWAY", "THREE_QUARTERS", "DAYS_LEFT_100")))
        assertNull(after.nextCelebration(dashboard).first)
    }
}

class TripExportTest {
    private val labels = TripExport.Labels("Left", "Returned", "Destination", "Reason", "Days absent", "Still away")

    @Test
    fun csvHasIrccColumnsAndQuotesWhenNeeded() {
        val trips = listOf(
            TimelineTrip(
                LocalDate(2026, 3, 1),
                LocalDate(2026, 3, 10),
                TripSource.MANUAL,
                reason = "Family, wedding",
                absentDays = 8,
            ),
            TimelineTrip(
                LocalDate(2026, 10, 20),
                null,
                TripSource.DETECTED,
                countries = setOf("US", "MX"),
                absentDays = 3,
            ),
        )
        val csv = TripExport.csv(trips, labels) { code ->
            mapOf("US" to "United States", "MX" to "Mexico")[code] ?: code
        }
        assertEquals(
            "Left,Returned,Destination,Reason,Days absent\r\n" +
                "2026-03-01,2026-03-10,,\"Family, wedding\",8\r\n" +
                "2026-10-20,Still away,\"Mexico, United States\",,3\r\n",
            csv,
        )
    }

    @Test
    fun selectionSkipsPlannedTripsAndOldOnes() {
        val old = TimelineTrip(LocalDate(2019, 1, 1), LocalDate(2019, 1, 5), TripSource.MANUAL, absentDays = 3)
        val recent = TimelineTrip(LocalDate(2025, 1, 1), LocalDate(2025, 1, 5), TripSource.MANUAL, absentDays = 3)
        val planned =
            TimelineTrip(
                LocalDate(2027, 1, 1),
                LocalDate(2027, 1, 5),
                TripSource.MANUAL,
                absentDays = 3,
                isPlanned = true,
            )
        assertEquals(listOf(recent), TripExport.select(listOf(planned, recent, old), since = LocalDate(2021, 10, 29)))
    }
}
