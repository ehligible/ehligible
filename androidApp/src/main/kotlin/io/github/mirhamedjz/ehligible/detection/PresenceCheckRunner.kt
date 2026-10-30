package io.github.mirhamedjz.ehligible.detection

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.SystemClock
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import io.github.mirhamedjz.ehligible.BuildConfig
import io.github.mirhamedjz.ehligible.app.data.AppRepository
import io.github.mirhamedjz.ehligible.core.geo.BorderIndex
import io.github.mirhamedjz.ehligible.core.presence.Check
import io.github.mirhamedjz.ehligible.core.presence.CheckClassifier
import kotlin.time.Clock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.TimeZone

/**
 * One presence check on Android: an approximate location fix (when allowed) plus the mobile network's country,
 * classified offline against the border data and recorded for today. Coordinates never leave this function.
 */
class PresenceCheckRunner(
    private val context: Context,
    private val repository: AppRepository,
    private val borders: suspend () -> BorderIndex,
    private val clock: Clock = Clock.System,
) {
    suspend fun run(foreground: Boolean): Check? {
        val allowed = mayUseLocation(foreground)
        val location = if (allowed) currentLocation(foreground) else null
        val verdict = location?.let { borders().locate(it.latitude, it.longitude, it.accuracy.toDouble()) }
        val network = networkCountry()
        val check = CheckClassifier.classify(verdict, network)
        // Debug builds only, and never coordinates: just which signals were available and the outcome.
        if (BuildConfig.DEBUG) {
            Log.d(
                TAG,
                "foreground=$foreground location=$allowed fix=${location != null} " +
                    "verdict=$verdict network=$network -> $check",
            )
        }
        if (check == null) return null
        repository.update { it.recordCheck(check, clock.now(), TimeZone.currentSystemDefault()) }
        return check
    }

    /**
     * Foreground checks need approximate location. Background checks additionally need the background
     * permission, which only builds with background location (v1.1+, see docs/SPEC.md) ask for.
     */
    private fun mayUseLocation(foreground: Boolean): Boolean {
        if (!granted(Manifest.permission.ACCESS_COARSE_LOCATION)) return false
        if (foreground) return true
        if (!BuildConfig.BACKGROUND_LOCATION) return false
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || granted(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    }

    private fun granted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    @OptIn(ExperimentalCoroutinesApi::class)
    @SuppressLint("MissingPermission") // Checked in mayUseLocation().
    private suspend fun currentLocation(foreground: Boolean): Location? = runCatching {
        val client = LocationServices.getFusedLocationProviderClient(context)
        val request = CurrentLocationRequest.Builder()
            // In the foreground a brief GPS fix is allowed: it works abroad in airplane mode, where Wi-Fi/cell
            // positioning can't. The OS still coarsens the result because the app only holds approximate access.
            .setPriority(if (foreground) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY)
            .setMaxUpdateAgeMillis(if (foreground) MAX_FOREGROUND_FIX_AGE_MILLIS else MAX_FIX_AGE_MILLIS)
            .setDurationMillis(FIX_TIMEOUT_MILLIS)
            .build()
        val cancellation = CancellationTokenSource()
        val fresh =
            withTimeoutOrNull(FIX_TIMEOUT_MILLIS + 2_000) {
                client.getCurrentLocation(request, cancellation.token).await(cancellation)
            }
        val maxAge = if (foreground) MAX_FOREGROUND_FIX_AGE_MILLIS else MAX_FIX_AGE_MILLIS
        fresh ?: client.lastLocation.await()?.takeIf { it.ageMillis() <= maxAge }
    }.onFailure { if (BuildConfig.DEBUG) Log.w(TAG, "Location request failed", it) }.getOrNull()

    private fun Location.ageMillis(): Long = (SystemClock.elapsedRealtimeNanos() - elapsedRealtimeNanos) / 1_000_000

    /** The country of the mobile network the phone is registered on (not the SIM's home country). */
    private fun networkCountry(): String? {
        val telephony = context.getSystemService(TelephonyManager::class.java) ?: return null
        return telephony.networkCountryIso?.takeIf { it.isNotBlank() }
    }

    private companion object {
        const val TAG = "PresenceCheck"
        const val FIX_TIMEOUT_MILLIS = 20_000L
        const val MAX_FIX_AGE_MILLIS = 30 * 60_000L

        /** Someone opening the app just after landing must not get the fix from before the flight. */
        const val MAX_FOREGROUND_FIX_AGE_MILLIS = 2 * 60_000L
    }
}
