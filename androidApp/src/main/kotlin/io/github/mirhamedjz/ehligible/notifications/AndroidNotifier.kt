package io.github.mirhamedjz.ehligible.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.mirhamedjz.ehligible.MainActivity
import io.github.mirhamedjz.ehligible.R
import io.github.mirhamedjz.ehligible.app.Notifier
import io.github.mirhamedjz.ehligible.app.resources.Res
import io.github.mirhamedjz.ehligible.app.resources.milestone_DAYS_LEFT_1
import io.github.mirhamedjz.ehligible.app.resources.milestone_DAYS_LEFT_100
import io.github.mirhamedjz.ehligible.app.resources.milestone_DAYS_LEFT_30
import io.github.mirhamedjz.ehligible.app.resources.milestone_DAYS_LEFT_7
import io.github.mirhamedjz.ehligible.app.resources.milestone_ELIGIBLE
import io.github.mirhamedjz.ehligible.app.resources.milestone_HALFWAY
import io.github.mirhamedjz.ehligible.app.resources.milestone_QUARTER
import io.github.mirhamedjz.ehligible.app.resources.milestone_THREE_QUARTERS
import io.github.mirhamedjz.ehligible.app.resources.notif_channel_milestones
import io.github.mirhamedjz.ehligible.app.resources.notif_channel_trips
import io.github.mirhamedjz.ehligible.app.resources.notif_days_to_go
import io.github.mirhamedjz.ehligible.app.resources.notif_eligible_body
import io.github.mirhamedjz.ehligible.app.resources.notif_review_body
import io.github.mirhamedjz.ehligible.app.resources.notif_review_title
import io.github.mirhamedjz.ehligible.app.resources.notif_travelling_body
import io.github.mirhamedjz.ehligible.app.resources.notif_travelling_title
import io.github.mirhamedjz.ehligible.core.model.Dashboard
import io.github.mirhamedjz.ehligible.core.model.Milestone
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString

/** Posts Eh-ligible's opt-in notifications. The caller checks the user's setting; this checks the OS permission. */
class AndroidNotifier(private val context: Context) : Notifier {
    override suspend fun milestone(milestone: Milestone, dashboard: Dashboard) {
        val title = getString(milestone.titleRes())
        val left = dashboard.projection.daysUntilEligible
        val body = if (milestone == Milestone.ELIGIBLE || left == null) {
            getString(Res.string.notif_eligible_body)
        } else {
            getPluralString(Res.plurals.notif_days_to_go, left, left)
        }
        post(CHANNEL_MILESTONES, ID_MILESTONE, title, body)
    }

    override suspend fun travelling(country: String?) {
        post(
            CHANNEL_TRIPS,
            ID_TRAVELLING,
            getString(Res.string.notif_travelling_title),
            getString(Res.string.notif_travelling_body),
        )
    }

    override suspend fun reviewNeeded(days: Int) {
        post(
            CHANNEL_TRIPS,
            ID_REVIEW,
            getString(Res.string.notif_review_title),
            getString(Res.string.notif_review_body),
        )
    }

    /** Creates (or renames, after a language change) the notification channels. */
    suspend fun createChannels() {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_MILESTONES,
                getString(Res.string.notif_channel_milestones),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_TRIPS,
                getString(Res.string.notif_channel_trips),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
    }

    private fun post(channel: String, id: Int, title: String, body: String) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED &&
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
        ) {
            return
        }
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(
                context,
                MainActivity::class.java,
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    private fun Milestone.titleRes() = when (this) {
        Milestone.QUARTER -> Res.string.milestone_QUARTER
        Milestone.HALFWAY -> Res.string.milestone_HALFWAY
        Milestone.THREE_QUARTERS -> Res.string.milestone_THREE_QUARTERS
        Milestone.DAYS_LEFT_100 -> Res.string.milestone_DAYS_LEFT_100
        Milestone.DAYS_LEFT_30 -> Res.string.milestone_DAYS_LEFT_30
        Milestone.DAYS_LEFT_7 -> Res.string.milestone_DAYS_LEFT_7
        Milestone.DAYS_LEFT_1 -> Res.string.milestone_DAYS_LEFT_1
        Milestone.ELIGIBLE -> Res.string.milestone_ELIGIBLE
    }

    private companion object {
        const val CHANNEL_MILESTONES = "milestones"
        const val CHANNEL_TRIPS = "trips"
        const val ID_MILESTONE = 1
        const val ID_TRAVELLING = 2
        const val ID_REVIEW = 3
    }
}
