package io.github.mirhamedjz.ehligible.core.geo

import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals

/** Encodes rings exactly like tools/geodata/build_borders.py. Points are (lat, lon). */
internal fun encodeBorders(countries: Map<String, List<List<Pair<Double, Double>>>>): ByteArray {
    val out = mutableListOf<Byte>()
    fun u8(v: Int) = out.add((v and 0xFF).toByte())
    fun u16(v: Int) = repeat(2) { u8(v shr (8 * it)) }
    fun i32(v: Int) = repeat(4) { u8(v shr (8 * it)) }
    fun varint(value: Long) {
        var v = value
        while (true) {
            val byte = (v and 0x7F).toInt()
            v = v ushr 7
            if (v == 0L) {
                u8(byte)
                return
            }
            u8(byte or 0x80)
        }
    }
    fun zigzag(v: Int) = varint(((v.toLong() shl 1) xor (v.toLong() shr 63)))

    "EHB1".forEach { u8(it.code) }
    u16(countries.size)
    for ((code, rings) in countries) {
        code.forEach { u8(it.code) }
        u16(rings.size)
        for (ring in rings) {
            val q = ring.map { (lat, lon) -> (lat * 100_000).roundToInt() to (lon * 100_000).roundToInt() }
            i32(q.minOf { it.first })
            i32(q.minOf { it.second })
            i32(q.maxOf { it.first })
            i32(q.maxOf { it.second })
            i32(q.size)
            i32(q[0].first)
            i32(q[0].second)
            for (i in 1 until q.size) {
                zigzag(q[i].first - q[i - 1].first)
                zigzag(q[i].second - q[i - 1].second)
            }
        }
    }
    return out.toByteArray()
}

private fun box(south: Double, west: Double, north: Double, east: Double) =
    listOf(south to west, south to east, north to east, north to west, south to west)

class BorderIndexTest {
    // A toy world: "Canada" north of 45°N, "US" south of it, and a lake inside Canada as a hole.
    private val index = BorderIndex.parse(
        encodeBorders(
            mapOf(
                "CA" to listOf(box(45.0, -80.0, 50.0, -70.0), box(47.0, -76.0, 47.5, -75.5)),
                "US" to listOf(box(40.0, -80.0, 45.0, -70.0)),
                "FR" to listOf(box(46.7, -56.3, 46.9, -56.1)),
            ),
        ),
    )

    @Test
    fun findsTheCountryContainingAPoint() {
        assertEquals("CA", index.countryAt(48.0, -72.0))
        assertEquals("US", index.countryAt(42.0, -75.0))
        assertEquals(null, index.countryAt(47.2, -75.8)) // the lake hole
        assertEquals(null, index.countryAt(10.0, 10.0))
    }

    @Test
    fun farFromTheBorderIsDefinite() {
        assertEquals(LocationVerdict.InCanada, index.locate(48.0, -72.0, accuracyMeters = 2_000.0))
        assertEquals(LocationVerdict.Abroad("US"), index.locate(42.0, -75.0, accuracyMeters = 2_000.0))
    }

    @Test
    fun withinAccuracyOfTheBorderIsUncertainOnBothSides() {
        // 45.01°N is about 1.1 km north of the border.
        assertEquals(LocationVerdict.NearBorder, index.locate(45.01, -75.0, accuracyMeters = 2_000.0))
        assertEquals(LocationVerdict.NearBorder, index.locate(44.99, -75.0, accuracyMeters = 2_000.0))
        // A worse fix widens the uncertain band: 5.5 km away is uncertain with 8 km accuracy.
        assertEquals(LocationVerdict.InCanada, index.locate(45.05, -75.0, accuracyMeters = 2_000.0))
        assertEquals(LocationVerdict.NearBorder, index.locate(45.05, -75.0, accuracyMeters = 8_000.0))
    }

    @Test
    fun watersNextToCanadaCountAsCanada() {
        // In the middle of the lake, about 19 km from the shore, far from the US.
        assertEquals(LocationVerdict.InCanada, index.locate(47.25, -75.75, accuracyMeters = 2_000.0))
        // Just off the east coast.
        assertEquals(LocationVerdict.InCanada, index.locate(48.0, -69.9, accuracyMeters = 2_000.0))
    }

    @Test
    fun theOpenSeaIsAbroad() {
        assertEquals(LocationVerdict.Abroad(null), index.locate(40.0, -40.0, accuracyMeters = 2_000.0))
        assertEquals(LocationVerdict.Abroad("FR"), index.locate(46.75, -56.35, accuracyMeters = 2_000.0))
    }
}
