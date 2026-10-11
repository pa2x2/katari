package eu.kanade.presentation.more.stats.components

import android.icu.text.ListFormatter
import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import eu.kanade.presentation.more.stats.data.StatsActivity
import eu.kanade.presentation.more.stats.data.StatsType
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import java.text.NumberFormat
import java.time.LocalDate
import java.time.ZoneId

/**
 * Names a count of finished items by the units it adds up, such as "Chapters" or "Chapters and episodes". Only nouns
 * are used so the label stays one line in a summary tile whichever units are counted. Types with nothing finished don't
 * decide the label unless no type has any.
 */
@Composable
internal fun statisticsCompletionLabel(types: List<StatsType>, completionCountByType: Map<EntryType, Long>): String {
    val units = types.filter { (completionCountByType[it.type] ?: 0L) > 0L }
        .ifEmpty { types }
        .map { stringResource(it.consumedUnitName) }
        .distinct()
    val locale = LocalConfiguration.current.locales[0]
    return ListFormatter.getInstance(locale).format(units).replaceFirstChar { it.titlecase(locale) }
}

/**
 * Context under the finished-items count of [types] in [activity]: each type's share when several finished something,
 * the average time per item when one did, and when an item was last finished when none did. Null when there's nothing
 * to say, such as before anything was ever finished.
 */
@Composable
internal fun statisticsCompletionDetail(
    activity: StatsActivity,
    types: List<StatsType>,
    formatDuration: (Long) -> String,
): String? {
    val formatSeconds = rememberStatisticsDurationFormatter(StatisticsDurationPrecision.SECONDS)
    val counted = types.mapNotNull { type ->
        activity.completionCountByType[type.type]?.takeIf { it > 0L }?.let { type to it }
    }
    return when (counted.size) {
        0 -> types.mapNotNull { activity.lastCompletionDateByType[it.type] }.maxOrNull()?.let { date ->
            stringResource(MR.strings.statistics_last_finished, formatCompletionDate(date))
        }
        1 -> {
            val (type, count) = counted.single()
            val perItemMillis = (activity.totalDurationByType[type.type] ?: 0L) / count
            perItemMillis.takeIf { it > 0L }?.let {
                // Minute precision would turn quick items into "<1 min", which can't follow the label's "~".
                val time = if (it < MINUTE_MILLIS) formatSeconds(it) else formatDuration(it)
                stringResource(type.timePerItemLabel, time)
            }
        }
        else -> {
            val numbers = NumberFormat.getIntegerInstance()
            counted.map { (type, count) -> "${stringResource(type.displayName)} ${numbers.format(count)}" }
                .joinToString(" · ")
        }
    }
}

/** A day such as "Sep 28"; the year is kept only when it isn't the current one. */
@Composable
private fun formatCompletionDate(date: LocalDate): String {
    val millis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    var flags = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH
    if (date.year == LocalDate.now().year) flags = flags or DateUtils.FORMAT_NO_YEAR
    return DateUtils.formatDateTime(LocalContext.current, millis, flags)
}

private const val MINUTE_MILLIS = 60_000L
