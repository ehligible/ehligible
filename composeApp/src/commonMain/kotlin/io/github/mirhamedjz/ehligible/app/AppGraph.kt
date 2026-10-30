package io.github.mirhamedjz.ehligible.app

import io.github.mirhamedjz.ehligible.app.data.AppRepository
import io.github.mirhamedjz.ehligible.core.model.Dashboard
import io.github.mirhamedjz.ehligible.core.model.Milestone
import io.github.mirhamedjz.ehligible.core.presence.Check
import kotlin.time.Clock
import kotlinx.datetime.TimeZone

/** Runs a presence check right now and records it. Returns the check, or null when there was no signal. */
fun interface PresenceChecker {
    suspend fun checkNow(): Check?
}

/** Pushes fresh numbers to home-screen widgets. */
fun interface WidgetRefresher {
    suspend fun refresh()
}

/** Posts the opt-in notifications. Implementations must respect the user's setting and OS permission. */
interface Notifier {
    suspend fun milestone(milestone: Milestone, dashboard: Dashboard)

    suspend fun travelling(country: String?)

    suspend fun reviewNeeded(days: Int)

    companion object {
        val NONE: Notifier = object : Notifier {
            override suspend fun milestone(milestone: Milestone, dashboard: Dashboard) = Unit

            override suspend fun travelling(country: String?) = Unit

            override suspend fun reviewNeeded(days: Int) = Unit
        }
    }
}

/** Facts about the running app build. */
data class AppInfo(
    val versionName: String,
    /** False for the first Play release, which ships without background location (docs/SPEC.md). */
    val backgroundLocationSupported: Boolean,
)

/** App-wide services, created once per process by the platform entry point. */
class AppGraph(
    val repository: AppRepository,
    val presenceChecker: PresenceChecker,
    val widgets: WidgetRefresher,
    val notifier: Notifier,
    val appInfo: AppInfo,
    val clock: Clock = Clock.System,
    val timeZone: () -> TimeZone = { TimeZone.currentSystemDefault() },
)
