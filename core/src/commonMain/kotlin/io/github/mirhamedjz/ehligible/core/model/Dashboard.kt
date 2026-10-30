package io.github.mirhamedjz.ehligible.core.model

import io.github.mirhamedjz.ehligible.core.presence.CheckKind
import io.github.mirhamedjz.ehligible.core.presence.PresenceTimeline
import io.github.mirhamedjz.ehligible.core.presence.TimelineTrip
import io.github.mirhamedjz.ehligible.core.rules.CitizenshipRules
import io.github.mirhamedjz.ehligible.core.rules.EligibilityCalculator
import io.github.mirhamedjz.ehligible.core.rules.EligibilityProjection
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

/** Where the person is right now, for the status chip. */
sealed interface CurrentStatus {
    data object InCanada : CurrentStatus

    data class Abroad(val country: String?) : CurrentStatus

    data object NearBorder : CurrentStatus

    data object Unknown : CurrentStatus
}

/** Everything the home screen and the widget show, derived from [AppData] for one day. */
data class Dashboard(
    val today: LocalDate,
    val profile: Profile,
    val projection: EligibilityProjection,
    val target: CountdownTarget,
    val currentStatus: CurrentStatus,
    val reviewDays: List<LocalDate>,
    val trips: List<TimelineTrip>,
) {
    /** The date the countdown points at (exact first eligible date, or the safer date). */
    val countdownDate: LocalDate?
        get() = when (target) {
            CountdownTarget.EXACT -> projection.firstEligibleDate
            CountdownTarget.SAFER -> projection.saferApplyDate
        }

    val daysLeft: Int? get() = countdownDate?.let { today.daysUntil(it) }

    val creditedDays: Int get() = projection.creditToDate.creditedDays

    /** Share of the 1,095 days earned so far, from 0 to 1. */
    val progress: Float get() =
        (projection.creditToDate.halfDays / (2f * CitizenshipRules.REQUIRED_DAYS)).coerceIn(0f, 1f)

    val isEligibleToday: Boolean get() = projection.isEligibleToday
}

/** Builds the [Dashboard], or null before onboarding is done. */
fun AppData.dashboard(today: LocalDate, now: Instant): Dashboard? {
    val profile = profile ?: return null
    val timeline = timeline(today) ?: return null
    val calculator = EligibilityCalculator(profile.prDate, profile.prePrStatus, timeline)
    return Dashboard(
        today = today,
        profile = profile,
        projection = calculator.project(today, settings.marginDays),
        target = settings.countdownTarget,
        currentStatus = currentStatus(today, now),
        reviewDays = timeline.daysNeedingReview(),
        trips = timeline.trips(tripNotes),
    )
}

fun AppData.timeline(today: LocalDate): PresenceTimeline? {
    val profile = profile ?: return null
    return PresenceTimeline(profile.trackingStart, today, dayLogs, trips, userVerdicts)
}

private val RECENT = 36.hours

private fun AppData.currentStatus(today: LocalDate, now: Instant): CurrentStatus {
    val check = lastCheck
    if (check != null && now - Instant.fromEpochMilliseconds(check.epochMillis) <= RECENT) {
        return when (check.kind) {
            CheckKind.INSIDE -> CurrentStatus.InCanada
            CheckKind.BORDER -> CurrentStatus.NearBorder
            CheckKind.OUTSIDE, CheckKind.BORDER_FOREIGN -> CurrentStatus.Abroad(check.country)
        }
    }
    val enteredTrip = trips.firstOrNull { it.isAbsentOn(today) }
    return if (enteredTrip != null) CurrentStatus.Abroad(null) else CurrentStatus.Unknown
}
