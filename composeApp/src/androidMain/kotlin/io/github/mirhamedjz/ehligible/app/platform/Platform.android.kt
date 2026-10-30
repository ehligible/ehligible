package io.github.mirhamedjz.ehligible.app.platform

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.compose.LifecycleResumeEffect
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.IllformedLocaleException
import java.util.Locale
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate

actual fun formatDate(date: LocalDate, style: DateStyle): String {
    val formatStyle = when (style) {
        DateStyle.SHORT -> FormatStyle.SHORT
        DateStyle.MEDIUM -> FormatStyle.MEDIUM
        DateStyle.LONG -> FormatStyle.LONG
    }
    return DateTimeFormatter.ofLocalizedDate(formatStyle).withLocale(Locale.getDefault()).format(date.toJavaLocalDate())
}

actual fun formatNumber(value: Int): String = NumberFormat.getIntegerInstance().format(value)

actual fun countryName(code: String): String = try {
    Locale.Builder().setRegion(code).build().displayCountry.ifBlank { code }
} catch (_: IllformedLocaleException) {
    code
}

private fun Context.has(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

private fun Context.askedBefore(permission: String): Boolean =
    getSharedPreferences(PERMISSION_PREFS, Context.MODE_PRIVATE).getBoolean(permission, false)

private fun Context.rememberAsked(permission: String) {
    getSharedPreferences(PERMISSION_PREFS, Context.MODE_PRIVATE).edit { putBoolean(permission, true) }
}

private fun Context.openAppSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
    startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

private const val PERMISSION_PREFS = "permission_requests"

@Composable
private fun rememberPermission(permission: String?, onResult: (Boolean) -> Unit): PermissionController {
    val context = LocalContext.current
    val activity = LocalActivity.current
    fun current() = permission == null || context.has(permission)
    val controller = remember { StatePermissionController(current()) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        controller.state = ok || current()
        onResult(ok)
    }
    controller.onRequest = request@{
        if (permission == null) return@request onResult(true)
        // After repeated denials Android stops showing its prompt, so the button would do nothing.
        // The app's system settings page is the only way left to grant it.
        val blocked = context.askedBefore(permission) &&
            activity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        if (blocked) {
            context.openAppSettings()
        } else {
            context.rememberAsked(permission)
            launcher.launch(permission)
        }
    }
    LifecycleResumeEffect(controller) {
        controller.state = current()
        onPauseOrDispose { }
    }
    return controller
}

@Composable
actual fun rememberLocationPermission(onResult: (Boolean) -> Unit): PermissionController =
    rememberPermission(Manifest.permission.ACCESS_COARSE_LOCATION, onResult)

@Composable
actual fun rememberBackgroundLocationPermission(onResult: (Boolean) -> Unit): PermissionController = rememberPermission(
    // Before Android 10, approximate location already applies in the background.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        Manifest.permission.ACCESS_BACKGROUND_LOCATION
    } else {
        Manifest.permission.ACCESS_COARSE_LOCATION
    },
    onResult,
)

@Composable
actual fun rememberNotificationPermission(onResult: (Boolean) -> Unit): PermissionController = rememberPermission(
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.POST_NOTIFICATIONS else null,
    onResult,
)

@Composable
actual fun rememberShareText(): (subject: String, text: String) -> Unit {
    val context = LocalContext.current
    return remember(context) {
        { subject, text ->
            val send = Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_SUBJECT, subject)
                .putExtra(Intent.EXTRA_TEXT, text)
            context.startActivity(Intent.createChooser(send, subject).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}

@Composable
actual fun rememberSaveDocument(
    mimeType: String,
    onSaved: (Boolean) -> Unit,
): (fileName: String, content: String) -> Unit {
    val context = LocalContext.current
    val pending = remember { arrayOfNulls<String>(1) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(mimeType)) { uri ->
        val content = pending[0]
        pending[0] = null
        if (uri == null || content == null) {
            onSaved(false)
            return@rememberLauncherForActivityResult
        }
        val ok = runCatching {
            context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(content.toByteArray(Charsets.UTF_8)) }
        }.isSuccess
        onSaved(ok)
    }
    return remember(launcher) {
        { fileName, content ->
            pending[0] = content
            launcher.launch(fileName)
        }
    }
}

@Composable
actual fun rememberOpenDocument(onOpened: (String?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val text = uri?.let {
            runCatching {
                context.contentResolver.openInputStream(it)!!.use { stream -> stream.readBytes().decodeToString() }
            }.getOrNull()
        }
        onOpened(text)
    }
    return remember(launcher) {
        { launcher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }
    }
}

/** The widget receiver lives in the app module, so it's referenced by name. */
private const val WIDGET_RECEIVER = "io.github.mirhamedjz.ehligible.widget.CountdownWidgetReceiver"

@Composable
actual fun rememberWidgetPinner(): (() -> Unit)? {
    val context = LocalContext.current
    val manager = context.getSystemService(AppWidgetManager::class.java)
    if (manager == null || !manager.isRequestPinAppWidgetSupported) return null
    return remember(context) {
        { manager.requestPinAppWidget(ComponentName(context.packageName, WIDGET_RECEIVER), null, null) }
    }
}
