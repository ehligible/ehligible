package io.github.mirhamedjz.ehligible.app

import io.github.mirhamedjz.ehligible.app.resources.Res
import io.github.mirhamedjz.ehligible.app.resources.milestone_DAYS_LEFT_1
import io.github.mirhamedjz.ehligible.app.resources.milestone_DAYS_LEFT_100
import io.github.mirhamedjz.ehligible.app.resources.milestone_DAYS_LEFT_30
import io.github.mirhamedjz.ehligible.app.resources.milestone_DAYS_LEFT_7
import io.github.mirhamedjz.ehligible.app.resources.milestone_ELIGIBLE
import io.github.mirhamedjz.ehligible.app.resources.milestone_HALFWAY
import io.github.mirhamedjz.ehligible.app.resources.milestone_QUARTER
import io.github.mirhamedjz.ehligible.app.resources.milestone_THREE_QUARTERS
import io.github.mirhamedjz.ehligible.app.resources.notif_days_to_go
import io.github.mirhamedjz.ehligible.app.resources.notif_eligible_body
import io.github.mirhamedjz.ehligible.app.resources.notif_review_body
import io.github.mirhamedjz.ehligible.app.resources.notif_review_title
import io.github.mirhamedjz.ehligible.app.resources.notif_travelling_body
import io.github.mirhamedjz.ehligible.app.resources.notif_travelling_title
import io.github.mirhamedjz.ehligible.core.model.Dashboard
import io.github.mirhamedjz.ehligible.core.model.Milestone
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString

/** A notification's title and body, in the app's language. */
data class NotificationText(val title: String, val body: String)

/** The texts of the opt-in notifications, shared by the platform [Notifier]s. */
object NotificationTexts {
    suspend fun milestone(milestone: Milestone, dashboard: Dashboard): NotificationText {
        val left = dashboard.projection.daysUntilEligible
        val body = if (milestone == Milestone.ELIGIBLE || left == null) {
            getString(Res.string.notif_eligible_body)
        } else {
            getPluralString(Res.plurals.notif_days_to_go, left, left)
        }
        return NotificationText(getString(milestone.title), body)
    }

    suspend fun travelling(): NotificationText =
        NotificationText(getString(Res.string.notif_travelling_title), getString(Res.string.notif_travelling_body))

    suspend fun reviewNeeded(): NotificationText =
        NotificationText(getString(Res.string.notif_review_title), getString(Res.string.notif_review_body))
}

/** The milestone's headline, e.g. "Halfway there!". */
val Milestone.title: StringResource
    get() = when (this) {
        Milestone.QUARTER -> Res.string.milestone_QUARTER
        Milestone.HALFWAY -> Res.string.milestone_HALFWAY
        Milestone.THREE_QUARTERS -> Res.string.milestone_THREE_QUARTERS
        Milestone.DAYS_LEFT_100 -> Res.string.milestone_DAYS_LEFT_100
        Milestone.DAYS_LEFT_30 -> Res.string.milestone_DAYS_LEFT_30
        Milestone.DAYS_LEFT_7 -> Res.string.milestone_DAYS_LEFT_7
        Milestone.DAYS_LEFT_1 -> Res.string.milestone_DAYS_LEFT_1
        Milestone.ELIGIBLE -> Res.string.milestone_ELIGIBLE
    }
