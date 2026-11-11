package app.katari.entry.interactions.anime.introtimes

import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * The episodes that share one series match. An entry whose episodes fall into several groups, such as seasons, is
 * matched group by group ([groupKey]), since each season is usually its own series; otherwise the whole entry is one.
 *
 * @property seasonName the group's title, such as "Season 2", when the entry is matched group by group.
 * @property year the year its earliest episode came out, when the source dates its episodes.
 * @property episodeCount how many whole-numbered episodes it has, which a matching series has room for.
 * @property firstNumber the number its source gives the first of them; null when none has a whole number.
 * @property firstAired the day its source dates that first episode, when its dates say when episodes came out.
 */
internal data class IntroTimesScope(
    val entryId: Long,
    val groupKey: String?,
    val title: String,
    val seasonName: String?,
    val year: Int?,
    val episodeCount: Int,
    val firstNumber: Int?,
    val firstAired: LocalDate?,
)

/**
 * [number] is the episode's place in its scope, counted from 1 at [IntroTimesScope.firstNumber], or null when it has
 * no whole number to count by. [name] is what its source calls it.
 */
internal data class IntroTimesEpisode(
    val scope: IntroTimesScope,
    val number: Int?,
    val name: String,
)

/**
 * [entryEpisodes] are all the episodes of [entry], the episode's own entry rather than a merged title holding it, and
 * [groupTitles] its groups' titles by key.
 */
internal fun introTimesEpisode(
    entry: Entry,
    episode: EntryChapter,
    entryEpisodes: List<EntryChapter>,
    groupTitles: Map<String, String>,
): IntroTimesEpisode {
    val grouped = entryEpisodes.mapNotNullTo(HashSet()) { it.groupKey }.size > 1
    val groupKey = episode.groupKey.takeIf { grouped }
    val scoped = if (grouped) entryEpisodes.filter { it.groupKey == groupKey } else entryEpisodes
    val numbered = scoped.filter { it.wholeNumber != null }
    val first = numbered.minByOrNull { it.chapterNumber }
    val dates = releaseDates(scoped)
    return IntroTimesEpisode(
        scope = IntroTimesScope(
            entryId = entry.id,
            groupKey = groupKey,
            title = entry.displayTitle,
            seasonName = groupKey?.let(groupTitles::get),
            year = dates.values.minOrNull()?.year,
            episodeCount = numbered.mapNotNullTo(HashSet()) { it.wholeNumber }.size,
            firstNumber = first?.wholeNumber,
            firstAired = first?.let { dates[it.id] },
        ),
        // A season listed as 13 to 24 is episodes 1 to 12 of its own series.
        number = episode.wholeNumber?.let { number -> first?.wholeNumber?.let { number - it + 1 } },
        name = episode.name,
    )
}

// Episode 0, a half episode or an unrecognized number has no number of its own on AniSkip or TheIntroDB.
private val EntryChapter.wholeNumber: Int?
    get() = chapterNumber.takeIf { it >= 1.0 && it % 1.0 == 0.0 }?.toInt()

/** The days [episodes] came out, by episode id, when their source dates them by release. */
private fun releaseDates(episodes: List<EntryChapter>): Map<Long, LocalDate> {
    // Estimated dates and ones a source stamped on every episode alike say nothing of when the series aired.
    val dated = episodes.filter { it.dateUpload > 0L && it.dateText == null }
    if (dated.size > 1 && dated.distinctBy { it.dateUpload }.size == 1) return emptyMap()
    return dated.associate { it.id to Instant.ofEpochMilli(it.dateUpload).atZone(ZoneOffset.UTC).toLocalDate() }
}
