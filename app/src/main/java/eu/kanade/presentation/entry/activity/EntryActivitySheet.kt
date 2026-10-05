package eu.kanade.presentation.entry.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.components.AdaptiveSheet
import eu.kanade.presentation.more.stats.components.StatisticsInsightTile
import eu.kanade.presentation.more.stats.components.StatisticsRhythmBars
import eu.kanade.presentation.more.stats.components.rememberStatisticsDurationFormatter
import eu.kanade.presentation.util.relativeTimeSpanString
import eu.kanade.tachiyomi.ui.entry.EntryChapterProgress
import eu.kanade.tachiyomi.ui.entry.activity.EntryActivityScreenModel
import tachiyomi.domain.statistics.entry.EntryActivitySummary
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import java.text.NumberFormat
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Everything recorded for one title: time, sessions, progress and pace, when it was started and last read, and its
 * months of activity.
 *
 * @param consumedUnitLabel the type's "Chapters read" wording, or null to leave progress out.
 * @param paceMillis expected time of one unread chapter, shown under progress.
 * @param onOpenActivity lists the title's sessions; null when none were recorded in detail.
 */
@Composable
fun EntryActivitySheet(
    summary: EntryActivitySummary,
    progress: EntryChapterProgress,
    consumedUnitLabel: StringResource?,
    paceMillis: Long?,
    onOpenActivity: (() -> Unit)?,
    onDismissRequest: () -> Unit,
) {
    val formatDuration = rememberStatisticsDurationFormatter()
    val numbers = remember { NumberFormat.getIntegerInstance() }
    val locale = LocalConfiguration.current.locales[0]
    val dateFormatter = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
    val formatDate = { epochMillis: Long ->
        Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)
    }
    val dash = "—"

    AdaptiveSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(MR.strings.entry_activity_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
            TileRow(
                first = {
                    StatisticsInsightTile(
                        value = formatDuration(summary.totalDurationMillis),
                        label = stringResource(MR.strings.statistics_time_spent),
                        detail = summary.untrackedDurationMillis.takeIf { it > 0L }
                            ?.let { stringResource(MR.strings.entry_activity_untracked, formatDuration(it)) },
                        modifier = it,
                    )
                },
                second = {
                    StatisticsInsightTile(
                        value = numbers.format(summary.sessionCount),
                        label = stringResource(MR.strings.statistics_sessions),
                        detail = summary.sessionCount.takeIf { it > 0L }?.let { count ->
                            stringResource(
                                MR.strings.entry_activity_session_average,
                                formatDuration(summary.sessionDurationMillis / count),
                            )
                        },
                        modifier = it,
                    )
                },
            )
            TileRow(
                first = {
                    StatisticsInsightTile(
                        value = consumedUnitLabel?.let {
                            stringResource(
                                MR.strings.entry_activity_progress,
                                progress.totalCount - progress.unreadCount,
                                progress.totalCount,
                            )
                        } ?: dash,
                        label = consumedUnitLabel?.let { label -> stringResource(label) }.orEmpty(),
                        detail = paceMillis?.let {
                            stringResource(MR.strings.entry_activity_chapter_pace, formatDuration(it))
                        },
                        modifier = it,
                    )
                },
                second = {
                    val finishedAt = summary.lastCompletionAtEpochMillis
                        ?.takeIf { progress.totalCount > 0 && progress.unreadCount == 0 }
                    StatisticsInsightTile(
                        value = summary.startedAtEpochMillis?.let(formatDate) ?: dash,
                        label = stringResource(MR.strings.entry_activity_started),
                        detail = finishedAt?.let { stringResource(MR.strings.entry_activity_finished, formatDate(it)) },
                        modifier = it,
                    )
                },
            )
            summary.lastRead?.let { lastRead ->
                Text(
                    text = stringResource(
                        MR.strings.entry_activity_last_read,
                        relativeTimeSpanString(lastRead.atEpochMillis),
                        lastRead.chapterName,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
            if (summary.monthlyDurations.isNotEmpty()) {
                MonthlyActivity(summary = summary, formatDuration = formatDuration)
            }
            if (onOpenActivity != null) {
                TextButton(onClick = onOpenActivity, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(MR.strings.statistics_see_activity))
                }
            }
        }
    }
}

@Composable
private fun TileRow(
    first: @Composable (Modifier) -> Unit,
    second: @Composable (Modifier) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        first(Modifier.weight(1f).fillMaxHeight())
        second(Modifier.weight(1f).fillMaxHeight())
    }
}

@Composable
private fun MonthlyActivity(summary: EntryActivitySummary, formatDuration: (Long) -> String) {
    val locale = LocalConfiguration.current.locales[0]
    val months = remember(summary.monthlyDurations) {
        val byMonth = summary.monthlyDurations.associate { it.month to it.durationMillis }
        val current = YearMonth.now()
        (EntryActivityScreenModel.MONTHS - 1 downTo 0).map { offset ->
            val month = current.minusMonths(offset.toLong())
            month to (byMonth[month.toString()] ?: 0L)
        }
    }
    val letters = remember(months, locale) {
        val formatter = DateTimeFormatter.ofPattern("MMMMM", locale)
        months.map { (month, _) -> month.atDay(1).format(formatter) }
    }
    val monthFormatter = remember(locale) { DateTimeFormatter.ofPattern("MMM yyyy", locale) }
    Column(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(MR.strings.statistics_last_12_months),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        StatisticsRhythmBars(
            values = months.map { it.second },
            labels = letters,
            highlightIndex = months.lastIndex,
            color = MaterialTheme.colorScheme.primary,
            description = months.filter { it.second > 0L }
                .joinToString { (month, duration) -> "${month.format(monthFormatter)}: ${formatDuration(duration)}" },
            barAreaHeight = 48.dp,
        )
    }
}
