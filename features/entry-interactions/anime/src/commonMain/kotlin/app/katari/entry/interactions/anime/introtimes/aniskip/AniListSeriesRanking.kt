package app.katari.entry.interactions.anime.introtimes.aniskip

import app.katari.entry.interactions.anime.introtimes.IntroTimesScope
import app.katari.entry.interactions.anime.introtimes.titleWords
import app.katari.entry.interactions.anime.introtimes.yearDistance
import app.katari.entry.interactions.anime.introtimes.yearsBefore

/**
 * The series a scope's episodes most likely belong to among AniList's [candidates], or null when none carries its
 * title. The year, the episode count and the format pick between remakes and seasons with the same name.
 */
internal fun bestSeries(candidates: List<AniListSeries>, scope: IntroTimesScope): AniListSeries? =
    candidates.carrying(scope.title)
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

/**
 * The first season of the show [scope]'s episodes are a later season of, among [candidates]: the series that carries
 * [title], the show's name. Its year and episode count are the first season's, so among remakes it is the one that
 * started closest before the scope's episodes.
 */
internal fun firstSeason(candidates: List<AniListSeries>, title: String, scope: IntroTimesScope): AniListSeries? =
    candidates.carrying(title)
        .minWithOrNull(
            compareBy<IndexedValue<AniListSeries>>(
                { (_, series) -> yearsBefore(series.year, scope) },
                { (_, series) -> if (series.isSeries) 0 else 1 },
                { it.index },
            ),
        )
        ?.value

/**
 * The one of a show's [seasons] that is its [season]-th for [scope]'s source, or null when that can't be told. AniList
 * names most later seasons by number, which settles it. One named otherwise is taken by the year its episodes came
 * out, since a season split in parts, or a film's story retold as a short season, makes counting places unreliable:
 * among those that started within a year of them, the one of that very year, then the one as long as the scope, then
 * the one at the season's place.
 */
internal fun seasonSeries(seasons: List<AniListSeries>, season: Int, scope: IntroTimesScope): AniListSeries? {
    val first = seasons.firstOrNull() ?: return null
    if (season == 1) return first
    val numbered = first.titles.mapTo(HashSet()) { titleWords(it) + season.toString() }
    seasons.drop(1).singleOrNull { later -> later.titles.any { titleWords(it) in numbered } }?.let { return it }
    if (scope.year == null) return null
    val near = seasons.drop(1).filter { yearDistance(it.year, scope) <= 1 }
    return near.singleOrNull()
        ?: near.singleOrNull { it.year == scope.year }
        ?: near.singleOrNull { it.episodes == scope.episodeCount }
        ?: seasons.getOrNull(season - 1)?.takeIf { it in near }
}

/**
 * The number [scope]'s first episode has in a show AniList keeps whole, as its [seasons] say, where a source splits it
 * in seasons and numbers them through; null when the scope is numbered from the show's start or the show has seasons.
 */
internal fun firstEpisodeIn(seasons: List<AniListSeries>, scope: IntroTimesScope): Int? =
    scope.firstNumber?.takeIf { it > 1 && seasons.size == 1 }

private fun List<AniListSeries>.carrying(title: String): List<IndexedValue<AniListSeries>> {
    val wanted = titleWords(title)
    if (wanted.isEmpty()) return emptyList()
    return withIndex().filter { (_, series) -> series.titles.any { titleWords(it) == wanted } }
}

private fun formatFits(series: AniListSeries, scope: IntroTimesScope): Boolean =
    scope.episodeCount <= 1 || series.isSeries
