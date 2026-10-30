package io.github.mirhamedjz.ehligible.app.ui.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.mirhamedjz.ehligible.app.platform.DateStyle
import io.github.mirhamedjz.ehligible.app.platform.formatDate
import io.github.mirhamedjz.ehligible.app.resources.Res
import io.github.mirhamedjz.ehligible.app.resources.action_back
import io.github.mirhamedjz.ehligible.app.resources.review_all_away
import io.github.mirhamedjz.ehligible.app.resources.review_away
import io.github.mirhamedjz.ehligible.app.resources.review_body
import io.github.mirhamedjz.ehligible.app.resources.review_done
import io.github.mirhamedjz.ehligible.app.resources.review_in_canada
import io.github.mirhamedjz.ehligible.app.resources.review_title
import io.github.mirhamedjz.ehligible.app.ui.AppUiState
import io.github.mirhamedjz.ehligible.app.ui.AppViewModel
import io.github.mirhamedjz.ehligible.app.ui.InfoCard
import io.github.mirhamedjz.ehligible.app.ui.screenPadding
import org.jetbrains.compose.resources.stringResource

@Composable
fun ReviewScreen(viewModel: AppViewModel, ready: AppUiState.Ready, onBack: () -> Unit) {
    val days = ready.dashboard.reviewDays
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.review_title)) },
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
            item { InfoCard(stringResource(Res.string.review_body)) }
            if (days.isEmpty()) {
                item {
                    Text(
                        "✨ " + stringResource(Res.string.review_done),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(vertical = 32.dp),
                    )
                }
            } else {
                if (days.size > 1) {
                    item {
                        OutlinedButton(
                            onClick = { viewModel.confirmDays(days, inCanada = false) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(Res.string.review_all_away))
                        }
                    }
                }
                items(days, key = { it.toString() }) { day ->
                    val confirm = { inCanada: Boolean -> viewModel.confirmDays(listOf(day), inCanada) }
                    val cardColors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    )
                    Card(colors = cardColors) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                formatDate(day, DateStyle.LONG),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f),
                            )
                            OutlinedButton(onClick = { confirm(true) }) {
                                Text(stringResource(Res.string.review_in_canada))
                            }
                            Spacer(Modifier.width(8.dp))
                            FilledTonalButton(onClick = { confirm(false) }) {
                                Text(stringResource(Res.string.review_away))
                            }
                        }
                    }
                }
            }
        }
    }
}
