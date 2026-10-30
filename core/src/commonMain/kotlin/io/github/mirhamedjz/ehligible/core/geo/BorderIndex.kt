package io.github.mirhamedjz.ehligible.core.geo

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** Where an approximate location fix puts the person, relative to Canada. */
sealed interface LocationVerdict {
    data object InCanada : LocationVerdict

    /** Outside Canada; [country] is an ISO 3166-1 alpha-2 code, or null when unknown (e.g., at sea). */
    data class Abroad(val country: String?) : LocationVerdict

    /** Too close to the border to tell, given the fix's accuracy. */
    data object NearBorder : LocationVerdict
}

/**
 * Offline country lookup over the boundary file built by tools/geodata/build_borders.py.
 * Nothing here touches the network; coordinates are only used in memory.
 */
class BorderIndex private constructor(private val countries: List<CountryShape>) {
    private class Ring(
        val minLat: Int,
        val minLon: Int,
        val maxLat: Int,
        val maxLon: Int,
        /** Interleaved lat, lon pairs in 1e-5 degrees. */
        val coords: IntArray,
    ) {
        val size: Int get() = coords.size / 2
    }

    private class CountryShape(val code: String, val rings: List<Ring>)

    private val canada: CountryShape = countries.first { it.code == CANADA }
    private val foreign: List<CountryShape> = countries.filter { it.code != CANADA }

    /** The country containing the point, or null at sea / on an islet missing from the data. */
    fun countryAt(lat: Double, lon: Double): String? {
        if (canada.contains(lat, lon)) return CANADA
        return foreign.firstOrNull { it.contains(lat, lon) }?.code
    }

    /**
     * Classifies a location fix. A point within [accuracyMeters] (at least [MIN_UNCERTAINTY_METERS]) of the border
     * between Canada and another country is [LocationVerdict.NearBorder]. Points at sea within
     * [TERRITORIAL_WATERS_METERS] of Canada (and closer to Canada than to foreign land) count as Canada.
     */
    fun locate(lat: Double, lon: Double, accuracyMeters: Double): LocationVerdict {
        val band = accuracyMeters.coerceIn(MIN_UNCERTAINTY_METERS, MAX_UNCERTAINTY_METERS)
        return when (val country = countryAt(lat, lon)) {
            CANADA -> if (nearestForeign(lat, lon, band) !=
                null
            ) {
                LocationVerdict.NearBorder
            } else {
                LocationVerdict.InCanada
            }

            null -> locateAtSea(lat, lon, band)

            else -> if (canada.distanceMeters(lat, lon, band) <=
                band
            ) {
                LocationVerdict.NearBorder
            } else {
                LocationVerdict.Abroad(country)
            }
        }
    }

    private fun locateAtSea(lat: Double, lon: Double, band: Double): LocationVerdict {
        val toCanada = canada.distanceMeters(lat, lon, TERRITORIAL_WATERS_METERS)
        val nearest = nearestForeign(lat, lon, TERRITORIAL_WATERS_METERS)
        val canadianWaters = toCanada <= TERRITORIAL_WATERS_METERS && (nearest == null || toCanada < nearest.second)
        return when {
            canadianWaters -> if (nearest != null &&
                nearest.second <= band
            ) {
                LocationVerdict.NearBorder
            } else {
                LocationVerdict.InCanada
            }

            toCanada <= band -> LocationVerdict.NearBorder

            else -> LocationVerdict.Abroad(nearest?.first)
        }
    }

    /** The closest foreign country within [limitMeters], with its distance. */
    private fun nearestForeign(lat: Double, lon: Double, limitMeters: Double): Pair<String, Double>? {
        var best: Pair<String, Double>? = null
        for (shape in foreign) {
            val distance = shape.distanceMeters(lat, lon, limitMeters)
            if (distance <= limitMeters && (best == null || distance < best.second)) best = shape.code to distance
        }
        return best
    }

    private fun CountryShape.contains(lat: Double, lon: Double): Boolean {
        val y = lat * SCALE
        val x = lon * SCALE
        var inside = false
        for (ring in rings) {
            if (y < ring.minLat || y > ring.maxLat || x < ring.minLon || x > ring.maxLon) continue
            val c = ring.coords
            var j = ring.size - 1
            for (i in 0 until ring.size) {
                val yi = c[2 * i].toDouble()
                val xi = c[2 * i + 1].toDouble()
                val yj = c[2 * j].toDouble()
                val xj = c[2 * j + 1].toDouble()
                if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) inside = !inside
                j = i
            }
        }
        return inside
    }

    /** Distance in meters from the point to this country's outline, or [Double.MAX_VALUE] if beyond [limitMeters]. */
    private fun CountryShape.distanceMeters(lat: Double, lon: Double, limitMeters: Double): Double {
        val metersPerLonDegree = METERS_PER_LAT_DEGREE * max(cos(lat * PI / 180.0), 0.05)
        val padLat = limitMeters / METERS_PER_LAT_DEGREE * SCALE
        val padLon = limitMeters / metersPerLonDegree * SCALE
        val y = lat * SCALE
        val x = lon * SCALE
        var best = Double.MAX_VALUE
        for (ring in rings) {
            if (y < ring.minLat - padLat || y > ring.maxLat + padLat) continue
            if (x < ring.minLon - padLon || x > ring.maxLon + padLon) continue
            val c = ring.coords
            for (i in 0 until ring.size - 1) {
                val ay = (c[2 * i] - y) / SCALE * METERS_PER_LAT_DEGREE
                val ax = (c[2 * i + 1] - x) / SCALE * metersPerLonDegree
                val by = (c[2 * i + 2] - y) / SCALE * METERS_PER_LAT_DEGREE
                val bx = (c[2 * i + 3] - x) / SCALE * metersPerLonDegree
                best = min(best, distanceToSegment(ax, ay, bx, by))
            }
        }
        return best
    }

    /** Distance from the origin to segment AB, in the local planar projection. */
    private fun distanceToSegment(ax: Double, ay: Double, bx: Double, by: Double): Double {
        val dx = bx - ax
        val dy = by - ay
        val lengthSquared = dx * dx + dy * dy
        val t = if (lengthSquared == 0.0) 0.0 else ((-ax * dx - ay * dy) / lengthSquared).coerceIn(0.0, 1.0)
        val px = ax + t * dx
        val py = ay + t * dy
        return sqrt(px * px + py * py)
    }

    companion object {
        const val CANADA: String = "CA"

        /** Android's approximate location is accurate to roughly 2-3 km. */
        const val MIN_UNCERTAINTY_METERS: Double = 2_500.0
        const val MAX_UNCERTAINTY_METERS: Double = 15_000.0
        const val TERRITORIAL_WATERS_METERS: Double = 25_000.0

        private const val SCALE = 100_000.0
        private const val METERS_PER_LAT_DEGREE = 111_195.0

        fun parse(bytes: ByteArray): BorderIndex {
            val reader = Reader(bytes)
            check(reader.ascii(4) == "EHB1") { "Not a border file" }
            val countries = List(reader.u16()) {
                val code = reader.ascii(2)
                val rings = List(reader.u16()) {
                    val minLat = reader.i32()
                    val minLon = reader.i32()
                    val maxLat = reader.i32()
                    val maxLon = reader.i32()
                    val points = reader.i32()
                    val coords = IntArray(points * 2)
                    coords[0] = reader.i32()
                    coords[1] = reader.i32()
                    for (p in 1 until points) {
                        coords[2 * p] = coords[2 * p - 2] + reader.zigzag()
                        coords[2 * p + 1] = coords[2 * p - 1] + reader.zigzag()
                    }
                    Ring(minLat, minLon, maxLat, maxLon, coords)
                }
                CountryShape(code, rings)
            }
            return BorderIndex(countries)
        }
    }

    private class Reader(private val bytes: ByteArray) {
        private var position = 0

        fun u8(): Int = bytes[position++].toInt() and 0xFF

        fun u16(): Int = u8() or (u8() shl 8)

        fun i32(): Int = u8() or (u8() shl 8) or (u8() shl 16) or (u8() shl 24)

        fun ascii(length: Int): String = buildString { repeat(length) { append(u8().toChar()) } }

        fun zigzag(): Int {
            var result = 0L
            var shift = 0
            while (true) {
                val byte = u8()
                result = result or ((byte and 0x7F).toLong() shl shift)
                if (byte and 0x80 == 0) break
                shift += 7
            }
            return ((result ushr 1) xor -(result and 1)).toInt()
        }
    }
}
