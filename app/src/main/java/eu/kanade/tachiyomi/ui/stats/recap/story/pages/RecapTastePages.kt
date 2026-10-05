package eu.kanade.tachiyomi.ui.stats.recap.story.pages

import eu.kanade.tachiyomi.ui.stats.recap.story.GenreShare
import eu.kanade.tachiyomi.ui.stats.recap.story.StatisticsRecapIndex
import eu.kanade.tachiyomi.ui.stats.recap.story.StatisticsRecapPage
import eu.kanade.tachiyomi.ui.stats.recap.story.TypeShare
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToInt

/** Shown only when the time really was split: two or more types each with a tenth of it. */
internal fun StatisticsRecapIndex.typesPage(): StatisticsRecapPage.Types? {
    if (totalMillis <= 0L) return null
    val shares = segments.groupingBy { type(it.entryId) }
        .fold(0L) { total, segment -> total + segment.durationMillis }
        .map { (type, duration) -> TypeShare(type, duration, (duration * 100.0 / totalMillis).roundToInt()) }
        .sortedByDescending { it.durationMillis }
    if (shares.count { it.durationMillis * 10 >= totalMillis } < 2) return null
    val monthLeaders = segments
        .groupBy { YearMonth.from(LocalDate.parse(it.localDate)) }
        .mapValues { (_, month) ->
            month.groupingBy { type(it.entryId) }
                .fold(0L) { total, segment -> total + segment.durationMillis }
                .maxBy { it.value }.key
        }
    val ledMonths = shares.drop(1).firstNotNullOfOrNull { share ->
        val months = monthLeaders.filterValues { it == share.type }.keys.sorted()
        months.takeIf { it.isNotEmpty() }?.let { share.type to it }
    }
    return StatisticsRecapPage.Types(shares, ledMonths)
}

/**
 * Genres weighted by the time spent in titles carrying them. Sources spell genres differently, so they're matched
 * ignoring case, spaces and punctuation ("Sci-Fi", "sci fi"), and shown as the most-read title spells them.
 */
internal fun StatisticsRecapIndex.genresPage(): StatisticsRecapPage.Genres? {
    if (totalMillis <= 0L) return null
    val names = mutableMapOf<String, String>()
    val durations = mutableMapOf<String, Long>()
    titles.forEach { title ->
        genres(title.entryId).map { it.trim() }.filter { it.isNotEmpty() }.distinctBy(::genreKey).forEach { genre ->
            val key = genreKey(genre)
            names.getOrPut(key) { genre.replaceFirstChar { it.titlecase() } }
            durations[key] = (durations[key] ?: 0L) + title.durationMillis
        }
    }
    val ranked = durations.entries.sortedByDescending { it.value }.take(GENRES_SHOWN)
    val topKey = ranked.firstOrNull()?.key ?: return null
    val topTitles = titles.take(TOP_TITLES_FOR_GENRE)
    val withTopGenre = topTitles.count { title -> genres(title.entryId).any { genreKey(it) == topKey } }
    if (withTopGenre < MIN_TITLES_WITH_TOP_GENRE) return null
    return StatisticsRecapPage.Genres(
        genres = ranked.map { GenreShare(names.getValue(it.key), (it.value * 100.0 / totalMillis).roundToInt()) },
        topGenreTitles = withTopGenre,
        topTitleCount = topTitles.size,
    )
}

private fun genreKey(genre: String): String = genre.lowercase().filter(Char::isLetterOrDigit)

private const val GENRES_SHOWN = 5
private const val TOP_TITLES_FOR_GENRE = 10
private const val MIN_TITLES_WITH_TOP_GENRE = 3
