package io.github.mirhamedjz.ehligible.core.presence

import io.github.mirhamedjz.ehligible.core.geo.BorderIndex
import io.github.mirhamedjz.ehligible.core.geo.LocationVerdict

/** One presence check, ready to be recorded in a [DayLog]. */
data class Check(val kind: CheckKind, val country: String? = null)

/** Combines the two signals of a check (docs/SPEC.md, "Presence detection" table). */
object CheckClassifier {
    /**
     * @param location the offline verdict for an approximate location fix, or null if there was no fix.
     * @param networkCountry ISO code of the mobile network the phone is registered on, or null/blank if none.
     */
    fun classify(location: LocationVerdict?, networkCountry: String?): Check? {
        val network = networkCountry?.trim()?.uppercase()?.takeIf { it.length == 2 }
        val foreignNetwork = network?.takeIf { it != BorderIndex.CANADA }
        return when (location) {
            LocationVerdict.InCanada -> Check(CheckKind.INSIDE)

            is LocationVerdict.Abroad -> Check(CheckKind.OUTSIDE, location.country ?: foreignNetwork)

            LocationVerdict.NearBorder ->
                if (foreignNetwork != null) Check(CheckKind.BORDER_FOREIGN, foreignNetwork) else Check(CheckKind.BORDER)

            null -> when {
                network == null -> null
                foreignNetwork == null -> Check(CheckKind.INSIDE)
                else -> Check(CheckKind.OUTSIDE, foreignNetwork)
            }
        }
    }
}
