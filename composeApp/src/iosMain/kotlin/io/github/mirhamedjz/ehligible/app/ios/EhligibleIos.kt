package io.github.mirhamedjz.ehligible.app.ios

import androidx.compose.ui.window.ComposeUIViewController
import io.github.mirhamedjz.ehligible.app.App
import io.github.mirhamedjz.ehligible.app.AppGraph
import io.github.mirhamedjz.ehligible.app.AppInfo
import io.github.mirhamedjz.ehligible.app.BackgroundSync
import io.github.mirhamedjz.ehligible.app.data.AppRepository
import io.github.mirhamedjz.ehligible.app.resources.Res
import io.github.mirhamedjz.ehligible.core.geo.BorderIndex
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSBundle
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask
import platform.UIKit.UIViewController

/** What only Swift can do for the Kotlin side (WidgetKit has no Objective-C API). Implemented in `iosApp`. */
interface IosHost {
    fun reloadWidgets()
}

/**
 * The iOS app's entry point, called from Swift as `EhligibleIos.shared`. [start] runs at every launch, including
 * when iOS relaunches the app in the background for a location event; [mainViewController] hosts the shared UI.
 */
object EhligibleIos {
    private val scope = MainScope()
    private var host: IosHost? = null

    private val bordersLock = Mutex()
    private var bordersCache: BorderIndex? = null

    private suspend fun borders(): BorderIndex = bordersLock.withLock {
        bordersCache ?: withContext(Dispatchers.Default) {
            BorderIndex.parse(Res.readBytes("files/borders.bin"))
        }.also { bordersCache = it }
    }

    private val repository: AppRepository by lazy { AppRepository.create(dataFile(), scope) }

    private val location: IosLocation by lazy {
        IosLocation(repository, ::borders, onBackgroundCheck = { before -> backgroundSync.afterCheck(before) })
    }

    val graph: AppGraph by lazy {
        AppGraph(
            repository = repository,
            presenceChecker = {
                // Coming back to the app is when a just-granted "Always" permission takes effect.
                location.syncMonitoring()
                location.checkNow()
            },
            widgets = {
                IosWidgets.write(repository.current(), graph.clock.now(), graph.timeZone())
                host?.reloadWidgets()
            },
            notifier = IosNotifier,
            appInfo = AppInfo(versionName = versionName(), backgroundLocationSupported = true),
        )
    }

    private val backgroundSync: BackgroundSync by lazy { BackgroundSync(graph) }

    fun start(host: IosHost) {
        this.host = host
        location.syncMonitoring()
    }

    fun mainViewController(): UIViewController = ComposeUIViewController { App(graph) }

    @OptIn(ExperimentalForeignApi::class)
    private fun dataFile(): Path {
        val directory = NSFileManager.defaultManager.URLForDirectory(
            directory = NSApplicationSupportDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null,
        )
        val path = checkNotNull(directory?.path) { "No Application Support directory" }
        return "$path/${AppRepository.FILE_NAME}".toPath()
    }

    private fun versionName(): String =
        NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: "1.0.0"
}
