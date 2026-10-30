package io.github.mirhamedjz.ehligible.core.model

import kotlin.math.roundToInt
import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * What the iOS widget shows. A widget extension can't run the rules, so the app precomputes the next [DAYS] days,
 * each as if it were today (the home screen's projection: staying in Canada apart from planned trips), and the
 * widget shows the entry for the current date. The app writes it as JSON to the shared App Group container;
 * `iosApp/EhligibleWidget/WidgetSnapshot.swift` reads it.
 */
@Serializable
data class WidgetSnapshot(
    /** When the app wrote it, in epoch milliseconds. */
    val generatedAt: Long,
    /** Days waiting for the user to say where they were. */
    val reviewDays: Int = 0,
    val target: CountdownTarget,
    val days: List<WidgetDay>,
) {
    fun encode(): String = json.encodeToString(serializer(), this)

    companion object {
        /** Two weeks, so the widget stays right even when the app isn't opened for a while. */
        const val DAYS: Int = 14

        private val json = Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
        }

        fun decode(text: String): WidgetSnapshot = json.decodeFromString(serializer(), text)
    }
}

/** Where the person is, for the widget's status line. */
@Serializable
enum class WidgetStatus { IN_CANADA, ABROAD, NEAR_BORDER, UNKNOWN }

/** The widget's numbers for one day. */
@Serializable
data class WidgetDay(
    val date: LocalDate,
    /** Days until [countdownDate]; null when it isn't within reach. */
    val daysLeft: Int?,
    /** The first eligible date or the safer date, depending on [WidgetSnapshot.target]. */
    val countdownDate: LocalDate?,
    val creditedDays: Int,
    /** Share of the 1,095 days earned, from 0 to 1. */
    val progress: Double,
    /** Show the "you can apply" celebration instead of a countdown. */
    val celebrate: Boolean,
    val status: WidgetStatus,
    /** ISO 3166-1 alpha-2 code of the country when abroad, if known. */
    val country: String? = null,
)

/** Builds the [WidgetSnapshot] starting on [today], or null before onboarding is done. */
fun AppData.widgetSnapshot(
    today: LocalDate,
    now: Instant,
    zone: TimeZone,
    days: Int = WidgetSnapshot.DAYS,
): WidgetSnapshot? {
    val current = dashboard(today, now) ?: return null
    return WidgetSnapshot(
        generatedAt = now.toEpochMilliseconds(),
        reviewDays = current.reviewDays.size,
        target = current.target,
        days = (0 until days).mapNotNull { offset ->
            if (offset == 0) return@mapNotNull current.toWidgetDay()
            // A later day is shown from its midnight on, so the status is judged from then: a check older than
            // 36 hours turns into "not checked recently", and a planned trip shows as being abroad.
            val date = today.plus(offset, DateTimeUnit.DAY)
            dashboard(date, date.atStartOfDayIn(zone))?.toWidgetDay()
        },
    )
}

private fun Dashboard.toWidgetDay(): WidgetDay {
    val status = currentStatus
    return WidgetDay(
        date = today,
        daysLeft = daysLeft,
        countdownDate = countdownDate,
        creditedDays = creditedDays,
        progress = (progress * 10_000).roundToInt() / 10_000.0,
        celebrate = showsCelebration,
        status = when (status) {
            CurrentStatus.InCanada -> WidgetStatus.IN_CANADA
            is CurrentStatus.Abroad -> WidgetStatus.ABROAD
            CurrentStatus.NearBorder -> WidgetStatus.NEAR_BORDER
            CurrentStatus.Unknown -> WidgetStatus.UNKNOWN
        },
        country = (status as? CurrentStatus.Abroad)?.country,
    )
}
