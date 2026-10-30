package io.github.mirhamedjz.ehligible.core.geo

import io.github.mirhamedjz.ehligible.core.geo.LocationVerdict.Abroad
import io.github.mirhamedjz.ehligible.core.geo.LocationVerdict.InCanada
import io.github.mirhamedjz.ehligible.core.geo.LocationVerdict.NearBorder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/** Checks the shipped boundary file against well-known places. */
class RealBordersTest {
    private val index = BorderIndex.parse(
        File("../composeApp/src/commonMain/composeResources/files/borders.bin").readBytes(),
    )

    private fun at(lat: Double, lon: Double) = index.locate(lat, lon, accuracyMeters = 2_000.0)

    @Test
    fun citiesAcrossCanadaAreInCanada() {
        assertEquals(InCanada, at(43.6532, -79.3832), "Toronto")
        assertEquals(InCanada, at(45.5019, -73.5674), "Montreal")
        assertEquals(InCanada, at(49.2827, -123.1207), "Vancouver")
        assertEquals(InCanada, at(44.6488, -63.5752), "Halifax")
        assertEquals(InCanada, at(53.5461, -113.4938), "Edmonton")
        assertEquals(InCanada, at(60.7212, -135.0568), "Whitehorse")
        assertEquals(InCanada, at(63.7467, -68.5170), "Iqaluit")
        assertEquals(InCanada, at(48.4284, -123.3656), "Victoria")
    }

    @Test
    fun citiesAbroadAreNamed() {
        assertEquals(Abroad("US"), at(47.6062, -122.3321), "Seattle")
        assertEquals(Abroad("US"), at(40.7128, -74.0060), "New York")
        assertEquals(Abroad("FR"), at(48.8566, 2.3522), "Paris")
        assertEquals(Abroad("MX"), at(19.4326, -99.1332), "Mexico City")
        assertEquals(Abroad("IR"), at(35.6892, 51.3890), "Tehran")
        assertEquals(Abroad("IN"), at(28.6139, 77.2090), "New Delhi")
        assertEquals(Abroad("PM"), at(46.7811, -56.1764), "Saint-Pierre")
    }

    @Test
    fun twinBorderTownsAreUncertain() {
        assertEquals(NearBorder, at(42.3149, -83.0364), "Windsor")
        assertEquals(NearBorder, at(42.3314, -83.0458), "Detroit")
        assertEquals(NearBorder, at(48.9850, -123.0790), "Point Roberts")
        assertEquals(NearBorder, at(45.0045, -72.0985), "Stanstead / Derby Line")
    }

    @Test
    fun theOpenOceanIsAbroadWithNoCountry() {
        assertEquals(Abroad(null), at(40.0, -40.0))
    }
}
