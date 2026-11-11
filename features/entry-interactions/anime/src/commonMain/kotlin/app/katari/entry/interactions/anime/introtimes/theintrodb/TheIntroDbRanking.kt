package app.katari.entry.interactions.anime.introtimes.theintrodb

import app.katari.entry.interactions.anime.introtimes.IntroTimesScope
import app.katari.entry.interactions.anime.introtimes.season
import app.katari.entry.interactions.anime.introtimes.titleWords
import app.katari.entry.interactions.anime.introtimes.yearDistance
import app.katari.entry.interactions.anime.introtimes.yearsBefore

/**
 * The series or film a scope's episodes most likely are among [candidates], or null when none carries [title]. A
 * scope of one episode is looked for among films first, any other among series, as a film and a series often share
 * a name; the year then picks between remakes. A later season's series started before its episodes came out, so it
 * is the one that started closest before them.
 */
internal fun bestMedia(
    candidates: List<TheIntroDbCandidate>,
    scope: IntroTimesScope,
    title: String = scope.title,
): TheIntroDbCandidate? {
    val wanted = titleWords(title)
    if (wanted.isEmpty()) return null
    val wantsFilm = scope.episodeCount <= 1
    val laterSeason = (scope.season ?: 1) > 1
    return candidates.withIndex()
        .filter { (_, candidate) -> candidate.titles.any { titleWords(it) == wanted } }
        .minWithOrNull(
            compareBy<IndexedValue<TheIntroDbCandidate>>(
                { (_, candidate) -> if (candidate.film == wantsFilm) 0 else 1 },
                { (_, candidate) ->
                    if (laterSeason) yearsBefore(candidate.year, scope) else yearDistance(candidate.year, scope)
                },
                // The service's own relevance order.
                { it.index },
            ),
        )
        ?.value
}
