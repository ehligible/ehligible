package io.github.mirhamedjz.ehligible.detection

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.mirhamedjz.ehligible.EhligibleApp
import java.util.concurrent.TimeUnit

/** Checks presence every few hours (and on demand) while the app isn't on screen. */
class PresenceWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as EhligibleApp
        if (app.graph.repository.current().profile == null) return Result.success()
        val before = app.graph.repository.current()
        runCatching { app.checkRunner.run(foreground = false) }
        app.backgroundSync.afterCheck(before)
        return Result.success()
    }

    companion object {
        private const val PERIODIC = "presence-periodic"
        private const val NOW = "presence-now"

        /** Every ~4 hours; WorkManager keeps the schedule across reboots and app updates. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<PresenceWorker>(4, TimeUnit.HOURS, 1, TimeUnit.HOURS).build()
            WorkManager.getInstance(
                context,
            ).enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.UPDATE, request)
        }

        /** A one-off check soon, e.g. after a time-zone change. */
        fun checkSoon(context: Context) {
            val request = OneTimeWorkRequestBuilder<PresenceWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(NOW, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
