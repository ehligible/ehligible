package io.github.mirhamedjz.ehligible.app

import io.github.mirhamedjz.ehligible.core.model.AppData
import io.github.mirhamedjz.ehligible.core.model.dashboard
import io.github.mirhamedjz.ehligible.core.model.nextCelebration
import kotlinx.datetime.toLocalDateTime

/**
 * What happens after a check made while the app isn't on screen: notifications (if the user opted in),
 * one-time milestones, and fresh widgets. Platform workers call [afterCheck] with the data from before the check.
 */
class BackgroundSync(private val graph: AppGraph) {
    suspend fun afterCheck(before: AppData) {
        val now = graph.clock.now()
        val today = now.toLocalDateTime(graph.timeZone()).date
        val after = graph.repository.current()
        val dashboard = after.dashboard(today, now)
        if (dashboard != null) {
            val notify = after.settings.notificationsEnabled
            val wasAbroad = before.lastCheck?.kind?.isCanadaSide == false
            val isAbroad = after.lastCheck?.kind?.isCanadaSide == false
            if (notify && isAbroad && !wasAbroad) graph.notifier.travelling(after.lastCheck?.country)

            val reviewBefore = before.dashboard(today, now)?.reviewDays?.size ?: 0
            if (notify &&
                dashboard.reviewDays.size > reviewBefore
            ) {
                graph.notifier.reviewNeeded(dashboard.reviewDays.size)
            }

            val (milestone, updated) = after.nextCelebration(dashboard)
            if (milestone != null) {
                graph.repository.update { it.copy(celebrated = updated.celebrated) }
                if (notify) graph.notifier.milestone(milestone, dashboard)
            }
        }
        runCatching { graph.widgets.refresh() }
    }
}
