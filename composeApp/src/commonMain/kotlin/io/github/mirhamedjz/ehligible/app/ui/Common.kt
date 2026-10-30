package io.github.mirhamedjz.ehligible.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.mirhamedjz.ehligible.app.platform.countryName
import io.github.mirhamedjz.ehligible.app.platform.flagEmoji
import io.github.mirhamedjz.ehligible.app.platform.formatDate
import io.github.mirhamedjz.ehligible.app.resources.Res
import io.github.mirhamedjz.ehligible.app.resources.action_cancel
import io.github.mirhamedjz.ehligible.app.resources.action_ok
import io.github.mirhamedjz.ehligible.app.resources.action_pick_date
import io.github.mirhamedjz.ehligible.app.resources.fun_facts
import io.github.mirhamedjz.ehligible.app.resources.milestone_DAYS_LEFT_1
import io.github.mirhamedjz.ehligible.app.resources.milestone_DAYS_LEFT_100
import io.github.mirhamedjz.ehligible.app.resources.milestone_DAYS_LEFT_30
import io.github.mirhamedjz.ehligible.app.resources.milestone_DAYS_LEFT_7
import io.github.mirhamedjz.ehligible.app.resources.milestone_ELIGIBLE
import io.github.mirhamedjz.ehligible.app.resources.milestone_HALFWAY
import io.github.mirhamedjz.ehligible.app.resources.milestone_QUARTER
import io.github.mirhamedjz.ehligible.app.resources.milestone_THREE_QUARTERS
import io.github.mirhamedjz.ehligible.app.resources.one_liner_abroad
import io.github.mirhamedjz.ehligible.app.resources.one_liner_eligible
import io.github.mirhamedjz.ehligible.app.resources.one_liner_soon
import io.github.mirhamedjz.ehligible.app.resources.one_liners
import io.github.mirhamedjz.ehligible.core.model.CurrentStatus
import io.github.mirhamedjz.ehligible.core.model.Dashboard
import io.github.mirhamedjz.ehligible.core.model.Milestone
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun milestoneText(milestone: Milestone): String = stringResource(
    when (milestone) {
        Milestone.QUARTER -> Res.string.milestone_QUARTER
        Milestone.HALFWAY -> Res.string.milestone_HALFWAY
        Milestone.THREE_QUARTERS -> Res.string.milestone_THREE_QUARTERS
        Milestone.DAYS_LEFT_100 -> Res.string.milestone_DAYS_LEFT_100
        Milestone.DAYS_LEFT_30 -> Res.string.milestone_DAYS_LEFT_30
        Milestone.DAYS_LEFT_7 -> Res.string.milestone_DAYS_LEFT_7
        Milestone.DAYS_LEFT_1 -> Res.string.milestone_DAYS_LEFT_1
        Milestone.ELIGIBLE -> Res.string.milestone_ELIGIBLE
    },
)

/** Today's playful line: changes daily, reacts to trips and to the final stretch. */
@Composable
fun oneLiner(dashboard: Dashboard): String {
    val lines = stringArrayResource(Res.array.one_liners)
    val soon = stringResource(Res.string.one_liner_soon)
    val abroad = stringResource(Res.string.one_liner_abroad)
    val eligible = stringResource(Res.string.one_liner_eligible)
    val left = dashboard.daysLeft
    val template = when {
        dashboard.isEligibleToday -> eligible
        dashboard.currentStatus is CurrentStatus.Abroad -> abroad
        left == null -> return ""
        left <= 7 -> soon
        else -> lines[dailyIndex(dashboard.today, lines.size, salt = 0)]
    }
    return template.replace("{n}", left?.toString().orEmpty())
}

@Composable
fun funFactOfTheDay(today: LocalDate): String {
    val facts = stringArrayResource(Res.array.fun_facts)
    return facts[dailyIndex(today, facts.size, salt = 11)]
}

private fun dailyIndex(day: LocalDate, size: Int, salt: Int): Int =
    (((day.toEpochDays() + salt) * 7 + 3) % size).toInt().let { if (it < 0) it + size else it }

/** "🇫🇷 France, 🇺🇸 United States" */
fun countriesLabel(codes: Collection<String>): String =
    codes.sorted().joinToString(", ") { "${flagEmoji(it)} ${countryName(it)}".trim() }

fun LocalDate.toPickerMillis(): Long = atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

fun Long.pickerMillisToDate(): LocalDate = Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.UTC).date

/** A read-only field that opens a date picker. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    label: String,
    date: LocalDate?,
    onDateChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    maxDate: LocalDate? = null,
    minDate: LocalDate? = null,
    supportingText: String? = null,
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedTextField(
            value = date?.let { formatDate(it) }.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            placeholder = { Text(stringResource(Res.string.action_pick_date)) },
            trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
            supportingText = supportingText?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
        )
        // The text field is read-only; this overlay makes the whole field open the picker.
        Box(Modifier.matchParentSize().clickable { open = true })
    }
    if (open) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date?.toPickerMillis(),
            // Open on the most useful month: the chosen date, else the earliest allowed (e.g., the departure).
            initialDisplayedMonthMillis = (date ?: minDate ?: maxDate)?.toPickerMillis(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val day = utcTimeMillis.pickerMillisToDate()
                    return (maxDate == null || day <= maxDate) && (minDate == null || day >= minDate)
                }
            },
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onDateChange(it.pickerMillisToDate()) }
                    open = false
                }) { Text(stringResource(Res.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { open = false }) { Text(stringResource(Res.string.action_cancel)) }
            },
        ) { DatePicker(state = state) }
    }
}

@Composable
fun InfoCard(
    text: String,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
) {
    Card(modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = container)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

@Composable
fun Pill(text: String, container: Color, content: Color, modifier: Modifier = Modifier) {
    Surface(color = container, contentColor = content, shape = MaterialTheme.shapes.small, modifier = modifier) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
            Text(text, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** Content padding for a scrolling screen in a Scaffold: the bars' insets from [scaffold] plus some room. */
fun screenPadding(scaffold: PaddingValues, horizontal: Dp = 16.dp, top: Dp = 8.dp, bottom: Dp = 32.dp): PaddingValues =
    PaddingValues(
        start = horizontal,
        end = horizontal,
        top = scaffold.calculateTopPadding() + top,
        bottom = scaffold.calculateBottomPadding() + bottom,
    )
