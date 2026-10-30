package io.github.mirhamedjz.ehligible.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.mirhamedjz.ehligible.app.AppGraph
import io.github.mirhamedjz.ehligible.core.model.AppData
import io.github.mirhamedjz.ehligible.core.model.AppDataJson
import io.github.mirhamedjz.ehligible.core.model.CountdownTarget
import io.github.mirhamedjz.ehligible.core.model.Dashboard
import io.github.mirhamedjz.ehligible.core.model.InvalidBackupException
import io.github.mirhamedjz.ehligible.core.model.Milestone
import io.github.mirhamedjz.ehligible.core.model.Profile
import io.github.mirhamedjz.ehligible.core.model.Settings
import io.github.mirhamedjz.ehligible.core.model.dashboard
import io.github.mirhamedjz.ehligible.core.model.nextCelebration
import io.github.mirhamedjz.ehligible.core.presence.TimelineTrip
import io.github.mirhamedjz.ehligible.core.presence.Trip
import io.github.mirhamedjz.ehligible.core.presence.TripNote
import io.github.mirhamedjz.ehligible.core.presence.UserVerdict
import io.github.mirhamedjz.ehligible.core.rules.DateRange
import io.github.mirhamedjz.ehligible.core.rules.PrePrStatus
import kotlin.uuid.Uuid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

fun newTripId(): String = Uuid.random().toString()

sealed interface AppUiState {
    data object Loading : AppUiState

    data class Onboarding(val data: AppData) : AppUiState

    data class Ready(val data: AppData, val dashboard: Dashboard) : AppUiState
}

/** What the onboarding flow collected. */
data class OnboardingAnswers(
    val prDate: LocalDate,
    val firstStatusDate: LocalDate?,
    val statusGaps: List<DateRange>,
    val trips: List<Trip>,
    val notifications: Boolean,
)

/** Answers in progress; lives in the ViewModel so it survives configuration changes. */
class OnboardingDraft {
    var step by mutableIntStateOf(0)
    var prDate by mutableStateOf<LocalDate?>(null)
    var hadStatus by mutableStateOf<Boolean?>(null)
    var firstStatusDate by mutableStateOf<LocalDate?>(null)
    val statusGaps = mutableStateListOf<DateRange>()
    val trips = mutableStateListOf<Trip>()
    var noTrips by mutableStateOf(false)
    var notifications by mutableStateOf(false)

    fun answers(): OnboardingAnswers? {
        val pr = prDate ?: return null
        val hasStatus = hadStatus == true
        return OnboardingAnswers(
            prDate = pr,
            firstStatusDate = if (hasStatus) firstStatusDate else null,
            statusGaps = if (hasStatus) statusGaps.toList() else emptyList(),
            trips = if (noTrips) emptyList() else trips.toList(),
            notifications = notifications,
        )
    }
}

class AppViewModel(private val graph: AppGraph) : ViewModel() {
    private val today = MutableStateFlow(currentDate())
    private val celebrationState = MutableStateFlow<Milestone?>(null)

    /** A milestone just reached while the app was open; the UI throws confetti, then calls [dismissCelebration]. */
    val celebration: StateFlow<Milestone?> = celebrationState.asStateFlow()

    val state: StateFlow<AppUiState> = combine(graph.repository.data, today) { data, day ->
        val dashboard = data.dashboard(day, graph.clock.now())
        if (dashboard == null) AppUiState.Onboarding(data) else AppUiState.Ready(data, dashboard)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, AppUiState.Loading)

    val appInfo get() = graph.appInfo

    val onboarding = OnboardingDraft()

    /** The countdown the user would get with the onboarding answers so far (for the final step). */
    fun previewDashboard(answers: OnboardingAnswers): Dashboard? = AppData(
        profile = Profile(
            answers.prDate,
            PrePrStatus(answers.firstStatusDate, answers.statusGaps),
            trackingStart = currentDate(),
        ),
        trips = answers.trips,
    ).dashboard(currentDate(), graph.clock.now())

    fun currentDate(): LocalDate = graph.clock.now().toLocalDateTime(graph.timeZone()).date

    /** Called whenever the app comes to the foreground: refresh "today" and run a presence check. */
    fun onForeground() {
        today.value = currentDate()
        viewModelScope.launch {
            runCatching { graph.presenceChecker.checkNow() }
            afterChange()
        }
    }

    fun refreshToday() {
        today.value = currentDate()
    }

    fun completeOnboarding(answers: OnboardingAnswers) = mutate { data ->
        data.copy(
            profile = Profile(
                prDate = answers.prDate,
                prePrStatus = PrePrStatus(answers.firstStatusDate, answers.statusGaps),
                trackingStart = currentDate(),
            ),
            trips = answers.trips,
            settings = data.settings.copy(notificationsEnabled = answers.notifications),
        )
    }

    /** Adds a trip, or updates the one with [id]. */
    fun saveTrip(id: String?, departure: LocalDate, returnDate: LocalDate, destination: String, reason: String) =
        mutate { it.upsertTrip(Trip(id ?: newTripId(), departure, returnDate, destination, reason)) }

    fun deleteTrip(id: String) = mutate { it.deleteTrip(id) }

    fun annotateDetectedTrip(departure: LocalDate, note: TripNote) = mutate { it.annotateDetectedTrip(departure, note) }

    /**
     * Replaces a detected trip with an entered one with corrected dates. Days the detection counted as away
     * that fall outside the corrected trip are confirmed as days in Canada.
     */
    fun correctDetectedTrip(
        original: TimelineTrip,
        departure: LocalDate,
        returnDate: LocalDate,
        destination: String,
        reason: String,
    ) = mutate { data ->
        val corrected = Trip(newTripId(), departure, returnDate, destination, reason)
        var result = data.upsertTrip(corrected)
        val end = original.returnDate ?: currentDate().plus(1, DateTimeUnit.DAY)
        var day = original.departure.plus(1, DateTimeUnit.DAY)
        while (day < end) {
            if (!corrected.covers(day)) result = result.confirmDay(day, UserVerdict.PRESENT)
            day = day.plus(1, DateTimeUnit.DAY)
        }
        result
    }

    fun confirmDays(dates: List<LocalDate>, inCanada: Boolean) = mutate { data ->
        val verdict = if (inCanada) UserVerdict.PRESENT else UserVerdict.ABSENT
        dates.fold(data) { acc, date -> acc.confirmDay(date, verdict) }
    }

    fun setMargin(days: Int) = updateSettings { it.copy(marginDays = days.coerceIn(0, Settings.MAX_MARGIN_DAYS)) }

    fun setCountdownTarget(target: CountdownTarget) = updateSettings { it.copy(countdownTarget = target) }

    fun setNotifications(enabled: Boolean) = updateSettings { it.copy(notificationsEnabled = enabled) }

    fun toggleChecklistItem(id: String) = updateSettings {
        it.copy(checklist = if (id in it.checklist) it.checklist - id else it.checklist + id)
    }

    fun setBirthYear(year: Int?) = mutate { data -> data.copy(profile = data.profile?.copy(birthYear = year)) }

    fun setPrDate(date: LocalDate) = mutate { data -> data.copy(profile = data.profile?.copy(prDate = date)) }

    fun setPrePrStatus(status: PrePrStatus) = mutate { data ->
        data.copy(profile = data.profile?.copy(prePrStatus = status))
    }

    suspend fun exportBackup(): String = AppDataJson.encode(graph.repository.current())

    /** Replaces all data with a backup. Returns false if the text isn't a valid backup. */
    suspend fun importBackup(text: String): Boolean {
        val restored = try {
            AppDataJson.decode(text)
        } catch (_: InvalidBackupException) {
            return false
        }
        graph.repository.update { restored }
        afterChange()
        return true
    }

    fun dismissCelebration() {
        celebrationState.value = null
    }

    private fun updateSettings(transform: (Settings) -> Settings) = mutate {
        it.copy(settings = transform(it.settings))
    }

    private fun mutate(transform: (AppData) -> AppData) {
        viewModelScope.launch {
            graph.repository.update(transform)
            afterChange()
        }
    }

    /** After any change: refresh widgets and celebrate a newly reached milestone. */
    private suspend fun afterChange() {
        runCatching { graph.widgets.refresh() }
        val data = graph.repository.current()
        val dashboard = data.dashboard(currentDate(), graph.clock.now()) ?: return
        val (milestone, updated) = data.nextCelebration(dashboard)
        if (milestone != null) {
            graph.repository.update { it.copy(celebrated = updated.celebrated) }
            celebrationState.value = milestone
        }
    }
}
