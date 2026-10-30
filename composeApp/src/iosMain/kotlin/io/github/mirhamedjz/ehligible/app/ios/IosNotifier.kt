package io.github.mirhamedjz.ehligible.app.ios

import io.github.mirhamedjz.ehligible.app.NotificationText
import io.github.mirhamedjz.ehligible.app.NotificationTexts
import io.github.mirhamedjz.ehligible.app.Notifier
import io.github.mirhamedjz.ehligible.core.model.Dashboard
import io.github.mirhamedjz.ehligible.core.model.Milestone
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter

/** Posts Eh-ligible's opt-in notifications. The caller checks the user's setting; iOS drops them if not allowed. */
internal object IosNotifier : Notifier {
    override suspend fun milestone(milestone: Milestone, dashboard: Dashboard) =
        post("milestone", NotificationTexts.milestone(milestone, dashboard))

    override suspend fun travelling(country: String?) = post("travelling", NotificationTexts.travelling())

    override suspend fun reviewNeeded(days: Int) = post("review", NotificationTexts.reviewNeeded())

    private fun post(id: String, text: NotificationText) {
        val content = UNMutableNotificationContent()
        content.setTitle(text.title)
        content.setBody(text.body)
        content.setSound(UNNotificationSound.defaultSound())
        val request = UNNotificationRequest.requestWithIdentifier(id, content, trigger = null)
        UNUserNotificationCenter.currentNotificationCenter().addNotificationRequest(
            request,
            withCompletionHandler = null,
        )
    }
}
