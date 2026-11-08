package app.katari.entry.interactions.anime.introtimes.aniskip

import app.katari.entry.interactions.anime.introtimes.IntroTimesScope
import app.katari.entry.interactions.anime.introtimes.titleWords
import app.katari.entry.interactions.anime.introtimes.yearDistance

/**
 * The series a scope's episodes most likely belong to among AniList's [candidates], or null when none carries its
 * title. The year, the episode count and the format pick between remakes and seasons with the same name.
 */
internal fun bestSeries(candidates: List<AniListSeries>, scope: IntroTimesScope): AniListSeries? {
    val wanted = titleWords(scope.title)
    if (wanted.isEmpty()) return null
    return candidates.withIndex()
        .filter { (_, series) -> series.titles.any { titleWords(it) == wanted } }
        .minWithOrNull(
            compareBy<IndexedValue<AniListSeries>>(
                { (_, series) -> yearDistance(series.year, scope) },
                { (_, series) -> if (series.episodes != null && series.episodes < scope.episodeCount) 1 else 0 },
                { (_, series) -> if (formatFits(series, scope)) 0 else 1 },
                // AniList's own relevance order.
                { it.index },
            ),
        )
        ?.value
}

private fun formatFits(series: AniListSeries, scope: IntroTimesScope): Boolean =
    scope.episodeCount <= 1 || series.format in SeriesFormats

private val SeriesFormats = setOf("TV", "TV_SHORT", "ONA")
