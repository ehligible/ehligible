package io.github.mirhamedjz.ehligible.app.ios

import io.github.mirhamedjz.ehligible.app.data.AppRepository
import io.github.mirhamedjz.ehligible.core.geo.BorderIndex
import io.github.mirhamedjz.ehligible.core.model.AppData
import io.github.mirhamedjz.ehligible.core.presence.Check
import io.github.mirhamedjz.ehligible.core.presence.CheckClassifier
import kotlin.coroutines.resume
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.TimeZone
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLLocationAccuracyReduced
import platform.Foundation.NSError
import platform.Foundation.timeIntervalSinceNow
import platform.UIKit.UIApplication
import platform.UIKit.UIBackgroundTaskInvalid
import platform.darwin.NSObject

private val FIX_TIMEOUT = 20.seconds

/** Someone opening the app just after landing must not get the fix from before the flight. */
private val MAX_FOREGROUND_FIX_AGE = 2.minutes
private val MAX_FIX_AGE = 30.minutes

/**
 * Presence checks on iOS. With the app open: one approximate fix ([checkNow]). With "Always" access, iOS's
 * significant-change service wakes the app (relaunching it in the background if needed) when the phone moves a
 * few hundred metres or more: enough to notice a trip, with almost no battery use. iOS has no public API for the
 * mobile network's country, so the location decides alone. Coordinates never leave this class.
 */
@OptIn(ExperimentalForeignApi::class)
internal class IosLocation(
    private val repository: AppRepository,
    private val borders: suspend () -> BorderIndex,
    private val onBackgroundCheck: suspend (before: AppData) -> Unit,
    private val clock: Clock = Clock.System,
) : NSObject(),
    CLLocationManagerDelegateProtocol {
    private val manager = CLLocationManager()
    private val scope = MainScope()
    private var pendingFix: CancellableContinuation<CLLocation?>? = null

    init {
        manager.delegate = this
        // Approximate is all the app needs, and all it asks for (NSLocationDefaultAccuracyReduced).
        manager.desiredAccuracy = kCLLocationAccuracyReduced
    }

    /** Background monitoring follows the permission: on with "Always", off otherwise. */
    fun syncMonitoring() {
        if (manager.authorizationStatus == kCLAuthorizationStatusAuthorizedAlways) {
            manager.startMonitoringSignificantLocationChanges()
        } else {
            manager.stopMonitoringSignificantLocationChanges()
        }
    }

    suspend fun checkNow(): Check? {
        val status = manager.authorizationStatus
        if (status != kCLAuthorizationStatusAuthorizedWhenInUse && status != kCLAuthorizationStatusAuthorizedAlways) {
            return null
        }
        val location = withTimeoutOrNull(FIX_TIMEOUT) { requestFix() } ?: return null
        return record(location, MAX_FOREGROUND_FIX_AGE)
    }

    private suspend fun requestFix(): CLLocation? = suspendCancellableCoroutine { continuation ->
        finishFix(null)
        pendingFix = continuation
        manager.requestLocation()
    }

    private fun finishFix(location: CLLocation?) {
        val waiting = pendingFix ?: return
        pendingFix = null
        if (waiting.isActive) waiting.resume(location)
    }

    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
        val location = didUpdateLocations.lastOrNull() as? CLLocation ?: return
        if (pendingFix != null) return finishFix(location)
        // A significant-change update, possibly with the app relaunched in the background just for it.
        val app = UIApplication.sharedApplication
        var task = UIBackgroundTaskInvalid
        task = app.beginBackgroundTaskWithName("presence-check") {
            app.endBackgroundTask(task)
            task = UIBackgroundTaskInvalid
        }
        scope.launch {
            try {
                val before = repository.current()
                if (before.profile != null && record(location, MAX_FIX_AGE) != null) onBackgroundCheck(before)
            } finally {
                if (task != UIBackgroundTaskInvalid) app.endBackgroundTask(task)
            }
        }
    }

    override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) = finishFix(null)

    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) = syncMonitoring()

    private suspend fun record(location: CLLocation, maxAge: Duration): Check? {
        val age = (-location.timestamp.timeIntervalSinceNow).seconds
        if (age > maxAge) return null
        val (latitude, longitude) = location.coordinate.useContents { latitude to longitude }
        val verdict = borders().locate(latitude, longitude, location.horizontalAccuracy)
        val check = CheckClassifier.classify(verdict, networkCountry = null) ?: return null
        repository.update { it.recordCheck(check, clock.now(), TimeZone.currentSystemDefault()) }
        return check
    }
}
