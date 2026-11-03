package eu.kanade.tachiyomi.ui.stats.recap.period

import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.statistics.recap.StatisticsRecapActivity

/**
 * The part of [activity] this period's recap is about: a window filtered to one type keeps only that type's activity.
 * Apply it before deciding whether there is anything to show, so that decision and the story agree.
 */
fun StatisticsRecapPeriod.scope(activity: StatisticsRecapActivity): StatisticsRecapActivity =
    (this as? StatisticsRecapPeriod.Window)?.type?.let(activity::onlyType) ?: activity

private fun StatisticsRecapActivity.onlyType(type: EntryType): StatisticsRecapActivity {
    val entries = entries.filter { it.type == type }
    val ids = entries.mapTo(HashSet()) { it.id }
    return copy(
        segments = segments.filter { it.entryId in ids },
        entries = entries,
        completions = completions.filter { it.type == type },
        finishedEntryIds = finishedEntryIds.filterTo(HashSet()) { it in ids },
    )
}
