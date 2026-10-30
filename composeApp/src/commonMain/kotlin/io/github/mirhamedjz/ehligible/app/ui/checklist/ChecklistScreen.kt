package io.github.mirhamedjz.ehligible.app.ui.checklist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.mirhamedjz.ehligible.app.platform.formatDate
import io.github.mirhamedjz.ehligible.app.resources.Res
import io.github.mirhamedjz.ehligible.app.resources.action_back
import io.github.mirhamedjz.ehligible.app.resources.checklist_age_maybe
import io.github.mirhamedjz.ehligible.app.resources.checklist_age_not_needed
import io.github.mirhamedjz.ehligible.app.resources.checklist_all_done
import io.github.mirhamedjz.ehligible.app.resources.checklist_birth_year
import io.github.mirhamedjz.ehligible.app.resources.checklist_fee
import io.github.mirhamedjz.ehligible.app.resources.checklist_fee_body
import io.github.mirhamedjz.ehligible.app.resources.checklist_language
import io.github.mirhamedjz.ehligible.app.resources.checklist_language_body
import io.github.mirhamedjz.ehligible.app.resources.checklist_link_calculator
import io.github.mirhamedjz.ehligible.app.resources.checklist_link_who
import io.github.mirhamedjz.ehligible.app.resources.checklist_links
import io.github.mirhamedjz.ehligible.app.resources.checklist_pr
import io.github.mirhamedjz.ehligible.app.resources.checklist_pr_body
import io.github.mirhamedjz.ehligible.app.resources.checklist_presence
import io.github.mirhamedjz.ehligible.app.resources.checklist_presence_done
import io.github.mirhamedjz.ehligible.app.resources.checklist_presence_todo
import io.github.mirhamedjz.ehligible.app.resources.checklist_prohibitions
import io.github.mirhamedjz.ehligible.app.resources.checklist_prohibitions_body
import io.github.mirhamedjz.ehligible.app.resources.checklist_taxes
import io.github.mirhamedjz.ehligible.app.resources.checklist_taxes_body
import io.github.mirhamedjz.ehligible.app.resources.checklist_test
import io.github.mirhamedjz.ehligible.app.resources.checklist_test_body
import io.github.mirhamedjz.ehligible.app.resources.checklist_title
import io.github.mirhamedjz.ehligible.app.resources.home_apply_from
import io.github.mirhamedjz.ehligible.app.ui.AppUiState
import io.github.mirhamedjz.ehligible.app.ui.AppViewModel
import io.github.mirhamedjz.ehligible.app.ui.Links
import io.github.mirhamedjz.ehligible.app.ui.SectionTitle
import io.github.mirhamedjz.ehligible.app.ui.screenPadding
import io.github.mirhamedjz.ehligible.core.rules.ApplicationRequirements
import io.github.mirhamedjz.ehligible.core.rules.Requirement
import org.jetbrains.compose.resources.stringResource

private object Items {
    const val PR = "pr"
    const val TAXES = "taxes"
    const val LANGUAGE = "language"
    const val TEST = "test"
    const val FEE = "fee"
    const val PROHIBITIONS = "prohibitions"
}

@Composable
fun ChecklistScreen(viewModel: AppViewModel, ready: AppUiState.Ready, onBack: () -> Unit) {
    val dashboard = ready.dashboard
    val ticked = ready.data.settings.checklist
    val profile = dashboard.profile
    val applicationDate = dashboard.countdownDate ?: dashboard.today
    val taxYears = ApplicationRequirements.taxYearsToConsider(applicationDate).joinToString(", ")
    val languageAndTest = ApplicationRequirements.languageAndTest(profile.birthYear, applicationDate)
    val uriHandler = LocalUriHandler.current
    val presenceDone = dashboard.isEligibleToday
    val ageNote = when (languageAndTest) {
        Requirement.NOT_REQUIRED -> stringResource(Res.string.checklist_age_not_needed)
        Requirement.MAYBE -> stringResource(Res.string.checklist_age_maybe)
        Requirement.REQUIRED, Requirement.UNKNOWN -> null
    }
    val languageApplies = languageAndTest != Requirement.NOT_REQUIRED
    val required = buildList {
        add(Items.PR)
        add(Items.TAXES)
        if (languageApplies) addAll(listOf(Items.LANGUAGE, Items.TEST))
        add(Items.FEE)
        add(Items.PROHIBITIONS)
    }
    val allDone = presenceDone && required.all { it in ticked }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.checklist_title)) },
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
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = screenPadding(padding),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (allDone) {
                item {
                    Text(
                        "🎉 " + stringResource(Res.string.checklist_all_done),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
            }
            item {
                val todo = dashboard.projection.firstEligibleDate?.let {
                    stringResource(Res.string.home_apply_from, formatDate(it))
                }.orEmpty()
                ChecklistItem(
                    title = stringResource(Res.string.checklist_presence),
                    body = if (presenceDone) {
                        stringResource(
                            Res.string.checklist_presence_done,
                        )
                    } else {
                        stringResource(Res.string.checklist_presence_todo, todo)
                    },
                    checked = presenceDone,
                    onToggle = null,
                )
            }
            item {
                ChecklistItem(
                    stringResource(Res.string.checklist_pr),
                    stringResource(Res.string.checklist_pr_body),
                    Items.PR in ticked,
                    { viewModel.toggleChecklistItem(Items.PR) },
                )
            }
            item {
                ChecklistItem(
                    stringResource(Res.string.checklist_taxes),
                    stringResource(Res.string.checklist_taxes_body, taxYears),
                    Items.TAXES in ticked,
                    { viewModel.toggleChecklistItem(Items.TAXES) },
                )
            }
            item {
                ChecklistItem(
                    stringResource(Res.string.checklist_language),
                    listOfNotNull(stringResource(Res.string.checklist_language_body), ageNote).joinToString("\n"),
                    !languageApplies || Items.LANGUAGE in ticked,
                    if (languageApplies) ({ viewModel.toggleChecklistItem(Items.LANGUAGE) }) else null,
                )
            }
            item {
                ChecklistItem(
                    stringResource(Res.string.checklist_test),
                    listOfNotNull(stringResource(Res.string.checklist_test_body), ageNote).joinToString("\n"),
                    !languageApplies || Items.TEST in ticked,
                    if (languageApplies) ({ viewModel.toggleChecklistItem(Items.TEST) }) else null,
                )
            }
            item {
                ChecklistItem(
                    stringResource(Res.string.checklist_fee),
                    stringResource(Res.string.checklist_fee_body),
                    Items.FEE in ticked,
                    { viewModel.toggleChecklistItem(Items.FEE) },
                )
            }
            item {
                ChecklistItem(
                    stringResource(Res.string.checklist_prohibitions),
                    stringResource(Res.string.checklist_prohibitions_body),
                    Items.PROHIBITIONS in ticked,
                    { viewModel.toggleChecklistItem(Items.PROHIBITIONS) },
                )
            }
            item { BirthYearField(profile.birthYear, viewModel::setBirthYear) }
            item { SectionTitle(stringResource(Res.string.checklist_links)) }
            item {
                Column {
                    TextButton(onClick = {
                        uriHandler.openUri(Links.IRCC_WHO_CAN_APPLY)
                    }) { Text(stringResource(Res.string.checklist_link_who)) }
                    TextButton(onClick = {
                        uriHandler.openUri(Links.IRCC_CALCULATOR)
                    }) { Text(stringResource(Res.string.checklist_link_calculator)) }
                }
            }
        }
    }
}

@Composable
private fun ChecklistItem(title: String, body: String, checked: Boolean, onToggle: (() -> Unit)?) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        // The whole card toggles, not just the small box.
        val toggle = if (onToggle != null) {
            Modifier.toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() })
        } else {
            Modifier
        }
        Row(Modifier.fillMaxWidth().then(toggle).padding(12.dp), verticalAlignment = Alignment.Top) {
            Checkbox(checked = checked, onCheckedChange = null, enabled = onToggle != null)
            Column(Modifier.padding(top = 10.dp, end = 4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun BirthYearField(year: Int?, onChange: (Int?) -> Unit) {
    var text by remember(year) { mutableStateOf(year?.toString().orEmpty()) }
    OutlinedTextField(
        value = text,
        onValueChange = { input ->
            val digits = input.filter(Char::isDigit).take(4)
            text = digits
            val parsed = digits.toIntOrNull()
            if (digits.isEmpty()) {
                onChange(null)
            } else if (parsed != null && digits.length == 4 &&
                parsed in 1900..2100
            ) {
                onChange(parsed)
            }
        },
        label = { Text(stringResource(Res.string.checklist_birth_year)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}
