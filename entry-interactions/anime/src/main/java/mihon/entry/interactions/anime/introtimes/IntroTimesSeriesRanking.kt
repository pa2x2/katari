package mihon.entry.interactions.anime.introtimes

import java.text.Normalizer
import kotlin.math.abs

/**
 * The series a scope's episodes most likely belong to among AniList's [candidates], or null when none carries its
 * title. A wrong match skips the wrong spans, so a title must match one of the series' names word for word; the year,
 * the episode count and the format then pick between remakes and seasons with the same name.
 */
internal fun bestSeries(candidates: List<AniListSeries>, scope: IntroTimesScope): AniListSeries? {
    val wanted = titleWords(scope.title)
    if (wanted.isEmpty()) return null
    return candidates.withIndex()
        .filter { (_, series) -> series.titles.any { titleWords(it) == wanted } }
        .minWithOrNull(
            compareBy<IndexedValue<AniListSeries>>(
                { (_, series) -> yearDistance(series, scope) },
                { (_, series) -> if (series.episodes != null && series.episodes < scope.episodeCount) 1 else 0 },
                { (_, series) -> if (formatFits(series, scope)) 0 else 1 },
                // AniList's own relevance order.
                { it.index },
            ),
        )
        ?.value
}

/**
 * The words of a title, compared as a set so word order doesn't matter: Russian names on AniList often reorder the
 * source's. Season words are dropped and ordinals reduced to their number, so "2nd Season" equals "Season 2", and a
 * year in parentheses is dropped, as AniList adds one to tell remakes apart.
 */
internal fun titleWords(title: String): Set<String> =
    Normalizer.normalize(title.lowercase(), Normalizer.Form.NFKD)
        .replace(CombiningMarks, "")
        .replace('×', 'x')
        .replace(YearInParentheses, " ")
        .split(NonWord)
        .filter(String::isNotEmpty)
        .map { word -> Ordinal.matchEntire(word)?.groupValues?.get(1) ?: word }
        .filterNot { it in SeasonWords }
        .toSet()

private fun yearDistance(series: AniListSeries, scope: IntroTimesScope): Int {
    val wanted = scope.year ?: return 0
    val year = series.year ?: return FAR
    return abs(year - wanted).coerceAtMost(FAR)
}

private fun formatFits(series: AniListSeries, scope: IntroTimesScope): Boolean =
    scope.episodeCount <= 1 || series.format in SeriesFormats

private val CombiningMarks = Regex("\\p{Mn}+")
private val YearInParentheses = Regex("\\((?:19|20)\\d{2}\\)")
private val NonWord = Regex("[^\\p{L}\\p{N}]+")
private val Ordinal = Regex("(\\d+)(?:st|nd|rd|th)")
private val SeasonWords = setOf("season", "сезон", "tv")
private val SeriesFormats = setOf("TV", "TV_SHORT", "ONA")

// A source's dates can be a year off, around New Year or for a late upload; a series further off ranks with those
// AniList has no year for.
private const val FAR = 2
