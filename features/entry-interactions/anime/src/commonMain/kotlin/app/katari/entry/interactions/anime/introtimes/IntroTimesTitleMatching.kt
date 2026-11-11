package app.katari.entry.interactions.anime.introtimes

import java.text.Normalizer
import kotlin.math.abs

/**
 * The words of a title, compared as a set so word order doesn't matter: Russian names on AniList and Wikidata often
 * reorder the source's. Season words are dropped and ordinals and Roman numerals reduced to their number, so
 * "2nd Season" equals "Season 2" and "II", and a year in parentheses is dropped, as services add one to tell remakes
 * apart.
 *
 * A wrong match skips the wrong spans, so a service's series must carry the scope's title word for word.
 */
internal fun titleWords(title: String): Set<String> =
    Normalizer.normalize(title.lowercase(), Normalizer.Form.NFKD)
        .replace(CombiningMarks, "")
        .replace('×', 'x')
        .replace(YearInParentheses, " ")
        .split(NonWord)
        .filter(String::isNotEmpty)
        .map { word -> Ordinal.matchEntire(word)?.groupValues?.get(1) ?: RomanNumerals[word] ?: word }
        .filterNot { it in SeasonWords }
        .toSet()

/**
 * How far a series' [year] is from the year [scope] came out, for ranking remakes and seasons with the same name; a
 * series without a year ranks with those too far off to tell apart.
 */
internal fun yearDistance(year: Int?, scope: IntroTimesScope): Int {
    val wanted = scope.year ?: return 0
    year ?: return FAR
    return abs(year - wanted).coerceAtMost(FAR)
}

/**
 * How many years before [scope]'s episodes a series started in [year], for ranking the series a later season belongs
 * to: its own year is that of the first season. One that started after them, or has no year, ranks last.
 */
internal fun yearsBefore(year: Int?, scope: IntroTimesScope): Int {
    val wanted = scope.year ?: return 0
    return year?.takeIf { it <= wanted }?.let { wanted - it } ?: Int.MAX_VALUE
}

private val CombiningMarks = Regex("\\p{Mn}+")
private val YearInParentheses = Regex("\\((?:19|20)\\d{2}\\)")
private val NonWord = Regex("[^\\p{L}\\p{N}]+")
private val Ordinal = Regex("(\\d+)(?:st|nd|rd|th)")
private val SeasonWords = setOf("season", "сезон", "tv")

// "i", "v" and "x" are left as words: alone they are seldom a number.
private val RomanNumerals =
    mapOf("ii" to "2", "iii" to "3", "iv" to "4", "vi" to "6", "vii" to "7", "viii" to "8", "ix" to "9")

// A source's dates can be a year off, around New Year or for a late upload; a series further off ranks with those
// that have no year.
private const val FAR = 2
