package io.github.mirhamedjz.ehligible.detection

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.LocationResult
import io.github.mirhamedjz.ehligible.EhligibleApp
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Records a check for each location the fused provider delivers in the background ([BackgroundLocation]). */
class LocationUpdatesReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Availability changes arrive on the same intent without a location; there's nothing to record then.
        val location = LocationResult.extractResult(intent)?.lastLocation ?: return
        val app = context.applicationContext as EhligibleApp
        val pending = goAsync()
        app.appScope.launch {
            try {
                withTimeoutOrNull(BUDGET_MILLIS) {
                    val before = app.graph.repository.current()
                    if (before.profile != null && app.checkRunner.recordDelivered(location) != null) {
                        app.backgroundSync.afterCheck(before)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        /** Work started with goAsync() must finish well within the 10-second broadcast limit. */
        const val BUDGET_MILLIS = 8_000L
    }
}
