package io.github.mirhamedjz.ehligible.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.mirhamedjz.ehligible.app.ui.AppUiState
import io.github.mirhamedjz.ehligible.app.ui.AppViewModel
import io.github.mirhamedjz.ehligible.app.ui.MainNavigation
import io.github.mirhamedjz.ehligible.app.ui.components.ConfettiBurst
import io.github.mirhamedjz.ehligible.app.ui.milestoneText
import io.github.mirhamedjz.ehligible.app.ui.onboarding.OnboardingFlow
import io.github.mirhamedjz.ehligible.app.ui.theme.EhligibleTheme
import kotlin.time.Clock
import kotlinx.coroutines.delay
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

@Composable
fun App(graph: AppGraph) {
    EhligibleTheme {
        val viewModel = viewModel { AppViewModel(graph) }
        val state by viewModel.state.collectAsStateWithLifecycle()
        val celebration by viewModel.celebration.collectAsStateWithLifecycle()

        LifecycleResumeEffect(viewModel) {
            viewModel.onForeground()
            onPauseOrDispose { }
        }
        MidnightTicker(onNewDay = viewModel::refreshToday)

        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize()) {
                when (val current = state) {
                    AppUiState.Loading -> Unit
                    is AppUiState.Onboarding -> OnboardingFlow(viewModel)
                    is AppUiState.Ready -> MainNavigation(viewModel, current)
                }
                val milestone = celebration
                if (milestone != null) {
                    ConfettiBurst(key = milestone, onFinished = viewModel::dismissCelebration)
                }
                AnimatedVisibility(
                    visible = milestone != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 72.dp),
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shadowElevation = 6.dp,
                    ) {
                        Text(
                            text = milestone?.let { milestoneText(it) }.orEmpty(),
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Calls [onNewDay] just after each local midnight while the app is on screen. */
@Composable
private fun MidnightTicker(onNewDay: () -> Unit) {
    LaunchedEffect(Unit) {
        while (true) {
            val zone = TimeZone.currentSystemDefault()
            val now = Clock.System.now()
            val tomorrow = now.toLocalDateTime(zone).date.plus(1, DateTimeUnit.DAY)
            delay((tomorrow.atStartOfDayIn(zone) - now).inWholeMilliseconds + 1_000)
            onNewDay()
        }
    }
}
