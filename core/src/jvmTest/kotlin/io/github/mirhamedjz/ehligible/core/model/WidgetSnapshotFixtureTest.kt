package io.github.mirhamedjz.ehligible.core.model

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json

/**
 * The iOS widget decodes the snapshot in Swift, so its JSON shape is a contract. The same fixture file is decoded
 * by the Swift tests (`iosApp/EhligibleWidgetTests`); changing the format means changing both sides.
 */
class WidgetSnapshotFixtureTest {
    private val fixture = File("../iosApp/EhligibleWidgetTests/widget-snapshot.json").readText()

    private val expected = WidgetSnapshot(
        generatedAt = 1793289600000,
        reviewDays = 2,
        target = CountdownTarget.SAFER,
        days = listOf(
            WidgetDay(
                LocalDate(2026, 10, 29),
                436,
                LocalDate(2028, 1, 8),
                667,
                0.6091,
                false,
                WidgetStatus.ABROAD,
                "FR",
            ),
            WidgetDay(LocalDate(2026, 10, 30), 435, LocalDate(2028, 1, 8), 668, 0.6100, false, WidgetStatus.IN_CANADA),
            WidgetDay(LocalDate(2026, 10, 31), null, null, 668, 0.6100, false, WidgetStatus.UNKNOWN),
        ),
    )

    @Test
    fun theFixtureDecodesToTheExpectedSnapshot() {
        assertEquals(expected, WidgetSnapshot.decode(fixture))
    }

    @Test
    fun encodingProducesTheFixture() {
        assertEquals(Json.parseToJsonElement(fixture), Json.parseToJsonElement(expected.encode()))
    }
}
