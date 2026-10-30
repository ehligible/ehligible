package io.github.mirhamedjz.ehligible

import android.app.Application
import io.github.mirhamedjz.ehligible.app.AppGraph
import io.github.mirhamedjz.ehligible.app.AppInfo
import io.github.mirhamedjz.ehligible.app.BackgroundSync
import io.github.mirhamedjz.ehligible.app.data.AppRepository
import io.github.mirhamedjz.ehligible.app.resources.Res
import io.github.mirhamedjz.ehligible.core.geo.BorderIndex
import io.github.mirhamedjz.ehligible.detection.PresenceCheckRunner
import io.github.mirhamedjz.ehligible.detection.PresenceWorker
import io.github.mirhamedjz.ehligible.notifications.AndroidNotifier
import io.github.mirhamedjz.ehligible.widget.CountdownWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okio.Path.Companion.toOkioPath

class EhligibleApp : Application() {
    /** Lives as long as the process; used for DataStore and one-off background work. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val bordersLock = Mutex()
    private var bordersCache: BorderIndex? = null

    private suspend fun borders(): BorderIndex = bordersLock.withLock {
        bordersCache ?: BorderIndex.parse(Res.readBytes("files/borders.bin")).also { bordersCache = it }
    }

    private val notifier by lazy { AndroidNotifier(this) }

    val graph: AppGraph by lazy {
        val repository = AppRepository.create(filesDir.resolve(AppRepository.FILE_NAME).toOkioPath(), appScope)
        AppGraph(
            repository = repository,
            presenceChecker = { checkRunner.run(foreground = true) },
            widgets = { CountdownWidget.refreshAll(this) },
            notifier = notifier,
            appInfo = AppInfo(
                versionName = BuildConfig.VERSION_NAME,
                backgroundLocationSupported = BuildConfig.BACKGROUND_LOCATION,
            ),
        )
    }

    val checkRunner: PresenceCheckRunner by lazy { PresenceCheckRunner(this, graph.repository, ::borders) }

    val backgroundSync: BackgroundSync by lazy { BackgroundSync(graph) }

    override fun onCreate() {
        super.onCreate()
        PresenceWorker.schedule(this)
        CountdownWidget.scheduleMidnightRefresh(this)
        appScope.launch { notifier.createChannels() }
    }
}
