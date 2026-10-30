package io.github.mirhamedjz.ehligible.app.ui.journal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.mirhamedjz.ehligible.app.platform.countryName
import io.github.mirhamedjz.ehligible.app.resources.Res
import io.github.mirhamedjz.ehligible.app.resources.action_back
import io.github.mirhamedjz.ehligible.app.resources.action_cancel
import io.github.mirhamedjz.ehligible.app.resources.action_delete
import io.github.mirhamedjz.ehligible.app.resources.action_save
import io.github.mirhamedjz.ehligible.app.resources.journal_rule
import io.github.mirhamedjz.ehligible.app.resources.trip_delete_confirm
import io.github.mirhamedjz.ehligible.app.resources.trip_destination
import io.github.mirhamedjz.ehligible.app.resources.trip_editor_edit
import io.github.mirhamedjz.ehligible.app.resources.trip_editor_new
import io.github.mirhamedjz.ehligible.app.resources.trip_left
import io.github.mirhamedjz.ehligible.app.resources.trip_planned_note
import io.github.mirhamedjz.ehligible.app.resources.trip_reason
import io.github.mirhamedjz.ehligible.app.resources.trip_reason_hint
import io.github.mirhamedjz.ehligible.app.resources.trip_return_before_departure
import io.github.mirhamedjz.ehligible.app.resources.trip_returned
import io.github.mirhamedjz.ehligible.app.ui.AppUiState
import io.github.mirhamedjz.ehligible.app.ui.AppViewModel
import io.github.mirhamedjz.ehligible.app.ui.DateField
import io.github.mirhamedjz.ehligible.app.ui.InfoCard
import io.github.mirhamedjz.ehligible.core.presence.TripNote
import io.github.mirhamedjz.ehligible.core.presence.TripSource
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource

/** Adds a trip, edits an entered one, or annotates/corrects a detected one. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripEditorScreen(
    viewModel: AppViewModel,
    ready: AppUiState.Ready,
    tripId: String?,
    detectedDeparture: LocalDate?,
    onDone: () -> Unit,
) {
    val existing = tripId?.let { id -> ready.data.trips.firstOrNull { it.id == id } }
    val detected = detectedDeparture?.let { d ->
        ready.dashboard.trips.firstOrNull {
            it.source == TripSource.DETECTED &&
                it.departure == d
        }
    }
    var departure by remember { mutableStateOf(existing?.departure ?: detected?.departure) }
    var returnDate by remember { mutableStateOf(existing?.returnDate ?: detected?.returnDate) }
    var destination by remember {
        mutableStateOf(
            existing?.destination
                ?: detected?.let {
                    it.destination.ifBlank { it.countries.sorted().joinToString(", ") { c -> countryName(c) } }
                }.orEmpty(),
        )
    }
    var reason by remember { mutableStateOf(existing?.reason ?: detected?.reason.orEmpty()) }
    var confirmDelete by remember { mutableStateOf(false) }

    val dep = departure
    val ret = returnDate
    val datesValid = dep != null && (ret == null || ret >= dep) && (ret != null || detected != null)
    val datesChanged = detected != null && (dep != detected.departure || ret != detected.returnDate)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (existing == null &&
                                detected == null
                            ) {
                                Res.string.trip_editor_new
                            } else {
                                Res.string.trip_editor_edit
                            },
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.action_back),
                        )
                    }
                },
                actions = {
                    if (existing != null) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.action_delete))
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DateField(stringResource(Res.string.trip_left), departure, { departure = it })
            DateField(
                stringResource(Res.string.trip_returned),
                returnDate,
                { returnDate = it },
                minDate = departure,
                supportingText = if (dep != null && ret != null &&
                    ret < dep
                ) {
                    stringResource(Res.string.trip_return_before_departure)
                } else {
                    null
                },
            )
            OutlinedTextField(
                value = destination,
                onValueChange = { destination = it },
                label = { Text(stringResource(Res.string.trip_destination)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text(stringResource(Res.string.trip_reason)) },
                placeholder = { Text(stringResource(Res.string.trip_reason_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (dep != null && dep > ready.dashboard.today) {
                InfoCard(
                    stringResource(Res.string.trip_planned_note),
                    container = MaterialTheme.colorScheme.tertiaryContainer,
                )
            }
            InfoCard(stringResource(Res.string.journal_rule))
            Button(
                enabled = datesValid,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val start = dep ?: return@Button
                    when {
                        detected != null && !datesChanged -> viewModel.annotateDetectedTrip(
                            start,
                            TripNote(destination.trim(), reason.trim()),
                        )

                        detected != null -> viewModel.correctDetectedTrip(
                            detected,
                            start,
                            ret ?: return@Button,
                            destination.trim(),
                            reason.trim(),
                        )

                        else -> viewModel.saveTrip(
                            existing?.id,
                            start,
                            ret ?: return@Button,
                            destination.trim(),
                            reason.trim(),
                        )
                    }
                    onDone()
                },
            ) { Text(stringResource(Res.string.action_save)) }
        }
    }

    if (confirmDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(Res.string.trip_delete_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTrip(existing.id)
                    confirmDelete = false
                    onDone()
                }) { Text(stringResource(Res.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(Res.string.action_cancel)) }
            },
        )
    }
}
