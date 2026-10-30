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
import io.github.mirhamedjz.ehligible.app.NotificationText
import io.github.mirhamedjz.ehligible.app.NotificationTexts
import io.github.mirhamedjz.ehligible.app.Notifier
import io.github.mirhamedjz.ehligible.app.resources.Res
import io.github.mirhamedjz.ehligible.app.resources.notif_channel_milestones
import io.github.mirhamedjz.ehligible.app.resources.notif_channel_trips
import io.github.mirhamedjz.ehligible.core.model.Dashboard
import io.github.mirhamedjz.ehligible.core.model.Milestone
import org.jetbrains.compose.resources.getString

/** Posts Eh-ligible's opt-in notifications. The caller checks the user's setting; this checks the OS permission. */
class AndroidNotifier(private val context: Context) : Notifier {
    override suspend fun milestone(milestone: Milestone, dashboard: Dashboard) =
        post(CHANNEL_MILESTONES, ID_MILESTONE, NotificationTexts.milestone(milestone, dashboard))

    override suspend fun travelling(country: String?) =
        post(CHANNEL_TRIPS, ID_TRAVELLING, NotificationTexts.travelling())

    override suspend fun reviewNeeded(days: Int) = post(CHANNEL_TRIPS, ID_REVIEW, NotificationTexts.reviewNeeded())

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

    private fun post(channel: String, id: Int, text: NotificationText) {
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
            .setContentTitle(text.title)
            .setContentText(text.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text.body))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    private companion object {
        const val CHANNEL_MILESTONES = "milestones"
        const val CHANNEL_TRIPS = "trips"
        const val ID_MILESTONE = 1
        const val ID_TRAVELLING = 2
        const val ID_REVIEW = 3
    }
}
