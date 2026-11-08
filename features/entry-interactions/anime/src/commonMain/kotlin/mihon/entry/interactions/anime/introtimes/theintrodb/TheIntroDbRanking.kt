package mihon.entry.interactions.anime.introtimes.theintrodb

import mihon.entry.interactions.anime.introtimes.IntroTimesScope
import mihon.entry.interactions.anime.introtimes.titleWords
import mihon.entry.interactions.anime.introtimes.yearDistance

/**
 * The series or film a scope's episodes most likely are among [candidates], or null when none carries its title. A
 * scope of one episode is looked for among films first, any other among series, as a film and a series often share
 * a name; the year then picks between remakes.
 */
internal fun bestMedia(candidates: List<TheIntroDbCandidate>, scope: IntroTimesScope): TheIntroDbCandidate? {
    val wanted = titleWords(scope.title)
    if (wanted.isEmpty()) return null
    val wantsFilm = scope.episodeCount <= 1
    return candidates.withIndex()
        .filter { (_, candidate) -> candidate.titles.any { titleWords(it) == wanted } }
        .minWithOrNull(
            compareBy<IndexedValue<TheIntroDbCandidate>>(
                { (_, candidate) -> if (candidate.film == wantsFilm) 0 else 1 },
                { (_, candidate) -> yearDistance(candidate.year, scope) },
                // The service's own relevance order.
                { it.index },
            ),
        )
        ?.value
}
