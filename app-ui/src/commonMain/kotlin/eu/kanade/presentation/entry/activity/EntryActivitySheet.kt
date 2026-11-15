package eu.kanade.presentation.entry.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import app.katari.presentation.kit.button.Button
import app.katari.presentation.kit.button.ButtonStyle
import app.katari.presentation.kit.content.FactLines
import app.katari.presentation.kit.content.Figure
import app.katari.presentation.kit.content.LeadFigure
import app.katari.presentation.kit.content.StatisticsMetrics
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.sheet.Sheet
import app.katari.presentation.kit.sheet.SheetHeader
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.more.stats.components.StatisticsRhythmBars
import eu.kanade.presentation.more.stats.components.StatisticsShareBar
import eu.kanade.presentation.more.stats.components.rememberStatisticsDurationFormatter
import eu.kanade.presentation.util.relativeTimeSpanString
import eu.kanade.tachiyomi.ui.entry.activity.EntryActivityViewModel
import eu.kanade.tachiyomi.ui.entry.chapters.EntryChapterProgress
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
 * Everything recorded for one title, led by its time with when it was started and finished, then its progress, its
 * months of activity and the remaining facts as plain lines.
 *
 * @param consumedUnitLabel the type's "Chapters read" wording, or null to leave progress out.
 * @param paceMillis expected time of one unread chapter.
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
    val locale = LocalLocale.current.platformLocale
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
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = KatariTheme.spacing.rowInset, vertical = KatariTheme.spacing.m),
        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.l),
    ) {
        LeadFigure(
            value = formatDuration(summary.totalDurationMillis),
            detail = summary.startedAtEpochMillis?.let { startedAt ->
                {
                    Text(
                        text = startedAndFinished(summary, progress, startedAt, formatDate),
                        role = TextRole.Supporting,
                        tone = ContentTone.Secondary,
                    )
                }
            },
        )
        if (consumedUnitLabel != null && progress.totalCount > 0) {
            ConsumedProgress(progress, consumedUnitLabel)
        }
        if (summary.monthlyDurations.isNotEmpty()) {
            MonthlyActivity(summary = summary, formatDuration = formatDuration)
        }
        val facts = activityFacts(summary, paceMillis, formatDuration)
        if (facts.isNotEmpty()) {
            FactLines(facts)
        }
        if (onOpenActivity != null) {
            Button(
                label = stringResource(MR.strings.statistics_see_activity),
                onClick = onOpenActivity,
                modifier = Modifier.fillMaxWidth(),
                style = ButtonStyle.Tonal,
            )
        }
    }
}

@Composable
private fun startedAndFinished(
    summary: EntryActivitySummary,
    progress: EntryChapterProgress,
    startedAt: Long,
    formatDate: (Long) -> String,
): String {
    val finishedAt = summary.lastCompletionAtEpochMillis
        ?.takeIf { progress.totalCount > 0 && progress.unreadCount == 0 }
    return if (finishedAt != null) {
        stringResource(MR.strings.entry_activity_started_and_finished, formatDate(startedAt), formatDate(finishedAt))
    } else {
        stringResource(MR.strings.entry_activity_started, formatDate(startedAt))
    }
}

@Composable
private fun ConsumedProgress(progress: EntryChapterProgress, consumedUnitLabel: StringResource) {
    val consumed = progress.totalCount - progress.unreadCount
    Column(verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s)) {
        StatisticsShareBar(
            fraction = consumed.toFloat() / progress.totalCount,
            color = KatariTheme.chart.series,
            height = StatisticsMetrics.shareBarThick,
        )
        FactLines(
            listOf(
                Figure(
                    value = stringResource(MR.strings.entry_activity_progress, consumed, progress.totalCount),
                    label = stringResource(consumedUnitLabel),
                ),
            ),
        )
    }
}

@Composable
private fun activityFacts(
    summary: EntryActivitySummary,
    paceMillis: Long?,
    formatDuration: (Long) -> String,
): List<Figure> {
    val numbers = remember { NumberFormat.getIntegerInstance() }
    return listOfNotNull(
        summary.lastRead?.let { lastRead ->
            Figure(
                value = stringResource(
                    MR.strings.entry_activity_last_opened_detail,
                    lastRead.chapterName,
                    relativeTimeSpanString(lastRead.atEpochMillis),
                ),
                label = stringResource(MR.strings.entry_activity_last_opened),
            )
        },
        summary.sessionCount.takeIf { it > 0L }?.let { count ->
            Figure(
                value = if (count == 1L) {
                    numbers.format(count)
                } else {
                    stringResource(
                        MR.strings.entry_activity_session_average,
                        numbers.format(count),
                        formatDuration(summary.sessionDurationMillis / count),
                    )
                },
                label = stringResource(MR.strings.statistics_sessions),
            )
        },
        paceMillis?.let {
            Figure(
                value = stringResource(MR.strings.entry_activity_chapter_pace, formatDuration(it)),
                label = stringResource(MR.strings.entry_activity_pace),
            )
        },
        summary.untrackedDurationMillis.takeIf { it > 0L }?.let {
            Figure(value = formatDuration(it), label = stringResource(MR.strings.entry_activity_before_tracking))
        },
    )
}

@Composable
private fun MonthlyActivity(summary: EntryActivitySummary, formatDuration: (Long) -> String) {
    val locale = LocalLocale.current.platformLocale
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
