package mihon.entry.interactions.anime.introtimes

import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import java.time.Instant
import java.time.ZoneOffset

/**
 * The episodes that share one series match. An entry whose episodes fall into several groups, such as seasons, is
 * matched group by group ([groupKey]), since each season is usually its own series; otherwise the whole entry is one.
 *
 * @property year the year its earliest episode came out, when the source dates its episodes.
 * @property episodeCount how many whole-numbered episodes it has, which a matching series has room for.
 */
internal data class IntroTimesScope(
    val entryId: Long,
    val groupKey: String?,
    val title: String,
    val year: Int?,
    val episodeCount: Int,
)

/** [number] is the episode's number in the scope's series, or null when it has none AniSkip could know it by. */
internal data class IntroTimesEpisode(
    val scope: IntroTimesScope,
    val number: Int?,
)

/** [entryEpisodes] are all the episodes of [entry], the episode's own entry rather than a merged title holding it. */
internal fun introTimesEpisode(
    entry: Entry,
    episode: EntryChapter,
    entryEpisodes: List<EntryChapter>,
): IntroTimesEpisode {
    val grouped = entryEpisodes.mapNotNullTo(HashSet()) { it.groupKey }.size > 1
    val groupKey = episode.groupKey.takeIf { grouped }
    val scoped = if (grouped) entryEpisodes.filter { it.groupKey == groupKey } else entryEpisodes
    val numbers = scoped.mapNotNull { it.wholeNumber }
    // A season listed as 13 to 24 is episodes 1 to 12 of its own series.
    val first = numbers.minOrNull()
    return IntroTimesEpisode(
        scope = IntroTimesScope(
            entryId = entry.id,
            groupKey = groupKey,
            title = entry.displayTitle,
            year = earliestYear(scoped),
            episodeCount = numbers.distinct().size,
        ),
        number = episode.wholeNumber?.let { number -> first?.let { number - it + 1 } },
    )
}

// Episode 0, a half episode or an unrecognized number has no number of its own on AniSkip.
private val EntryChapter.wholeNumber: Int?
    get() = chapterNumber.takeIf { it >= 1.0 && it % 1.0 == 0.0 }?.toInt()

private fun earliestYear(episodes: List<EntryChapter>): Int? {
    // Estimated dates and ones a source stamped on every episode alike say nothing of when the series aired.
    val dates = episodes.filter { it.dateUpload > 0L && it.dateText == null }.map { it.dateUpload }
    if (dates.size > 1 && dates.distinct().size == 1) return null
    return dates.minOrNull()?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).year }
}
