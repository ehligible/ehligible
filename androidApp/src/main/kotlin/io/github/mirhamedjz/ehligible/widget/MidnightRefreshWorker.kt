package io.github.mirhamedjz.ehligible.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit
import kotlin.time.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/** Refreshes the widget just after midnight so the number of days ticks down, then schedules the next night. */
class MidnightRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        CountdownWidget.refreshAll(applicationContext)
        schedule(applicationContext)
        return Result.success()
    }

    companion object {
        private const val NAME = "widget-midnight"

        fun schedule(context: Context) {
            val zone = TimeZone.currentSystemDefault()
            val now = Clock.System.now()
            val nextMidnight = now.toLocalDateTime(zone).date.plus(1, DateTimeUnit.DAY).atStartOfDayIn(zone)
            val delayMillis = (nextMidnight - now).inWholeMilliseconds + 60_000
            val request = OneTimeWorkRequestBuilder<MidnightRefreshWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
