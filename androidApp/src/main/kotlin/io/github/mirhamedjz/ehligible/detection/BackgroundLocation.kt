package io.github.mirhamedjz.ehligible.detection

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import io.github.mirhamedjz.ehligible.BuildConfig

/**
 * Background location (v1.1+, docs/SPEC.md). Android throttles on-demand fixes for apps in the background, so instead
 * the fused provider delivers an approximate location every few hours to [LocationUpdatesReceiver], even while the
 * app is closed. Registrations don't survive a reboot or an app update, so [sync] runs at every process start, when
 * the app comes to the foreground and from the periodic worker; it is idempotent.
 */
object BackgroundLocation {
    private const val TAG = "PresenceCheck"
    private const val INTERVAL_MILLIS = 3 * 60 * 60_000L
    private const val MIN_INTERVAL_MILLIS = 60 * 60_000L

    /** Approximate location while the app is closed: only builds with background location ever ask for it. */
    fun allowed(context: Context): Boolean {
        if (!BuildConfig.BACKGROUND_LOCATION || !context.has(Manifest.permission.ACCESS_COARSE_LOCATION)) return false
        // Before Android 10, approximate location already applies in the background.
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            context.has(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    }

    @SuppressLint("MissingPermission") // Checked in allowed().
    fun sync(context: Context) {
        if (!BuildConfig.BACKGROUND_LOCATION) return
        val client = LocationServices.getFusedLocationProviderClient(context)
        val updates = updatesIntent(context)
        if (!allowed(context)) {
            client.removeLocationUpdates(updates)
            return
        }
        // Wi-Fi and cell positioning: cheap enough to run all day. Checks with the app open add a brief GPS fix.
        val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, INTERVAL_MILLIS)
            .setMinUpdateIntervalMillis(MIN_INTERVAL_MILLIS)
            .build()
        runCatching { client.requestLocationUpdates(request, updates) }
            .onFailure { if (BuildConfig.DEBUG) Log.w(TAG, "Background updates not registered", it) }
    }

    private fun updatesIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, LocationUpdatesReceiver::class.java),
        // Mutable: the fused provider adds the locations as extras.
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
    )

    private fun Context.has(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}
