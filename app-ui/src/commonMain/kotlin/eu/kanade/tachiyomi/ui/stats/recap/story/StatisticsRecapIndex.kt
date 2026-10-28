package eu.kanade.tachiyomi.ui.stats.recap.story

import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.stats.recap.period.StatisticsRecapPeriod
import mihon.entry.interactions.statistics.EntryStatisticsContribution
import tachiyomi.domain.statistics.model.StatisticsActivityBucket
import tachiyomi.domain.statistics.model.StatisticsActivitySegment
import tachiyomi.domain.statistics.model.StatisticsActivityTimeline
import tachiyomi.domain.statistics.model.StatisticsCompletionBucket
import tachiyomi.domain.statistics.recap.StatisticsRecapActivity
import tachiyomi.domain.statistics.recap.StatisticsRecapSegment
import java.time.LocalDate
import java.time.YearMonth

/**
 * A period's activity indexed for the page builders.
 *
 * Hidden titles still count toward every total; they're only left out wherever a page names or shows a title, or
 * tells what it is, as genres do.
 *
 * @param hiddenEntryIds titles the user hid from this recap, or that come from 18+ extensions.
 * @param contributions the Statistics types, in the order their counts are listed.
 */
internal class StatisticsRecapIndex(
    val period: StatisticsRecapPeriod,
    val activity: StatisticsRecapActivity,
    private val hiddenEntryIds: Set<Long>,
    private val contributions: List<EntryStatisticsContribution>,
) {
    private val entries = activity.entries.associateBy { it.id }

    val segments: List<StatisticsRecapSegment> = activity.segments.filter { it.entryId in entries }

    val totalMillis: Long = segments.sumOf { it.durationMillis }

    private val durationByEntry: Map<Long, Long> = segments.groupingBy { it.entryId }
        .fold(0L) { total, segment -> total + segment.durationMillis }

    /** Every timed title, the most time first. */
    val titles: List<StatisticsRecapTitle> = durationByEntry.keys.map { title(it, durationByEntry.getValue(it)) }
        .sortedWith(compareByDescending<StatisticsRecapTitle> { it.durationMillis }.thenBy { it.title.lowercase() })

    val shownTitles: List<StatisticsRecapTitle> = titles.filter { isShown(it.entryId) }

    val segmentsByDay: Map<LocalDate, List<StatisticsRecapSegment>> = segments.groupBy { LocalDate.parse(it.localDate) }

    val durationByDay: Map<LocalDate, Long> = segmentsByDay.mapValues { (_, day) -> day.sumOf { it.durationMillis } }
        .filterValues { it > 0L }

    /** Days with any time or anything finished, as the dashboard counts them; every streak day is one. */
    val activeDates: Set<LocalDate> = durationByDay.keys + activity.completions
        .filter { it.count > 0L }
        .map { LocalDate.parse(it.localDate) }

    val durationByMonth: Map<YearMonth, Long> = durationByDay.entries
        .groupingBy { YearMonth.from(it.key) }
        .fold(0L) { total, day -> total + day.value }

    fun isShown(entryId: Long): Boolean = entryId !in hiddenEntryIds

    fun type(entryId: Long): EntryType = entries.getValue(entryId).type

    fun genres(entryId: Long): List<String> = entries[entryId]?.genres.orEmpty()

    /** [entryId] as a recap title, with [durationMillis] as its time; defaults to its time in the whole period. */
    fun title(entryId: Long, durationMillis: Long = durationByEntry[entryId] ?: 0L): StatisticsRecapTitle {
        val entry = entries.getValue(entryId)
        return StatisticsRecapTitle(entry.id, entry.type, entry.title, entry.cover, durationMillis)
    }

    /** The shown titles of [segments], the most time first. */
    fun shownTitlesOf(segments: List<StatisticsRecapSegment>): List<StatisticsRecapTitle> = segments
        .filter { isShown(it.entryId) }
        .groupingBy { it.entryId }
        .fold(0L) { total, segment -> total + segment.durationMillis }
        .map { (entryId, duration) -> title(entryId, duration) }
        .sortedWith(compareByDescending<StatisticsRecapTitle> { it.durationMillis }.thenBy { it.title.lowercase() })

    fun contribution(type: EntryType): EntryStatisticsContribution? = contributions.firstOrNull { it.type == type }

    /** Items finished by [entryIds], or by every title when null, grouped by how the types word them. */
    fun consumedCounts(entryIds: Set<Long>? = null): List<StatisticsRecapConsumedCount> {
        val completions = activity.completions.filter { entryIds == null || it.entryId in entryIds }
        return contributions
            .groupBy(EntryStatisticsContribution::consumedCountPlural)
            .mapNotNull { (plural, grouped) ->
                val types = grouped.map(EntryStatisticsContribution::type).toSet()
                completions.filter { it.type in types }.sumOf { it.count }
                    .takeIf { it > 0L }
                    ?.let { StatisticsRecapConsumedCount(plural, grouped.first().consumedUnitLabel, it) }
            }
    }

    /** The period's activity in the shape the dashboard's streak and rhythm calculations take. */
    val timeline: StatisticsActivityTimeline by lazy {
        StatisticsActivityTimeline(
            activity = segments.groupBy { type(it.entryId) to it.localDate }
                .map { (key, rows) ->
                    StatisticsActivityBucket(key.first, key.second, rows.sumOf { it.durationMillis })
                },
            completions = activity.completions.map { StatisticsCompletionBucket(it.type, it.localDate, it.count) },
        )
    }

    val activitySegments: List<StatisticsActivitySegment> by lazy {
        segments.map {
            StatisticsActivitySegment(
                type = type(it.entryId),
                localDate = it.localDate,
                startedAtEpochMillis = it.startedAtEpochMillis,
                endedAtEpochMillis = it.endedAtEpochMillis,
                durationMillis = it.durationMillis,
                timeZoneId = it.timeZoneId,
            )
        }
    }
}
