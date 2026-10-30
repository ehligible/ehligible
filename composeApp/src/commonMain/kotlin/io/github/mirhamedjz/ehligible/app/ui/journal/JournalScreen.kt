package io.github.mirhamedjz.ehligible.app.ui.journal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.mirhamedjz.ehligible.app.platform.countryName
import io.github.mirhamedjz.ehligible.app.platform.formatDate
import io.github.mirhamedjz.ehligible.app.platform.rememberSaveDocument
import io.github.mirhamedjz.ehligible.app.platform.rememberShareText
import io.github.mirhamedjz.ehligible.app.resources.Res
import io.github.mirhamedjz.ehligible.app.resources.action_back
import io.github.mirhamedjz.ehligible.app.resources.export_col_days
import io.github.mirhamedjz.ehligible.app.resources.export_col_destination
import io.github.mirhamedjz.ehligible.app.resources.export_col_left
import io.github.mirhamedjz.ehligible.app.resources.export_col_reason
import io.github.mirhamedjz.ehligible.app.resources.export_col_returned
import io.github.mirhamedjz.ehligible.app.resources.export_ongoing
import io.github.mirhamedjz.ehligible.app.resources.export_subject
import io.github.mirhamedjz.ehligible.app.resources.journal_add
import io.github.mirhamedjz.ehligible.app.resources.journal_current
import io.github.mirhamedjz.ehligible.app.resources.journal_empty
import io.github.mirhamedjz.ehligible.app.resources.journal_export
import io.github.mirhamedjz.ehligible.app.resources.journal_export_csv
import io.github.mirhamedjz.ehligible.app.resources.journal_export_saved
import io.github.mirhamedjz.ehligible.app.resources.journal_export_share
import io.github.mirhamedjz.ehligible.app.resources.journal_past
import io.github.mirhamedjz.ehligible.app.resources.journal_planned
import io.github.mirhamedjz.ehligible.app.resources.journal_rule
import io.github.mirhamedjz.ehligible.app.resources.journal_title
import io.github.mirhamedjz.ehligible.app.resources.trip_days_away
import io.github.mirhamedjz.ehligible.app.resources.trip_detected
import io.github.mirhamedjz.ehligible.app.resources.trip_needs_review
import io.github.mirhamedjz.ehligible.app.resources.trip_ongoing
import io.github.mirhamedjz.ehligible.app.resources.trip_planned
import io.github.mirhamedjz.ehligible.app.resources.trip_same_day
import io.github.mirhamedjz.ehligible.app.ui.AppUiState
import io.github.mirhamedjz.ehligible.app.ui.InfoCard
import io.github.mirhamedjz.ehligible.app.ui.Pill
import io.github.mirhamedjz.ehligible.app.ui.SectionTitle
import io.github.mirhamedjz.ehligible.app.ui.countriesLabel
import io.github.mirhamedjz.ehligible.app.ui.screenPadding
import io.github.mirhamedjz.ehligible.core.export.TripExport
import io.github.mirhamedjz.ehligible.core.presence.TimelineTrip
import io.github.mirhamedjz.ehligible.core.presence.TripSource
import io.github.mirhamedjz.ehligible.core.rules.EligibilityWindow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
    ready: AppUiState.Ready,
    onBack: () -> Unit,
    onAddTrip: () -> Unit,
    onOpenManualTrip: (String) -> Unit,
    onOpenDetectedTrip: (LocalDate) -> Unit,
) {
    val dashboard = ready.dashboard
    val today = dashboard.today
    val planned = dashboard.trips.filter { it.isPlanned }.sortedBy { it.departure }
    val current = dashboard.trips.filter { trip -> !trip.isPlanned && trip.returnDate.let { it == null || it > today } }
    val past = dashboard.trips.filter { it !in planned && it !in current }.sortedByDescending { it.departure }

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val savedMessage = stringResource(Res.string.journal_export_saved)
    val share = rememberShareText()
    val saveCsv =
        rememberSaveDocument("text/csv") { ok -> if (ok) scope.launch { snackbar.showSnackbar(savedMessage) } }
    val labels = TripExport.Labels(
        departure = stringResource(Res.string.export_col_left),
        returnDate = stringResource(Res.string.export_col_returned),
        destination = stringResource(Res.string.export_col_destination),
        reason = stringResource(Res.string.export_col_reason),
        daysAbsent = stringResource(Res.string.export_col_days),
        ongoing = stringResource(Res.string.export_ongoing),
    )
    val subject = stringResource(Res.string.export_subject)
    // Export what IRCC will ask about: trips in the 5 years before the date the countdown points at.
    val since = EligibilityWindow.forApplicationDate(dashboard.countdownDate ?: today).start
    val exported = TripExport.select(dashboard.trips, since)
    var menuOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.journal_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.action_back),
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Default.Share, contentDescription = stringResource(Res.string.journal_export))
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.journal_export_share)) },
                                onClick = {
                                    menuOpen = false
                                    share(subject, TripExport.text(exported, labels, ::countryName) { formatDate(it) })
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.journal_export_csv)) },
                                onClick = {
                                    menuOpen = false
                                    saveCsv("ehligible-trips.csv", TripExport.csv(exported, labels, ::countryName))
                                },
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddTrip,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(Res.string.journal_add)) },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = screenPadding(padding, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { InfoCard(stringResource(Res.string.journal_rule)) }
            if (dashboard.trips.isEmpty()) {
                item {
                    Text(
                        stringResource(Res.string.journal_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
            }
            section(Res.string.journal_current, current, onOpenManualTrip, onOpenDetectedTrip)
            section(Res.string.journal_planned, planned, onOpenManualTrip, onOpenDetectedTrip)
            section(Res.string.journal_past, past, onOpenManualTrip, onOpenDetectedTrip)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.section(
    title: org.jetbrains.compose.resources.StringResource,
    trips: List<TimelineTrip>,
    onOpenManualTrip: (String) -> Unit,
    onOpenDetectedTrip: (LocalDate) -> Unit,
) {
    if (trips.isEmpty()) return
    item { SectionTitle(stringResource(title)) }
    items(trips, key = { "${it.source}-${it.manualId}-${it.departure}" }) { trip ->
        TripCard(trip) {
            if (trip.source ==
                TripSource.MANUAL
            ) {
                onOpenManualTrip(trip.manualId!!)
            } else {
                onOpenDetectedTrip(trip.departure)
            }
        }
    }
}

@Composable
private fun TripCard(trip: TimelineTrip, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (trip.isPlanned) {
                colors.tertiaryContainer.copy(
                    alpha = 0.5f,
                )
            } else {
                colors.surfaceContainerLow
            },
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val back = trip.returnDate?.let { formatDate(it) } ?: stringResource(Res.string.trip_ongoing)
            Text("${formatDate(trip.departure)} → $back", style = MaterialTheme.typography.titleMedium)
            val where = trip.destination.ifBlank { countriesLabel(trip.countries) }
            if (where.isNotBlank()) Text(where, style = MaterialTheme.typography.bodyLarge)
            if (trip.reason.isNotBlank()) {
                Text(
                    trip.reason,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
            val sameDay = trip.returnDate != null && trip.absentDays == 0
            Text(
                if (sameDay) {
                    stringResource(
                        Res.string.trip_same_day,
                    )
                } else {
                    pluralStringResource(Res.plurals.trip_days_away, trip.absentDays, trip.absentDays)
                },
                style = MaterialTheme.typography.labelLarge,
                color = colors.primary,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (trip.source ==
                    TripSource.DETECTED
                ) {
                    Pill(
                        stringResource(Res.string.trip_detected),
                        colors.secondaryContainer,
                        colors.onSecondaryContainer,
                    )
                }
                if (trip.isPlanned) {
                    Pill(
                        stringResource(Res.string.trip_planned),
                        colors.tertiaryContainer,
                        colors.onTertiaryContainer,
                    )
                }
                if (trip.needsReview) {
                    Pill(
                        stringResource(Res.string.trip_needs_review),
                        colors.errorContainer,
                        colors.onErrorContainer,
                    )
                }
            }
        }
    }
}
