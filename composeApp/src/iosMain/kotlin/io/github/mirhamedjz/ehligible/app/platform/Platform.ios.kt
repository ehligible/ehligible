package io.github.mirhamedjz.ehligible.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import io.github.mirhamedjz.ehligible.app.resources.Res
import io.github.mirhamedjz.ehligible.app.resources.onb_background_hint_ios
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toNSDateComponents
import org.jetbrains.compose.resources.StringResource
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.Foundation.NSCalendar
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterLongStyle
import platform.Foundation.NSDateFormatterMediumStyle
import platform.Foundation.NSDateFormatterNoStyle
import platform.Foundation.NSDateFormatterShortStyle
import platform.Foundation.NSLocale
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterDecimalStyle
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDefaults
import platform.Foundation.create
import platform.Foundation.currentLocale
import platform.Foundation.localizedStringForCountryCode
import platform.Foundation.stringWithContentsOfURL
import platform.Foundation.writeToURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.UIKit.popoverPresentationController
import platform.UniformTypeIdentifiers.UTTypeJSON
import platform.UniformTypeIdentifiers.UTTypePlainText
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusDenied
import platform.UserNotifications.UNUserNotificationCenter
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

actual fun formatDate(date: LocalDate, style: DateStyle): String {
    val nsDate = NSCalendar.currentCalendar.dateFromComponents(date.toNSDateComponents()) ?: return date.toString()
    val formatter = NSDateFormatter()
    formatter.dateStyle = when (style) {
        DateStyle.SHORT -> NSDateFormatterShortStyle
        DateStyle.MEDIUM -> NSDateFormatterMediumStyle
        DateStyle.LONG -> NSDateFormatterLongStyle
    }
    formatter.timeStyle = NSDateFormatterNoStyle
    return formatter.stringFromDate(nsDate)
}

actual fun formatNumber(value: Int): String {
    val formatter = NSNumberFormatter()
    formatter.numberStyle = NSNumberFormatterDecimalStyle
    return formatter.stringFromNumber(NSNumber(int = value)) ?: value.toString()
}

actual fun countryName(code: String): String = NSLocale.currentLocale.localizedStringForCountryCode(code) ?: code

/** Forwards authorization changes from CoreLocation to Compose. */
private class LocationAuthorization :
    NSObject(),
    CLLocationManagerDelegateProtocol {
    val manager = CLLocationManager()
    var onChange: () -> Unit = {}

    init {
        manager.delegate = this
    }

    val granted: Boolean
        get() = manager.authorizationStatus == kCLAuthorizationStatusAuthorizedWhenInUse ||
            manager.authorizationStatus == kCLAuthorizationStatusAuthorizedAlways

    val grantedAlways: Boolean get() = manager.authorizationStatus == kCLAuthorizationStatusAuthorizedAlways

    val decided: Boolean get() = manager.authorizationStatus != kCLAuthorizationStatusNotDetermined

    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) = onChange()
}

@Composable
actual fun rememberLocationPermission(onResult: (Boolean) -> Unit): PermissionController =
    rememberLocationAuthorization(always = false, onResult)

@Composable
actual fun rememberBackgroundLocationPermission(onResult: (Boolean) -> Unit): PermissionController =
    rememberLocationAuthorization(always = true, onResult)

@Composable
private fun rememberLocationAuthorization(always: Boolean, onResult: (Boolean) -> Unit): PermissionController {
    val authorization = remember { LocationAuthorization() }
    fun granted() = if (always) authorization.grantedAlways else authorization.granted
    val controller = remember { StatePermissionController(granted()) }
    val latestOnResult by rememberUpdatedState(onResult)
    DisposableEffect(authorization) {
        var asked = false
        controller.onRequest = {
            val defaults = NSUserDefaults.standardUserDefaults
            when {
                // iOS asks once; after a refusal (or "Keep Only While Using"), only Settings can change it.
                authorization.manager.authorizationStatus == kCLAuthorizationStatusDenied -> openAppSettings()

                always && defaults.boolForKey(ASKED_ALWAYS_KEY) -> openAppSettings()

                always -> {
                    asked = true
                    defaults.setBool(true, forKey = ASKED_ALWAYS_KEY)
                    authorization.manager.requestAlwaysAuthorization()
                }

                else -> {
                    asked = true
                    authorization.manager.requestWhenInUseAuthorization()
                }
            }
        }
        authorization.onChange = {
            controller.state = granted()
            if (asked && authorization.decided) {
                asked = false
                latestOnResult(granted())
            }
        }
        onDispose { authorization.onChange = {} }
    }
    return controller
}

actual val backgroundPermissionHint: StringResource = Res.string.onb_background_hint_ios

private const val ASKED_ALWAYS_KEY = "askedForAlwaysLocation"

private fun openAppSettings() {
    val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
    UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any>(), completionHandler = null)
}

@Composable
actual fun rememberNotificationPermission(onResult: (Boolean) -> Unit): PermissionController {
    val center = remember { UNUserNotificationCenter.currentNotificationCenter() }
    val controller = remember { StatePermissionController(false) }
    var denied by remember { mutableStateOf(false) }
    val latestOnResult by rememberUpdatedState(onResult)
    LaunchedEffect(center) {
        center.getNotificationSettingsWithCompletionHandler { settings ->
            val status = settings?.authorizationStatus
            dispatch_async(dispatch_get_main_queue()) {
                controller.state = status == UNAuthorizationStatusAuthorized
                denied = status == UNAuthorizationStatusDenied
            }
        }
    }
    controller.onRequest = request@{
        // iOS asks once; after a refusal only Settings can turn notifications on.
        if (denied) return@request openAppSettings()
        val options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
        center.requestAuthorizationWithOptions(options) { granted, _ ->
            dispatch_async(dispatch_get_main_queue()) {
                controller.state = granted
                latestOnResult(granted)
            }
        }
    }
    return controller
}

@Suppress("DEPRECATION") // keyWindow is the simplest way to find the presenter in a single-window app.
private fun topViewController(): UIViewController? {
    var top = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (top?.presentedViewController != null) top = top.presentedViewController
    return top
}

private fun presentShareSheet(items: List<Any>, onDone: (Boolean) -> Unit = {}) {
    val presenter = topViewController() ?: return onDone(false)
    val sheet = UIActivityViewController(activityItems = items, applicationActivities = null)
    sheet.popoverPresentationController?.sourceView = presenter.view
    sheet.completionWithItemsHandler = { _, completed, _, _ -> onDone(completed) }
    presenter.presentViewController(sheet, animated = true, completion = null)
}

@Composable
actual fun rememberShareText(): (subject: String, text: String) -> Unit = remember {
    { _, text -> presentShareSheet(listOf(text)) }
}

@OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
@Composable
actual fun rememberSaveDocument(
    mimeType: String,
    onSaved: (Boolean) -> Unit,
): (fileName: String, content: String) -> Unit {
    val latestOnSaved by rememberUpdatedState(onSaved)
    return remember {
        { fileName, content ->
            // iOS has no "save as" picker for app data: write a temporary file and let the share sheet save it.
            val url = NSURL.fileURLWithPath(NSTemporaryDirectory() + fileName)
            val written = NSString.create(
                string = content,
            ).writeToURL(url, atomically = true, encoding = NSUTF8StringEncoding, error = null)
            if (written) presentShareSheet(listOf(url)) { latestOnSaved(it) } else latestOnSaved(false)
        }
    }
}

private class DocumentPickerDelegate(private val onPicked: (String?) -> Unit) :
    NSObject(),
    UIDocumentPickerDelegateProtocol {
    @OptIn(ExperimentalForeignApi::class)
    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
        val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
        onPicked(url?.let { NSString.stringWithContentsOfURL(it, NSUTF8StringEncoding, null) })
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) = onPicked(null)
}

@Composable
actual fun rememberOpenDocument(onOpened: (String?) -> Unit): () -> Unit {
    val latestOnOpened by rememberUpdatedState(onOpened)
    // UIKit holds delegates weakly, so keep a strong reference for as long as this composable lives.
    val delegate = remember { DocumentPickerDelegate { latestOnOpened(it) } }
    return remember(delegate) {
        {
            val picker =
                UIDocumentPickerViewController(
                    forOpeningContentTypes = listOf(UTTypeJSON, UTTypePlainText),
                    asCopy = true,
                )
            picker.delegate = delegate
            topViewController()?.presentViewController(picker, animated = true, completion = null)
        }
    }
}

/** iOS has no API for an app to add its own widget; people add it from the home screen. */
@Composable
actual fun rememberWidgetPinner(): (() -> Unit)? = null
