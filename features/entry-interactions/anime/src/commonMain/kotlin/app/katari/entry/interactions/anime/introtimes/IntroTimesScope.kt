package app.katari.entry.interactions.anime.introtimes

import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * The episodes that share one series match. An entry whose episodes fall into several seasons is matched season by
 * season, since each is usually its own series: by the groups its source puts them in, or by the season their names
 * start with, as in "S3 E21", where a source lists a show's seasons together. Otherwise the whole entry is one.
 *
 * @property groupKey what the season's match is kept under: its group's key, or one made of the season its episodes'
 *   names give; null when the entry is matched whole.
 * @property seasonName the group's title, such as "Season 2", when the source groups the entry's seasons.
 * @property listedSeason the season its source lists the episodes under, by their group or their names.
 * @property year the year its earliest episode came out, when the source dates its episodes.
 * @property episodeCount how many whole-numbered episodes it has, which a matching series has room for.
 * @property firstNumber the number its source gives the first of them, and [lastNumber] the last; null when none has
 *   a whole number.
 * @property firstAired the day its source dates that first episode, and [lastAired] the last, when its dates say when
 *   episodes came out.
 */
internal data class IntroTimesScope(
    val entryId: Long,
    val groupKey: String?,
    val title: String,
    val seasonName: String?,
    val listedSeason: Int?,
    val year: Int?,
    val episodeCount: Int,
    val firstNumber: Int?,
    val lastNumber: Int?,
    val firstAired: LocalDate?,
    val lastAired: LocalDate?,
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
    // Seasons named by the episodes count only where the source doesn't group them and names more than one.
    val named = !grouped && entryEpisodes.mapNotNullTo(HashSet()) { episodeNameSeason(it.name) }.size > 1
    val nameSeason = if (named) episodeNameSeason(episode.name) else null
    val seasonName = episode.groupKey?.takeIf { grouped }?.let(groupTitles::get)
    val scoped = when {
        grouped -> entryEpisodes.filter { it.groupKey == episode.groupKey }
        named -> entryEpisodes.filter { episodeNameSeason(it.name) == nameSeason }
        else -> entryEpisodes
    }
    val numbered = scoped.filter { it.wholeNumber != null }
    val first = numbered.minByOrNull { it.chapterNumber }
    val last = numbered.maxByOrNull { it.chapterNumber }
    val dates = releaseDates(scoped)
    return IntroTimesEpisode(
        scope = IntroTimesScope(
            entryId = entry.id,
            groupKey = if (grouped) episode.groupKey else nameSeason?.let { "$NAME_SEASON_KEY$it" },
            title = entry.displayTitle,
            seasonName = seasonName,
            listedSeason = nameSeason ?: seasonNumber(seasonName),
            year = dates.values.minOrNull()?.year,
            episodeCount = numbered.mapNotNullTo(HashSet()) { it.wholeNumber }.size,
            firstNumber = first?.wholeNumber,
            lastNumber = last?.wholeNumber,
            firstAired = first?.let { dates[it.id] },
            lastAired = last?.let { dates[it.id] },
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
    // Estimated dates say nothing of when the series aired.
    val dated = episodes.filter { it.dateUpload > 0L && it.dateText == null }
        .associateWith { Instant.ofEpochMilli(it.dateUpload).atZone(ZoneOffset.UTC).toLocalDate() }
    // Nor do the days a source put most of its episodes up together, long after they aired. An episode listed twice,
    // as subbed and dubbed, shares its own day.
    val numbersByDay = dated.entries.groupBy({ it.value }, { it.key.chapterNumber }).values.map { it.distinct().size }
    val putUpTogether = numbersByDay.filter { it > 1 }.sum()
    if (putUpTogether * 2 > numbersByDay.sum()) return emptyMap()
    return dated.mapKeys { it.key.id }
}

// The key a season named by its episodes is matched under; sources' own group keys are used only where they group.
private const val NAME_SEASON_KEY = "S"
