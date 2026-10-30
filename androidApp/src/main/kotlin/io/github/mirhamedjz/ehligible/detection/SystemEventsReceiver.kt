package io.github.mirhamedjz.ehligible.detection

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Re-checks after events that often mean travel or a gap in checks: a time-zone change (landing somewhere new),
 * a reboot, or an app update.
 */
class SystemEventsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            -> {
                PresenceWorker.schedule(context)
                PresenceWorker.checkSoon(context)
            }
        }
    }
}
