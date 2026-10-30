package io.github.mirhamedjz.ehligible.core.model

import io.github.mirhamedjz.ehligible.core.presence.Check
import io.github.mirhamedjz.ehligible.core.presence.CheckKind
import io.github.mirhamedjz.ehligible.core.presence.Trip
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

class WidgetSnapshotTest {
    private val now = Instant.parse("2026-10-29T16:00:00Z")
    private val today = LocalDate(2026, 10, 29)
    private val zone = TimeZone.of("America/Toronto")
    private val data =
        AppData(profile = Profile(prDate = LocalDate(2025, 1, 1), trackingStart = LocalDate(2026, 10, 1)))

    @Test
    fun theCountdownIsPrecomputedForTheComingDays() {
        val snapshot = data.widgetSnapshot(today, now, zone)!!
        assertEquals(WidgetSnapshot.DAYS, snapshot.days.size)
        assertEquals(today, snapshot.days.first().date)
        assertEquals(LocalDate(2026, 11, 11), snapshot.days.last().date)
        assertEquals(listOf(429, 428, 427), snapshot.days.take(3).map { it.daysLeft })
        assertEquals(LocalDate(2028, 1, 1), snapshot.days.first().countdownDate)
        assertEquals(listOf(667, 668, 669), snapshot.days.take(3).map { it.creditedDays })
        assertEquals(now.toEpochMilliseconds(), snapshot.generatedAt)
    }

    @Test
    fun aPlannedTripPausesTheCreditAndMovesTheDate() {
        // Away Nov 1-5: the full days Nov 2-4 don't count, so the date moves 3 days later.
        val trip = Trip("t", LocalDate(2026, 11, 1), LocalDate(2026, 11, 5), "Paris", "Wedding")
        val days = data.copy(trips = listOf(trip)).widgetSnapshot(today, now, zone)!!.days
        assertEquals(LocalDate(2028, 1, 4), days.first().countdownDate)
        assertEquals(listOf(670, 670, 670, 670, 671), days.subList(3, 8).map { it.creditedDays })
    }

    @Test
    fun eachDayHasItsOwnStatus() {
        assertEquals(WidgetStatus.UNKNOWN, data.widgetSnapshot(today, now, zone)!!.days.first().status)
        val abroad = data.recordCheck(Check(CheckKind.OUTSIDE, "FR"), now, zone).widgetSnapshot(today, now, zone)!!
        assertEquals(WidgetStatus.ABROAD, abroad.days.first().status)
        assertEquals("FR", abroad.days.first().country)
        // Checked at noon on Oct 29: still fresh at the midnights of Oct 30 and 31, stale from Nov 1.
        val home = data.recordCheck(Check(CheckKind.INSIDE), now, zone).widgetSnapshot(today, now, zone)!!
        assertEquals(
            listOf(WidgetStatus.IN_CANADA, WidgetStatus.IN_CANADA, WidgetStatus.IN_CANADA, WidgetStatus.UNKNOWN),
            home.days.take(4).map { it.status },
        )
        assertNull(home.days.first().country)
    }

    @Test
    fun aPlannedTripShowsAsAbroadOnItsDays() {
        val trip = Trip("t", LocalDate(2026, 11, 1), LocalDate(2026, 11, 5), "Paris", "Wedding")
        val days = data.copy(trips = listOf(trip)).widgetSnapshot(today, now, zone)!!.days
        assertEquals(WidgetStatus.ABROAD, days[4].status) // Nov 2
        assertEquals(WidgetStatus.UNKNOWN, days[8].status) // Nov 6, home again
    }

    @Test
    fun noSnapshotBeforeOnboarding() {
        assertNull(AppData().widgetSnapshot(today, now, zone))
    }
}
