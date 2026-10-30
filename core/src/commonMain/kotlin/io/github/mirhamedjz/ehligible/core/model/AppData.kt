package io.github.mirhamedjz.ehligible.core.model

import io.github.mirhamedjz.ehligible.core.presence.Check
import io.github.mirhamedjz.ehligible.core.presence.CheckKind
import io.github.mirhamedjz.ehligible.core.presence.DayLog
import io.github.mirhamedjz.ehligible.core.presence.Trip
import io.github.mirhamedjz.ehligible.core.presence.TripNote
import io.github.mirhamedjz.ehligible.core.presence.UserVerdict
import io.github.mirhamedjz.ehligible.core.rules.PrePrStatus
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable

/** Everything the app stores. One versioned JSON document, kept only on the device (and in backups). */
@Serializable
data class AppData(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val profile: Profile? = null,
    val trips: List<Trip> = emptyList(),
    val dayLogs: Map<LocalDate, DayLog> = emptyMap(),
    val userVerdicts: Map<LocalDate, UserVerdict> = emptyMap(),
    val tripNotes: Map<LocalDate, TripNote> = emptyMap(),
    val settings: Settings = Settings(),
    val lastCheck: LastCheck? = null,
    /** Milestones already celebrated, so each one fires once. */
    val celebrated: Set<String> = emptySet(),
) {
    /** Records a check on the local calendar day where the phone was at [at]. */
    fun recordCheck(check: Check, at: Instant, zone: TimeZone): AppData {
        val date = at.toLocalDateTime(zone).date
        val log = dayLogs[date] ?: DayLog()
        return copy(
            dayLogs = dayLogs + (date to log.record(check.kind, check.country)),
            lastCheck = LastCheck(at.toEpochMilliseconds(), check.kind, check.country),
        )
    }

    fun confirmDay(date: LocalDate, verdict: UserVerdict?): AppData =
        copy(userVerdicts = if (verdict == null) userVerdicts - date else userVerdicts + (date to verdict))

    fun upsertTrip(trip: Trip): AppData = copy(trips = trips.filterNot { it.id == trip.id } + trip)

    fun deleteTrip(id: String): AppData = copy(trips = trips.filterNot { it.id == id })

    fun annotateDetectedTrip(departure: LocalDate, note: TripNote): AppData =
        copy(tripNotes = tripNotes + (departure to note))

    companion object {
        const val CURRENT_SCHEMA_VERSION: Int = 1
    }
}

@Serializable
data class Profile(
    val prDate: LocalDate,
    val prePrStatus: PrePrStatus = PrePrStatus.NONE,
    /** The day automatic detection started; earlier days rely on entered trips. */
    val trackingStart: LocalDate,
    val birthYear: Int? = null,
)

@Serializable
enum class CountdownTarget { EXACT, SAFER }

@Serializable
data class Settings(
    val marginDays: Int = DEFAULT_MARGIN_DAYS,
    val countdownTarget: CountdownTarget = CountdownTarget.EXACT,
    val notificationsEnabled: Boolean = false,
    /** Ids of ticked "Ready to apply?" items. */
    val checklist: Set<String> = emptySet(),
) {
    companion object {
        const val DEFAULT_MARGIN_DAYS: Int = 7
        const val MAX_MARGIN_DAYS: Int = 60
    }
}

@Serializable
data class LastCheck(val epochMillis: Long, val kind: CheckKind, val country: String? = null)
