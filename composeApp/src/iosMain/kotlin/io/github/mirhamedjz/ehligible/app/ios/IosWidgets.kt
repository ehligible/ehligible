package io.github.mirhamedjz.ehligible.app.ios

import io.github.mirhamedjz.ehligible.core.model.AppData
import io.github.mirhamedjz.ehligible.core.model.widgetSnapshot
import kotlin.time.Instant
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.writeToURL

/** Writes the [widgetSnapshot] where the widget extension can read it: the shared App Group container. */
internal object IosWidgets {
    /** Must match `iosApp/project.yml` and `iosApp/EhligibleWidget/WidgetSnapshot.swift`. */
    private const val APP_GROUP = "group.ca.ehligible"
    private const val FILE_NAME = "widget.json"

    @OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
    fun write(data: AppData, now: Instant, zone: TimeZone) {
        val files = NSFileManager.defaultManager
        // Unsigned builds (CI simulators) have no App Group container; there's no widget to feed then.
        val container = files.containerURLForSecurityApplicationGroupIdentifier(APP_GROUP) ?: return
        val url = container.URLByAppendingPathComponent(FILE_NAME) ?: return
        val snapshot = data.widgetSnapshot(now.toLocalDateTime(zone).date, now, zone)
        if (snapshot == null) {
            files.removeItemAtURL(url, error = null)
        } else {
            NSString.create(string = snapshot.encode())
                .writeToURL(url, atomically = true, encoding = NSUTF8StringEncoding, error = null)
        }
    }
}
