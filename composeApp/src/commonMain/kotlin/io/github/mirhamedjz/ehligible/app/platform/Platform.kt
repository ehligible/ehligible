package io.github.mirhamedjz.ehligible.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.StringResource

enum class DateStyle { SHORT, MEDIUM, LONG }

/** A date in the user's language, e.g. "Jan 1, 2028" / "1 janv. 2028". */
expect fun formatDate(date: LocalDate, style: DateStyle = DateStyle.MEDIUM): String

/** An integer with the locale's grouping, e.g. "1,095" / "1 095". */
expect fun formatNumber(value: Int): String

/** The country's name in the user's language, or the code itself if unknown. */
expect fun countryName(code: String): String

/** The flag emoji for an ISO 3166-1 alpha-2 code ("FR" → 🇫🇷). */
fun flagEmoji(code: String): String {
    if (code.length != 2 || !code.all { it in 'A'..'Z' || it in 'a'..'z' }) return ""
    return buildString {
        for (letter in code.uppercase()) {
            val codePoint = 0x1F1E6 + (letter - 'A')
            val offset = codePoint - 0x10000
            append(Char(0xD800 + (offset shr 10)))
            append(Char(0xDC00 + (offset and 0x3FF)))
        }
    }
}

/** A runtime permission the UI can check and ask for. */
interface PermissionController {
    val granted: Boolean

    fun request()
}

/** A [PermissionController] backed by Compose state, so screens recompose when the answer changes. */
internal class StatePermissionController(initial: Boolean) : PermissionController {
    var state by mutableStateOf(initial)
    var onRequest: () -> Unit = {}

    override val granted: Boolean get() = state

    override fun request() = onRequest()
}

/** Approximate ("coarse") location while the app is in use. Never precise location. */
@Composable
expect fun rememberLocationPermission(onResult: (Boolean) -> Unit = {}): PermissionController

/**
 * Approximate location while the app is closed ("Allow all the time" on Android, "Always" on iOS).
 * Only builds with background location (v1.1+, docs/SPEC.md) declare and ask for it.
 */
@Composable
expect fun rememberBackgroundLocationPermission(onResult: (Boolean) -> Unit = {}): PermissionController

/** How to grant background location on this platform; ends the background-location disclosure. */
expect val backgroundPermissionHint: StringResource

@Composable
expect fun rememberNotificationPermission(onResult: (Boolean) -> Unit = {}): PermissionController

/** Opens the system share sheet with plain text. */
@Composable
expect fun rememberShareText(): (subject: String, text: String) -> Unit

/** Lets the user save [content] as a file of [mimeType]; [onSaved] reports success. */
@Composable
expect fun rememberSaveDocument(
    mimeType: String,
    onSaved: (Boolean) -> Unit,
): (fileName: String, content: String) -> Unit

/** Lets the user pick a text file; [onOpened] receives its contents, or null if cancelled/unreadable. */
@Composable
expect fun rememberOpenDocument(onOpened: (String?) -> Unit): () -> Unit

/** Asks the launcher to pin the countdown widget, or null if this launcher can't. */
@Composable
expect fun rememberWidgetPinner(): (() -> Unit)?
