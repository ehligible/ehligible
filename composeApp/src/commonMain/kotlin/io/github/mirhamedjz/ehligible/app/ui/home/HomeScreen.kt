package io.github.mirhamedjz.ehligible.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.mirhamedjz.ehligible.app.platform.countryName
import io.github.mirhamedjz.ehligible.app.platform.flagEmoji
import io.github.mirhamedjz.ehligible.app.platform.formatDate
import io.github.mirhamedjz.ehligible.app.platform.formatNumber
import io.github.mirhamedjz.ehligible.app.resources.Res
import io.github.mirhamedjz.ehligible.app.resources.app_name
import io.github.mirhamedjz.ehligible.app.resources.disclaimer_short
import io.github.mirhamedjz.ehligible.app.resources.home_apply_from
import io.github.mirhamedjz.ehligible.app.resources.home_apply_link
import io.github.mirhamedjz.ehligible.app.resources.home_checklist
import io.github.mirhamedjz.ehligible.app.resources.home_counted
import io.github.mirhamedjz.ehligible.app.resources.home_days_to_go
import io.github.mirhamedjz.ehligible.app.resources.home_eligible_body
import io.github.mirhamedjz.ehligible.app.resources.home_eligible_title
import io.github.mirhamedjz.ehligible.app.resources.home_exact_date
import io.github.mirhamedjz.ehligible.app.resources.home_fun_fact
import io.github.mirhamedjz.ehligible.app.resources.home_not_reachable
import io.github.mirhamedjz.ehligible.app.resources.home_review_action
import io.github.mirhamedjz.ehligible.app.resources.home_review_banner
import io.github.mirhamedjz.ehligible.app.resources.home_safer_date
import io.github.mirhamedjz.ehligible.app.resources.home_settings
import io.github.mirhamedjz.ehligible.app.resources.home_trips
import io.github.mirhamedjz.ehligible.app.resources.leaf_description
import io.github.mirhamedjz.ehligible.app.resources.status_abroad
import io.github.mirhamedjz.ehligible.app.resources.status_abroad_unknown
import io.github.mirhamedjz.ehligible.app.resources.status_in_canada
import io.github.mirhamedjz.ehligible.app.resources.status_near_border
import io.github.mirhamedjz.ehligible.app.resources.status_paused
import io.github.mirhamedjz.ehligible.app.resources.status_unknown
import io.github.mirhamedjz.ehligible.app.ui.AppUiState
import io.github.mirhamedjz.ehligible.app.ui.Links
import io.github.mirhamedjz.ehligible.app.ui.components.MapleLeafProgress
import io.github.mirhamedjz.ehligible.app.ui.funFactOfTheDay
import io.github.mirhamedjz.ehligible.app.ui.oneLiner
import io.github.mirhamedjz.ehligible.app.ui.screenPadding
import io.github.mirhamedjz.ehligible.app.ui.theme.CountdownNumberStyle
import io.github.mirhamedjz.ehligible.core.model.CountdownTarget
import io.github.mirhamedjz.ehligible.core.model.CurrentStatus
import io.github.mirhamedjz.ehligible.core.model.Dashboard
import io.github.mirhamedjz.ehligible.core.rules.CitizenshipRules
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    ready: AppUiState.Ready,
    onOpenTrips: () -> Unit,
    onOpenReview: () -> Unit,
    onOpenChecklist: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val dashboard = ready.dashboard
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(Res.string.app_name), fontWeight = FontWeight.Black) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(Res.string.home_settings))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = screenPadding(padding, horizontal = 20.dp, top = 0.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { StatusChip(dashboard.currentStatus) }
            item { Countdown(dashboard) }
            if (dashboard.reviewDays.isNotEmpty()) {
                item { ReviewBanner(dashboard.reviewDays.size, onOpenReview) }
            }
            item {
                Text(
                    oneLiner(dashboard),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 420.dp),
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    FilledTonalButton(onClick = onOpenTrips, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(Res.string.home_trips))
                    }
                    FilledTonalButton(onClick = onOpenChecklist, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(Res.string.home_checklist))
                    }
                }
            }
            item { FunFactCard(funFactOfTheDay(dashboard.today)) }
            item {
                Text(
                    stringResource(Res.string.disclaimer_short),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun Countdown(dashboard: Dashboard) {
    val uriHandler = LocalUriHandler.current
    val percent = (dashboard.progress * 100).toInt()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        MapleLeafProgress(
            progress = dashboard.progress,
            contentDescription = stringResource(Res.string.leaf_description, percent.toString()),
            modifier = Modifier.size(232.dp),
        )
        Spacer(Modifier.height(8.dp))
        val daysLeft = dashboard.daysLeft
        when {
            dashboard.showsCelebration -> {
                Text("🎉", style = MaterialTheme.typography.displayMedium)
                Text(stringResource(Res.string.home_eligible_title), style = MaterialTheme.typography.headlineMedium)
                Text(
                    stringResource(Res.string.home_eligible_body),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp).widthIn(max = 420.dp),
                )
                Button(onClick = {
                    uriHandler.openUri(Links.IRCC_HOW_TO_APPLY)
                }, modifier = Modifier.padding(top = 12.dp)) {
                    Text(stringResource(Res.string.home_apply_link))
                }
            }

            daysLeft == null -> Text(stringResource(Res.string.home_not_reachable), textAlign = TextAlign.Center)

            else -> {
                Text(formatNumber(daysLeft), style = CountdownNumberStyle, color = MaterialTheme.colorScheme.primary)
                Text(
                    pluralStringResource(Res.plurals.home_days_to_go, daysLeft),
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.height(8.dp))
                CountdownDates(dashboard)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(
                Res.string.home_counted,
                formatNumber(dashboard.creditedDays.coerceAtMost(CitizenshipRules.REQUIRED_DAYS)),
                formatNumber(CitizenshipRules.REQUIRED_DAYS),
            ),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(progress = { dashboard.progress }, modifier = Modifier.fillMaxWidth(0.8f).height(8.dp))
    }
}

@Composable
private fun CountdownDates(dashboard: Dashboard) {
    val exact = dashboard.projection.firstEligibleDate
    val safer = dashboard.projection.saferApplyDate
    if (dashboard.target == CountdownTarget.SAFER) {
        safer?.let {
            Text(
                stringResource(Res.string.home_safer_date, formatDate(it)),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        exact?.let {
            Text(
                stringResource(Res.string.home_exact_date, formatDate(it)),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    } else {
        exact?.let {
            Text(
                stringResource(Res.string.home_apply_from, formatDate(it)),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        if (safer != null && safer != exact) {
            Text(
                stringResource(Res.string.home_safer_date, formatDate(safer)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun StatusChip(status: CurrentStatus) {
    val colors = MaterialTheme.colorScheme
    val (emoji, label, container) = when (status) {
        CurrentStatus.InCanada -> Triple("🇨🇦", stringResource(Res.string.status_in_canada), colors.primaryContainer)

        is CurrentStatus.Abroad -> Triple(
            status.country?.let(::flagEmoji)?.ifBlank { null } ?: "✈️",
            status.country?.let { stringResource(Res.string.status_abroad, countryName(it)) }
                ?: stringResource(Res.string.status_abroad_unknown),
            colors.tertiaryContainer,
        )

        CurrentStatus.NearBorder -> Triple(
            "🧭",
            stringResource(Res.string.status_near_border),
            colors.secondaryContainer,
        )

        CurrentStatus.Unknown -> Triple("📍", stringResource(Res.string.status_unknown), colors.surfaceContainerHigh)
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(color = container, shape = MaterialTheme.shapes.large) {
            Text(
                "$emoji  $label",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        if (status is CurrentStatus.Abroad) {
            Text(
                stringResource(Res.string.status_paused),
                style = MaterialTheme.typography.bodySmall,
                color = colors.tertiary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun ReviewBanner(days: Int, onReview: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Warning, contentDescription = null)
            Spacer(Modifier.size(12.dp))
            Text(pluralStringResource(Res.plurals.home_review_banner, days, days), modifier = Modifier.weight(1f))
            TextButton(onClick = onReview) { Text(stringResource(Res.string.home_review_action)) }
        }
    }
}

@Composable
private fun FunFactCard(fact: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    stringResource(Res.string.home_fun_fact),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(fact, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
