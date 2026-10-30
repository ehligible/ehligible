package io.github.mirhamedjz.ehligible.core.model

import io.github.mirhamedjz.ehligible.core.rules.CitizenshipRules

/** Moments worth a celebration (confetti in the app, a notification if enabled). Ordered from first to last. */
enum class Milestone {
    QUARTER,
    HALFWAY,
    THREE_QUARTERS,
    DAYS_LEFT_100,
    DAYS_LEFT_30,
    DAYS_LEFT_7,
    DAYS_LEFT_1,
    ELIGIBLE,
    ;

    companion object {
        /** Every milestone [dashboard] has reached. */
        fun reached(dashboard: Dashboard): Set<Milestone> {
            val halfDays = dashboard.projection.creditToDate.halfDays
            val needed = 2 * CitizenshipRules.REQUIRED_DAYS
            val left = dashboard.projection.daysUntilEligible
            return buildSet {
                if (4 * halfDays >= needed) add(QUARTER)
                if (2 * halfDays >= needed) add(HALFWAY)
                if (4 * halfDays >= 3 * needed) add(THREE_QUARTERS)
                if (left != null) {
                    if (left <= 100) add(DAYS_LEFT_100)
                    if (left <= 30) add(DAYS_LEFT_30)
                    if (left <= 7) add(DAYS_LEFT_7)
                    if (left <= 1) add(DAYS_LEFT_1)
                    if (left == 0) add(ELIGIBLE)
                }
            }
        }
    }
}

/**
 * The milestone to celebrate now (the latest newly reached one), plus the updated set of celebrated ids.
 * Reaching several at once (e.g., right after onboarding) celebrates only the latest.
 */
fun AppData.nextCelebration(dashboard: Dashboard): Pair<Milestone?, AppData> {
    val fresh = Milestone.reached(dashboard).filter { it.name !in celebrated }
    if (fresh.isEmpty()) return null to this
    return fresh.max() to copy(celebrated = celebrated + fresh.map { it.name })
}
