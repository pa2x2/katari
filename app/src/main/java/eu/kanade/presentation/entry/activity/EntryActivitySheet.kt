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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.more.stats.components.StatisticsRhythmBars
import eu.kanade.presentation.more.stats.components.rememberStatisticsDurationFormatter
import eu.kanade.presentation.util.relativeTimeSpanString
import eu.kanade.tachiyomi.ui.entry.EntryChapterProgress
import eu.kanade.tachiyomi.ui.entry.activity.EntryActivityViewModel
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.content.StatisticsMetrics
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.sheet.Sheet
import mihon.presentation.kit.sheet.SheetHeader
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
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
    val locale = LocalConfiguration.current.locales[0]
    val dateFormatter = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
    val formatDate = { epochMillis: Long ->
        Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)
    }

    Sheet(onDismissRequest = onDismissRequest) {
        Column {
            SheetHeader(title = stringResource(MR.strings.entry_activity_title))
            EntryActivityDetails(
                summary = summary,
                progress = progress,
                consumedUnitLabel = consumedUnitLabel,
                paceMillis = paceMillis,
                onOpenActivity = onOpenActivity,
                formatDuration = formatDuration,
                formatDate = formatDate,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
    }
}

@Composable
private fun EntryActivityDetails(
    summary: EntryActivitySummary,
    progress: EntryChapterProgress,
    consumedUnitLabel: StringResource?,
    paceMillis: Long?,
    onOpenActivity: (() -> Unit)?,
    formatDuration: (Long) -> String,
    formatDate: (Long) -> String,
    modifier: Modifier = Modifier,
) {
    val numbers = remember { NumberFormat.getIntegerInstance() }
    val dash = "—"
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(all = KatariTheme.spacing.m),
        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
    ) {
        TileRow(
            first = {
                EntryActivityTile(
                    value = formatDuration(summary.totalDurationMillis),
                    label = stringResource(MR.strings.statistics_time_spent),
                    detail = summary.untrackedDurationMillis.takeIf { it > 0L }
                        ?.let { stringResource(MR.strings.entry_activity_untracked, formatDuration(it)) },
                    modifier = it,
                )
            },
            second = {
                EntryActivityTile(
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
                EntryActivityTile(
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
                EntryActivityTile(
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
                    MR.strings.entry_activity_last_opened,
                    relativeTimeSpanString(lastRead.atEpochMillis),
                    lastRead.chapterName,
                ),
                role = TextRole.Body,
                tone = ContentTone.Secondary,
                modifier = Modifier.padding(horizontal = KatariTheme.spacing.m, vertical = KatariTheme.spacing.xs),
            )
        }
        if (summary.monthlyDurations.isNotEmpty()) {
            MonthlyActivity(summary = summary, formatDuration = formatDuration)
        }
        if (onOpenActivity != null) {
            Button(
                label = stringResource(MR.strings.statistics_see_activity),
                onClick = onOpenActivity,
                modifier = Modifier.align(Alignment.End),
                style = ButtonStyle.Text,
            )
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
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
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
        (EntryActivityViewModel.MONTHS - 1 downTo 0).map { offset ->
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
        modifier = Modifier.padding(horizontal = KatariTheme.spacing.m, vertical = KatariTheme.spacing.xs),
        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
    ) {
        Text(
            text = stringResource(MR.strings.statistics_last_12_months),
            role = TextRole.Supporting,
        )
        StatisticsRhythmBars(
            values = months.map { it.second },
            labels = letters,
            highlightIndex = months.lastIndex,
            color = KatariTheme.chart.series,
            description = months.filter { it.second > 0L }
                .joinToString { (month, duration) -> "${month.format(monthFormatter)}: ${formatDuration(duration)}" },
            barAreaHeight = StatisticsMetrics.rhythmCompactHeight,
        )
    }
}
