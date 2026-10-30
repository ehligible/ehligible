package io.github.mirhamedjz.ehligible.core.presence

import kotlinx.serialization.Serializable

/** The result of one presence check (see docs/SPEC.md, "Presence detection"). */
enum class CheckKind {
    /** In Canada, away from any foreign border, or on a Canadian mobile network. */
    INSIDE,

    /** Outside Canada, away from the Canadian border. */
    OUTSIDE,

    /** Within location accuracy of the border, with no sign of a foreign network. */
    BORDER,

    /** Within location accuracy of the border while the phone is on a foreign network. */
    BORDER_FOREIGN,
    ;

    val isCanadaSide: Boolean get() = this == INSIDE || this == BORDER
}

/** Every check recorded on one local calendar day. Coordinates are never stored. */
@Serializable
data class DayLog(
    val inside: Int = 0,
    val outside: Int = 0,
    val border: Int = 0,
    val borderForeign: Int = 0,
    val first: CheckKind? = null,
    val last: CheckKind? = null,
    /** ISO 3166-1 alpha-2 codes of foreign countries seen that day. */
    val countries: Set<String> = emptySet(),
) {
    val isEmpty: Boolean get() = first == null
    val hasForeignChecks: Boolean get() = outside + borderForeign > 0

    fun record(kind: CheckKind, country: String? = null): DayLog = copy(
        inside = inside + if (kind == CheckKind.INSIDE) 1 else 0,
        outside = outside + if (kind == CheckKind.OUTSIDE) 1 else 0,
        border = border + if (kind == CheckKind.BORDER) 1 else 0,
        borderForeign = borderForeign + if (kind == CheckKind.BORDER_FOREIGN) 1 else 0,
        first = first ?: kind,
        last = kind,
        countries = if (country != null && !kind.isCanadaSide) countries + country.uppercase() else countries,
    )
}

/** What the user told us about a specific day; always wins over detection. */
@Serializable
enum class UserVerdict { PRESENT, ABSENT }

/** How a calendar day is counted. Days needing review count as absent until the user confirms. */
enum class DayVerdict(val countsAsPresent: Boolean) {
    /** In Canada (a check in Canada, or confirmed by the user). */
    PRESENT(true),

    /** Left or came back that day: in Canada for part of it, so it counts as present. */
    TRIP_DAY(true),

    /** No checks, but surrounded by days in Canada (or before tracking / in the future, with no trip). */
    ASSUMED_PRESENT(true),

    /** A full day outside Canada. */
    ABSENT(false),

    /** Uncertain; counted as absent until the user confirms. */
    NEEDS_REVIEW(false),
}
