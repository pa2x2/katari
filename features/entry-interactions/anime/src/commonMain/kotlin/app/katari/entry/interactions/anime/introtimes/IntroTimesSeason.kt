package app.katari.entry.interactions.anime.introtimes

/** A title split into the series it names and the season it says: "Overlord II" is season 2 of "Overlord". */
internal data class TitleSeason(val series: String, val season: Int)

/** The number a group's title gives its season, as in "Season 2", "2nd Season", "Сезон 2", "2 сезон" or "S2". */
internal fun seasonNumber(name: String?): Int? {
    val match = name?.let(SeasonName::find) ?: return null
    return match.groupValues.drop(1).firstOrNull(String::isNotEmpty)?.toInt()
}

/**
 * The series and season [title] names when it ends in a season, or null when it doesn't. A part of a season, as in
 * "Season 3 Part 2", isn't read as one: where a part starts within its season can't be told from a title.
 */
internal fun titleSeason(title: String): TitleSeason? {
    val text = title.trim().trimEnd(')', ']').trimEnd()
    val match = SeasonSuffix.matchEntire(text)
        ?: RomanSuffix.matchEntire(text)
        ?: NumberSuffix.matchEntire(text)?.takeIf { it.groupValues[2].toInt() in BareSeasons && !endsInCounter(it) }
        ?: return null
    val series = match.groupValues[1].trimEnd { !it.isLetterOrDigit() }
    if (series.none(Char::isLetter)) return null
    val season = match.groupValues.drop(2).first(String::isNotEmpty)
    return TitleSeason(series, RomanSeasons[season] ?: season.toInt())
}

/** The season [this] scope's episodes are, as its group's name or else its title says; null when neither names one. */
internal val IntroTimesScope.season: Int?
    get() = seasonNumber(seasonName) ?: titleSeason(title)?.season

/** The scope's title without the season it names, which is how services that keep a series whole know it. */
internal val IntroTimesScope.seriesTitle: String
    get() = titleSeason(title)?.series ?: title

// "Kaiju No. 8" and "Part 2" end in a number that counts something other than seasons.
private fun endsInCounter(match: MatchResult): Boolean =
    match.groupValues[1].substringAfterLast(' ').trimEnd('.').lowercase() in Counters

private val SeasonName =
    Regex(
        """(?:season|сезон|\bs)\s*(\d{1,4})\b|\b(\d{1,4})(?:st|nd|rd|th|-?й)?\s*(?:season|сезон)""",
        RegexOption.IGNORE_CASE,
    )

private val SeasonSuffix =
    Regex(
        """(.+?)[\s:,(\[–—-]+(?:(\d{1,2})(?:st|nd|rd|th)\s+season|season\s+(\d{1,2})|s(\d{1,2})""" +
            """|сезон\s+(\d{1,2})|(\d{1,2})(?:-?й)?\s+сезон|(?:tv|тв)-?(\d{1,2}))""",
        RegexOption.IGNORE_CASE,
    )

// Upper case only: a lower-case "vi" or "ix" is more often a word.
private val RomanSuffix = Regex("""(.+?)\s+(II|III|IV|VI|VII|VIII|IX)""")
private val RomanSeasons = mapOf("II" to 2, "III" to 3, "IV" to 4, "VI" to 6, "VII" to 7, "VIII" to 8, "IX" to 9)

private val NumberSuffix = Regex("""(.+?)\s+(\d{1,2})""")

// A title ending in a bare number above these, such as "Area 88", is more often a name than a season.
private val BareSeasons = 2..19
private val Counters = setOf("no", "№", "#", "part", "cour", "vol", "volume", "movie", "film", "ova", "часть", "фильм")
