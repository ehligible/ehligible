package io.github.mirhamedjz.ehligible.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import io.github.mirhamedjz.ehligible.app.platform.backgroundPermissionHint
import io.github.mirhamedjz.ehligible.app.platform.rememberBackgroundLocationPermission
import io.github.mirhamedjz.ehligible.app.platform.rememberLocationPermission
import io.github.mirhamedjz.ehligible.app.platform.rememberNotificationPermission
import io.github.mirhamedjz.ehligible.app.platform.rememberOpenDocument
import io.github.mirhamedjz.ehligible.app.platform.rememberSaveDocument
import io.github.mirhamedjz.ehligible.app.platform.rememberWidgetPinner
import io.github.mirhamedjz.ehligible.app.resources.Res
import io.github.mirhamedjz.ehligible.app.resources.action_add_widget
import io.github.mirhamedjz.ehligible.app.resources.action_back
import io.github.mirhamedjz.ehligible.app.resources.action_cancel
import io.github.mirhamedjz.ehligible.app.resources.action_ok
import io.github.mirhamedjz.ehligible.app.resources.disclaimer_short
import io.github.mirhamedjz.ehligible.app.resources.onb_background_allow
import io.github.mirhamedjz.ehligible.app.resources.onb_background_body
import io.github.mirhamedjz.ehligible.app.resources.onb_background_title
import io.github.mirhamedjz.ehligible.app.resources.onb_location_allow
import io.github.mirhamedjz.ehligible.app.resources.onb_location_body
import io.github.mirhamedjz.ehligible.app.resources.onb_location_decline
import io.github.mirhamedjz.ehligible.app.resources.onb_location_title
import io.github.mirhamedjz.ehligible.app.resources.settings_about
import io.github.mirhamedjz.ehligible.app.resources.settings_background_off
import io.github.mirhamedjz.ehligible.app.resources.settings_background_on
import io.github.mirhamedjz.ehligible.app.resources.settings_background_turn_on
import io.github.mirhamedjz.ehligible.app.resources.settings_backup
import io.github.mirhamedjz.ehligible.app.resources.settings_backup_export
import io.github.mirhamedjz.ehligible.app.resources.settings_backup_import
import io.github.mirhamedjz.ehligible.app.resources.settings_backup_import_confirm
import io.github.mirhamedjz.ehligible.app.resources.settings_backup_invalid
import io.github.mirhamedjz.ehligible.app.resources.settings_backup_restored
import io.github.mirhamedjz.ehligible.app.resources.settings_backup_saved
import io.github.mirhamedjz.ehligible.app.resources.settings_countdown
import io.github.mirhamedjz.ehligible.app.resources.settings_credits
import io.github.mirhamedjz.ehligible.app.resources.settings_location
import io.github.mirhamedjz.ehligible.app.resources.settings_location_off
import io.github.mirhamedjz.ehligible.app.resources.settings_location_on
import io.github.mirhamedjz.ehligible.app.resources.settings_location_turn_on
import io.github.mirhamedjz.ehligible.app.resources.settings_margin
import io.github.mirhamedjz.ehligible.app.resources.settings_margin_body
import io.github.mirhamedjz.ehligible.app.resources.settings_margin_value
import io.github.mirhamedjz.ehligible.app.resources.settings_notifications
import io.github.mirhamedjz.ehligible.app.resources.settings_notifications_body
import io.github.mirhamedjz.ehligible.app.resources.settings_pr_date
import io.github.mirhamedjz.ehligible.app.resources.settings_privacy
import io.github.mirhamedjz.ehligible.app.resources.settings_profile
import io.github.mirhamedjz.ehligible.app.resources.settings_rare_cases
import io.github.mirhamedjz.ehligible.app.resources.settings_source
import io.github.mirhamedjz.ehligible.app.resources.settings_status_date
import io.github.mirhamedjz.ehligible.app.resources.settings_target
import io.github.mirhamedjz.ehligible.app.resources.settings_target_exact
import io.github.mirhamedjz.ehligible.app.resources.settings_target_safer
import io.github.mirhamedjz.ehligible.app.resources.settings_title
import io.github.mirhamedjz.ehligible.app.resources.settings_version
import io.github.mirhamedjz.ehligible.app.ui.AppUiState
import io.github.mirhamedjz.ehligible.app.ui.AppViewModel
import io.github.mirhamedjz.ehligible.app.ui.DateField
import io.github.mirhamedjz.ehligible.app.ui.Links
import io.github.mirhamedjz.ehligible.app.ui.SectionTitle
import io.github.mirhamedjz.ehligible.app.ui.screenPadding
import io.github.mirhamedjz.ehligible.core.model.CountdownTarget
import io.github.mirhamedjz.ehligible.core.model.Settings
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun SettingsScreen(viewModel: AppViewModel, ready: AppUiState.Ready, onBack: () -> Unit) {
    val settings = ready.data.settings
    val profile = ready.dashboard.profile
    val today = ready.dashboard.today
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val uriHandler = LocalUriHandler.current

    val savedText = stringResource(Res.string.settings_backup_saved)
    val restoredText = stringResource(Res.string.settings_backup_restored)
    val invalidText = stringResource(Res.string.settings_backup_invalid)
    var pendingImport by remember { mutableStateOf<String?>(null) }
    var showDisclosure by remember { mutableStateOf(false) }

    val location = rememberLocationPermission()
    val background = rememberBackgroundLocationPermission()
    var showBackgroundDisclosure by remember { mutableStateOf(false) }
    val pinWidget = rememberWidgetPinner()
    val notifications = rememberNotificationPermission { granted -> if (granted) viewModel.setNotifications(true) }
    val saveBackup =
        rememberSaveDocument("application/json") { ok -> if (ok) scope.launch { snackbar.showSnackbar(savedText) } }
    val openBackup = rememberOpenDocument { text ->
        if (text != null) pendingImport = text
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.action_back),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = screenPadding(padding),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { SectionTitle(stringResource(Res.string.settings_countdown)) }
            item {
                SettingCard {
                    Text(stringResource(Res.string.settings_target), style = MaterialTheme.typography.titleMedium)
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        CountdownTarget.entries.forEachIndexed { index, target ->
                            SegmentedButton(
                                selected = settings.countdownTarget == target,
                                onClick = { viewModel.setCountdownTarget(target) },
                                shape = SegmentedButtonDefaults.itemShape(index, CountdownTarget.entries.size),
                            ) {
                                Text(
                                    stringResource(
                                        if (target ==
                                            CountdownTarget.EXACT
                                        ) {
                                            Res.string.settings_target_exact
                                        } else {
                                            Res.string.settings_target_safer
                                        },
                                    ),
                                )
                            }
                        }
                    }
                }
            }
            item {
                SettingCard {
                    var margin by remember(settings.marginDays) { mutableFloatStateOf(settings.marginDays.toFloat()) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(Res.string.settings_margin),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            pluralStringResource(
                                Res.plurals.settings_margin_value,
                                margin.roundToInt(),
                                margin.roundToInt(),
                            ),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    Text(stringResource(Res.string.settings_margin_body), style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = margin,
                        onValueChange = { margin = it },
                        onValueChangeFinished = { viewModel.setMargin(margin.roundToInt()) },
                        valueRange = 0f..Settings.MAX_MARGIN_DAYS.toFloat(),
                    )
                }
            }
            if (pinWidget != null) {
                item {
                    OutlinedButton(onClick = pinWidget, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(Res.string.action_add_widget))
                    }
                }
            }
            item {
                SettingCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(Res.string.settings_notifications),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                stringResource(Res.string.settings_notifications_body),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Switch(
                            checked = settings.notificationsEnabled && notifications.granted,
                            onCheckedChange = { on ->
                                if (!on) {
                                    viewModel.setNotifications(false)
                                } else if (notifications.granted) {
                                    viewModel.setNotifications(true)
                                } else {
                                    notifications.request()
                                }
                            },
                        )
                    }
                }
            }
            item {
                SettingCard {
                    Text(stringResource(Res.string.settings_location), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(
                            if (location.granted) Res.string.settings_location_on else Res.string.settings_location_off,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (!location.granted) {
                        FilledTonalButton(onClick = {
                            showDisclosure = true
                        }, modifier = Modifier.padding(top = 8.dp)) {
                            Text(stringResource(Res.string.settings_location_turn_on))
                        }
                    }
                    if (location.granted && viewModel.appInfo.backgroundLocationSupported) {
                        val backgroundText = if (background.granted) {
                            Res.string.settings_background_on
                        } else {
                            Res.string.settings_background_off
                        }
                        Text(stringResource(backgroundText), style = MaterialTheme.typography.bodyMedium)
                        if (!background.granted) {
                            FilledTonalButton(
                                onClick = { showBackgroundDisclosure = true },
                                modifier = Modifier.padding(top = 8.dp),
                            ) { Text(stringResource(Res.string.settings_background_turn_on)) }
                        }
                    }
                }
            }

            item { SectionTitle(stringResource(Res.string.settings_profile)) }
            item {
                SettingCard {
                    DateField(
                        stringResource(Res.string.settings_pr_date),
                        profile.prDate,
                        viewModel::setPrDate,
                        maxDate = today,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DateField(
                            stringResource(Res.string.settings_status_date),
                            profile.prePrStatus.firstStatusDate,
                            { viewModel.setPrePrStatus(profile.prePrStatus.copy(firstStatusDate = it)) },
                            modifier = Modifier.weight(1f).padding(top = 8.dp),
                            maxDate = profile.prDate.minus(1, DateTimeUnit.DAY),
                        )
                        if (profile.prePrStatus.firstStatusDate != null) {
                            IconButton(onClick = {
                                viewModel.setPrePrStatus(profile.prePrStatus.copy(firstStatusDate = null))
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = null)
                            }
                        }
                    }
                }
            }

            item { SectionTitle(stringResource(Res.string.settings_backup)) }
            item {
                SettingCard {
                    OutlinedButton(
                        onClick = {
                            scope.launch { saveBackup("ehligible-backup-$today.json", viewModel.exportBackup()) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(Res.string.settings_backup_export)) }
                    OutlinedButton(onClick = openBackup, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(Res.string.settings_backup_import))
                    }
                }
            }

            item { SectionTitle(stringResource(Res.string.settings_about)) }
            item {
                SettingCard {
                    Text(
                        stringResource(Res.string.settings_version, viewModel.appInfo.versionName),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(stringResource(Res.string.disclaimer_short), style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(Res.string.settings_rare_cases), style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(Res.string.settings_credits), style = MaterialTheme.typography.bodySmall)
                    Row {
                        TextButton(onClick = {
                            uriHandler.openUri(Links.PRIVACY_POLICY)
                        }) { Text(stringResource(Res.string.settings_privacy)) }
                        TextButton(onClick = {
                            uriHandler.openUri(Links.SOURCE_CODE)
                        }) { Text(stringResource(Res.string.settings_source)) }
                    }
                }
            }
        }
    }

    if (showDisclosure) {
        AlertDialog(
            onDismissRequest = { showDisclosure = false },
            title = { Text(stringResource(Res.string.onb_location_title)) },
            text = { Text(stringResource(Res.string.onb_location_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showDisclosure = false
                    location.request()
                }) { Text(stringResource(Res.string.onb_location_allow)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDisclosure = false
                }) { Text(stringResource(Res.string.onb_location_decline)) }
            },
        )
    }

    if (showBackgroundDisclosure) {
        AlertDialog(
            onDismissRequest = { showBackgroundDisclosure = false },
            title = { Text(stringResource(Res.string.onb_background_title)) },
            text = {
                Text(stringResource(Res.string.onb_background_body) + " " + stringResource(backgroundPermissionHint))
            },
            confirmButton = {
                TextButton(onClick = {
                    showBackgroundDisclosure = false
                    background.request()
                }) { Text(stringResource(Res.string.onb_background_allow)) }
            },
            dismissButton = {
                TextButton(onClick = { showBackgroundDisclosure = false }) {
                    Text(stringResource(Res.string.onb_location_decline))
                }
            },
        )
    }

    pendingImport?.let { text ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text(stringResource(Res.string.settings_backup_import)) },
            text = { Text(stringResource(Res.string.settings_backup_import_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingImport = null
                    scope.launch {
                        snackbar.showSnackbar(if (viewModel.importBackup(text)) restoredText else invalidText)
                    }
                }) { Text(stringResource(Res.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingImport = null }) { Text(stringResource(Res.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun SettingCard(content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { content() }
    }
}
