package io.github.mirhamedjz.ehligible.app.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import io.github.mirhamedjz.ehligible.app.platform.formatDate
import io.github.mirhamedjz.ehligible.app.platform.formatNumber
import io.github.mirhamedjz.ehligible.app.platform.rememberLocationPermission
import io.github.mirhamedjz.ehligible.app.platform.rememberNotificationPermission
import io.github.mirhamedjz.ehligible.app.platform.rememberWidgetPinner
import io.github.mirhamedjz.ehligible.app.resources.Res
import io.github.mirhamedjz.ehligible.app.resources.action_add_widget
import io.github.mirhamedjz.ehligible.app.resources.action_back
import io.github.mirhamedjz.ehligible.app.resources.action_cancel
import io.github.mirhamedjz.ehligible.app.resources.action_continue
import io.github.mirhamedjz.ehligible.app.resources.action_not_now
import io.github.mirhamedjz.ehligible.app.resources.action_save
import io.github.mirhamedjz.ehligible.app.resources.disclaimer_short
import io.github.mirhamedjz.ehligible.app.resources.home_days_to_go
import io.github.mirhamedjz.ehligible.app.resources.home_eligible_title
import io.github.mirhamedjz.ehligible.app.resources.journal_add
import io.github.mirhamedjz.ehligible.app.resources.leaf_description
import io.github.mirhamedjz.ehligible.app.resources.onb_done_button
import io.github.mirhamedjz.ehligible.app.resources.onb_done_title
import io.github.mirhamedjz.ehligible.app.resources.onb_done_widget
import io.github.mirhamedjz.ehligible.app.resources.onb_location_allow
import io.github.mirhamedjz.ehligible.app.resources.onb_location_body
import io.github.mirhamedjz.ehligible.app.resources.onb_location_decline
import io.github.mirhamedjz.ehligible.app.resources.onb_location_on
import io.github.mirhamedjz.ehligible.app.resources.onb_location_title
import io.github.mirhamedjz.ehligible.app.resources.onb_notifications_allow
import io.github.mirhamedjz.ehligible.app.resources.onb_notifications_body
import io.github.mirhamedjz.ehligible.app.resources.onb_notifications_title
import io.github.mirhamedjz.ehligible.app.resources.onb_pr_body
import io.github.mirhamedjz.ehligible.app.resources.onb_pr_title
import io.github.mirhamedjz.ehligible.app.resources.onb_status_add_gap
import io.github.mirhamedjz.ehligible.app.resources.onb_status_body
import io.github.mirhamedjz.ehligible.app.resources.onb_status_gaps_body
import io.github.mirhamedjz.ehligible.app.resources.onb_status_gaps_title
import io.github.mirhamedjz.ehligible.app.resources.onb_status_no
import io.github.mirhamedjz.ehligible.app.resources.onb_status_since
import io.github.mirhamedjz.ehligible.app.resources.onb_status_since_hint
import io.github.mirhamedjz.ehligible.app.resources.onb_status_title
import io.github.mirhamedjz.ehligible.app.resources.onb_status_yes
import io.github.mirhamedjz.ehligible.app.resources.onb_step
import io.github.mirhamedjz.ehligible.app.resources.onb_trips_body
import io.github.mirhamedjz.ehligible.app.resources.onb_trips_none
import io.github.mirhamedjz.ehligible.app.resources.onb_trips_title
import io.github.mirhamedjz.ehligible.app.resources.onb_welcome_body
import io.github.mirhamedjz.ehligible.app.resources.onb_welcome_start
import io.github.mirhamedjz.ehligible.app.resources.onb_welcome_title
import io.github.mirhamedjz.ehligible.app.resources.range_from
import io.github.mirhamedjz.ehligible.app.resources.range_to
import io.github.mirhamedjz.ehligible.app.resources.settings_pr_date
import io.github.mirhamedjz.ehligible.app.resources.trip_destination
import io.github.mirhamedjz.ehligible.app.resources.trip_left
import io.github.mirhamedjz.ehligible.app.resources.trip_reason
import io.github.mirhamedjz.ehligible.app.resources.trip_reason_hint
import io.github.mirhamedjz.ehligible.app.resources.trip_returned
import io.github.mirhamedjz.ehligible.app.ui.AppViewModel
import io.github.mirhamedjz.ehligible.app.ui.DateField
import io.github.mirhamedjz.ehligible.app.ui.InfoCard
import io.github.mirhamedjz.ehligible.app.ui.components.ConfettiBurst
import io.github.mirhamedjz.ehligible.app.ui.components.MapleLeafProgress
import io.github.mirhamedjz.ehligible.app.ui.newTripId
import io.github.mirhamedjz.ehligible.app.ui.theme.CountdownNumberStyle
import io.github.mirhamedjz.ehligible.core.presence.Trip
import io.github.mirhamedjz.ehligible.core.rules.DateRange
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private const val STEP_COUNT = 7

/** Form dialogs don't close on an outside tap, so typed answers aren't lost by accident. */
private val FormDialogProperties = DialogProperties(dismissOnClickOutside = false)

@Composable
fun OnboardingFlow(viewModel: AppViewModel) {
    val draft = viewModel.onboarding
    val today = viewModel.currentDate()
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
        if (draft.step in 1 until STEP_COUNT - 1) {
            Column(Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                Text(
                    stringResource(Res.string.onb_step, draft.step, STEP_COUNT - 2),
                    style = MaterialTheme.typography.labelMedium,
                )
                LinearProgressIndicator(
                    progress = { draft.step / (STEP_COUNT - 2f) },
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                )
            }
        }
        AnimatedContent(
            targetState = draft.step,
            transitionSpec = { (slideInHorizontally { it / 3 } + fadeIn()) togetherWith fadeOut() },
            modifier = Modifier.weight(1f),
            label = "onboarding",
        ) { step ->
            val next = { draft.step = step + 1 }
            val back = { draft.step = step - 1 }
            when (step) {
                0 -> WelcomeStep(next)

                1 -> PrDateStep(draft.prDate, { draft.prDate = it }, today, next)

                2 -> StatusStep(viewModel, today, next, back)

                3 -> TripsStep(viewModel, today, next, back)

                4 -> LocationStep(next, back)

                5 -> NotificationsStep(onDecided = { allowed ->
                    draft.notifications = allowed
                    next()
                }, back)

                else -> DoneStep(viewModel)
            }
        }
    }
}

@Composable
private fun StepPage(
    title: String,
    body: String?,
    primary: String,
    onPrimary: () -> Unit,
    primaryEnabled: Boolean = true,
    secondary: String? = null,
    onSecondary: () -> Unit = {},
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.headlineMedium)
            if (body !=
                null
            ) {
                Text(
                    body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            content()
            Spacer(Modifier.height(8.dp))
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) TextButton(onClick = onBack) { Text(stringResource(Res.string.action_back)) }
            Spacer(Modifier.weight(1f))
            if (secondary != null) TextButton(onClick = onSecondary) { Text(secondary) }
            Button(onClick = onPrimary, enabled = primaryEnabled) { Text(primary) }
        }
    }
}

@Composable
private fun WelcomeStep(onStart: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        MapleLeafProgress(
            progress = 0.62f,
            contentDescription = stringResource(Res.string.leaf_description, "62"),
            modifier = Modifier.size(200.dp),
        )
        Text(
            stringResource(Res.string.onb_welcome_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(Res.string.onb_welcome_body),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 480.dp),
        )
        Button(onClick = onStart, modifier = Modifier.padding(top = 8.dp)) {
            Text(stringResource(Res.string.onb_welcome_start))
        }
        Text(
            stringResource(Res.string.disclaimer_short),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PrDateStep(prDate: LocalDate?, onPrDate: (LocalDate) -> Unit, today: LocalDate, next: () -> Unit) {
    StepPage(
        title = stringResource(Res.string.onb_pr_title),
        body = stringResource(Res.string.onb_pr_body),
        primary = stringResource(Res.string.action_continue),
        onPrimary = next,
        primaryEnabled = prDate != null,
    ) {
        DateField(stringResource(Res.string.settings_pr_date), prDate, onPrDate, maxDate = today)
    }
}

@Composable
private fun StatusStep(viewModel: AppViewModel, today: LocalDate, next: () -> Unit, back: () -> Unit) {
    val draft = viewModel.onboarding
    val prDate = draft.prDate ?: today
    var addingGap by remember { mutableStateOf(false) }
    val ready = draft.hadStatus == false || (draft.hadStatus == true && draft.firstStatusDate != null)
    StepPage(
        title = stringResource(Res.string.onb_status_title),
        body = stringResource(Res.string.onb_status_body),
        primary = stringResource(Res.string.action_continue),
        onPrimary = next,
        primaryEnabled = ready,
        onBack = back,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ChoiceButton(stringResource(Res.string.onb_status_yes), draft.hadStatus == true) { draft.hadStatus = true }
            ChoiceButton(stringResource(Res.string.onb_status_no), draft.hadStatus == false) { draft.hadStatus = false }
        }
        if (draft.hadStatus == true) {
            DateField(
                stringResource(Res.string.onb_status_since),
                draft.firstStatusDate,
                { draft.firstStatusDate = it },
                maxDate = prDate.minus(1, DateTimeUnit.DAY),
                supportingText = stringResource(Res.string.onb_status_since_hint),
            )
            Text(stringResource(Res.string.onb_status_gaps_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(Res.string.onb_status_gaps_body), style = MaterialTheme.typography.bodyMedium)
            draft.statusGaps.forEachIndexed { index, gap ->
                RemovableRow("${formatDate(gap.start)} → ${formatDate(gap.endInclusive)}") {
                    draft.statusGaps.removeAt(index)
                }
            }
            OutlinedButton(onClick = { addingGap = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(stringResource(Res.string.onb_status_add_gap))
            }
        }
    }
    if (addingGap) {
        RangeDialog(
            title = stringResource(Res.string.onb_status_add_gap),
            maxDate = prDate.minus(1, DateTimeUnit.DAY),
            onDismiss = { addingGap = false },
            onConfirm = { start, end ->
                draft.statusGaps.add(DateRange(start, end))
                addingGap = false
            },
        )
    }
}

@Composable
private fun TripsStep(viewModel: AppViewModel, today: LocalDate, next: () -> Unit, back: () -> Unit) {
    val draft = viewModel.onboarding
    var adding by remember { mutableStateOf(false) }
    val since =
        listOfNotNull(draft.firstStatusDate.takeIf { draft.hadStatus == true }, draft.prDate).minOrNull() ?: today
    val fiveYearsAgo = today.minus(5, DateTimeUnit.YEAR)
    StepPage(
        title = stringResource(Res.string.onb_trips_title),
        body = stringResource(Res.string.onb_trips_body, formatDate(maxOf(since, fiveYearsAgo))),
        primary = stringResource(Res.string.action_continue),
        onPrimary = next,
        primaryEnabled = draft.noTrips || draft.trips.isNotEmpty(),
        onBack = back,
    ) {
        // The whole row toggles, not just the small box.
        Row(
            Modifier
                .fillMaxWidth()
                .toggleable(value = draft.noTrips, role = Role.Checkbox, onValueChange = { draft.noTrips = it })
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = draft.noTrips, onCheckedChange = null)
            Spacer(Modifier.size(12.dp))
            Text(stringResource(Res.string.onb_trips_none))
        }
        if (!draft.noTrips) {
            draft.trips.forEachIndexed { index, trip ->
                val label = buildString {
                    append("${formatDate(trip.departure)} → ${formatDate(trip.returnDate)}")
                    if (trip.destination.isNotBlank()) append(" · ${trip.destination}")
                }
                RemovableRow(label) { draft.trips.removeAt(index) }
            }
            OutlinedButton(onClick = { adding = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(stringResource(Res.string.journal_add))
            }
        }
    }
    if (adding) {
        TripDialog(onDismiss = { adding = false }) { trip ->
            draft.trips.add(trip)
            adding = false
        }
    }
}

@Composable
private fun LocationStep(next: () -> Unit, back: () -> Unit) {
    val permission = rememberLocationPermission { next() }
    StepPage(
        title = stringResource(Res.string.onb_location_title),
        body = stringResource(Res.string.onb_location_body),
        primary = if (permission.granted) {
            stringResource(
                Res.string.action_continue,
            )
        } else {
            stringResource(Res.string.onb_location_allow)
        },
        onPrimary = { if (permission.granted) next() else permission.request() },
        secondary = if (permission.granted) null else stringResource(Res.string.onb_location_decline),
        onSecondary = next,
        onBack = back,
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("🧭", style = MaterialTheme.typography.displayLarge)
        }
        if (permission.granted) {
            InfoCard(
                stringResource(Res.string.onb_location_on),
                container = MaterialTheme.colorScheme.primaryContainer,
            )
        }
    }
}

@Composable
private fun NotificationsStep(onDecided: (Boolean) -> Unit, back: () -> Unit) {
    val permission = rememberNotificationPermission { granted -> onDecided(granted) }
    StepPage(
        title = stringResource(Res.string.onb_notifications_title),
        body = stringResource(Res.string.onb_notifications_body),
        primary = stringResource(Res.string.onb_notifications_allow),
        onPrimary = { if (permission.granted) onDecided(true) else permission.request() },
        secondary = stringResource(Res.string.action_not_now),
        onSecondary = { onDecided(false) },
        onBack = back,
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("🎉", style = MaterialTheme.typography.displayLarge)
        }
    }
}

@Composable
private fun DoneStep(viewModel: AppViewModel) {
    val answers = viewModel.onboarding.answers() ?: return
    val preview = remember(answers) { viewModel.previewDashboard(answers) }
    var confetti by remember { mutableStateOf(true) }
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        ) {
            Text(stringResource(Res.string.onb_done_title), style = MaterialTheme.typography.headlineMedium)
            if (preview != null) {
                MapleLeafProgress(
                    progress = preview.progress,
                    contentDescription = stringResource(
                        Res.string.leaf_description,
                        (preview.progress * 100).toInt().toString(),
                    ),
                    modifier = Modifier.size(200.dp),
                )
                val left = preview.daysLeft
                if (preview.isEligibleToday) {
                    Text(
                        "🎉 " + stringResource(Res.string.home_eligible_title),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                } else if (left != null) {
                    Text(formatNumber(left), style = CountdownNumberStyle, color = MaterialTheme.colorScheme.primary)
                    Text(
                        pluralStringResource(Res.plurals.home_days_to_go, left),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }
            InfoCard(
                stringResource(Res.string.onb_done_widget),
                container = MaterialTheme.colorScheme.secondaryContainer,
            )
            rememberWidgetPinner()?.let { pin ->
                OutlinedButton(onClick = pin) { Text(stringResource(Res.string.action_add_widget)) }
            }
            Button(onClick = {
                viewModel.completeOnboarding(answers)
            }) { Text(stringResource(Res.string.onb_done_button)) }
        }
        if (confetti) ConfettiBurst(key = "welcome", onFinished = { confetti = false })
    }
}

@Composable
private fun ChoiceButton(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) Button(onClick = onClick) { Text(label) } else OutlinedButton(onClick = onClick) { Text(label) }
}

@Composable
private fun RemovableRow(label: String, onRemove: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, contentDescription = null) }
        }
    }
}

@Composable
private fun RangeDialog(
    title: String,
    maxDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, LocalDate) -> Unit,
) {
    var start by remember { mutableStateOf<LocalDate?>(null) }
    var end by remember { mutableStateOf<LocalDate?>(null) }
    val s = start
    val e = end
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = FormDialogProperties,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DateField(stringResource(Res.string.range_from), start, { start = it }, maxDate = maxDate)
                DateField(stringResource(Res.string.range_to), end, { end = it }, maxDate = maxDate, minDate = start)
            }
        },
        confirmButton = {
            TextButton(enabled = s != null && e != null && e >= s, onClick = {
                if (s != null &&
                    e != null
                ) {
                    onConfirm(s, e)
                }
            }) {
                Text(stringResource(Res.string.action_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) } },
    )
}

@Composable
private fun TripDialog(onDismiss: () -> Unit, onConfirm: (Trip) -> Unit) {
    var departure by remember { mutableStateOf<LocalDate?>(null) }
    var returnDate by remember { mutableStateOf<LocalDate?>(null) }
    var destination by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    val dep = departure
    val ret = returnDate
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = FormDialogProperties,
        title = { Text(stringResource(Res.string.journal_add)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DateField(stringResource(Res.string.trip_left), departure, { departure = it })
                DateField(stringResource(Res.string.trip_returned), returnDate, {
                    returnDate = it
                }, minDate = departure)
                OutlinedTextField(destination, {
                    destination = it
                }, label = { Text(stringResource(Res.string.trip_destination)) }, singleLine = true)
                OutlinedTextField(
                    reason,
                    { reason = it },
                    label = { Text(stringResource(Res.string.trip_reason)) },
                    placeholder = { Text(stringResource(Res.string.trip_reason_hint)) },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = dep != null && ret != null && ret >= dep,
                onClick = {
                    if (dep != null &&
                        ret != null
                    ) {
                        onConfirm(Trip(newTripId(), dep, ret, destination.trim(), reason.trim()))
                    }
                },
            ) { Text(stringResource(Res.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) } },
    )
}
