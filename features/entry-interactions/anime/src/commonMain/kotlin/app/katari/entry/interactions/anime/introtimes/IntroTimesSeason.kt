package app.katari.entry.interactions.anime.introtimes

/**
 * A title split into the series it names and the season it says: "Overlord II" is season 2 of "Overlord". [part] is
 * the part of that season it names, as in "Season 3 Part 2"; null when it names the whole season.
 */
internal data class TitleSeason(val series: String, val season: Int, val part: Int? = null)

/** The number a group's title gives its season, as in "Season 2", "2nd Season", "Сезон 2", "2 сезон" or "S2". */
internal fun seasonNumber(name: String?): Int? {
    val match = name?.let(SeasonName::find) ?: return null
    return match.groupValues.drop(1).firstOrNull(String::isNotEmpty)?.toInt()
}

/**
 * The season the name of an episode starts with, as in "S3 E21 - The Attack Titan" or "Season 3 Episode 21", which is
 * how a source that lists a show's seasons together tells them apart; null when it names none.
 */
internal fun episodeNameSeason(name: String): Int? =
    EpisodeNameSeason.find(name)?.groupValues?.get(1)?.toInt()

/**
 * The series and season [title] names when it ends in a season, or in a part of one, as in "Season 3 Part 2"; null
 * when it names neither.
 */
internal fun titleSeason(title: String): TitleSeason? {
    val text = title.trim().trimEnd(')', ']').trimEnd()
    PartSuffix.matchEntire(text)?.let { part ->
        return wholeSeason(part.groupValues[1])?.copy(part = part.groupValues[2].toInt())
    }
    return wholeSeason(text)
}

private fun wholeSeason(text: String): TitleSeason? {
    val match = SeasonSuffix.matchEntire(text)
        ?: RomanSuffix.matchEntire(text)
        ?: NumberSuffix.matchEntire(text)?.takeIf { it.groupValues[2].toInt() in BareSeasons && !endsInCounter(it) }
        ?: return null
    val series = match.groupValues[1].trimEnd { !it.isLetterOrDigit() }
    if (series.none(Char::isLetter)) return null
    val season = match.groupValues.drop(2).first(String::isNotEmpty)
    return TitleSeason(series, RomanSeasons[season] ?: season.toInt())
}

/** The season [this] scope's episodes are, as its source lists them or else its title says; null when neither does. */
internal val IntroTimesScope.season: Int?
    get() = listedSeason ?: titleSeason(title)?.season

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

private val PartSuffix = Regex("""(.+?)[\s:,(\[–—-]+(?:part|cour|часть)\s+(\d)""", RegexOption.IGNORE_CASE)

private val EpisodeNameSeason =
    Regex("""^\s*s(?:eason)?\s*(\d{1,2})\s*[:,·-]?\s*e(?:p(?:isode)?)?\.?\s*\d""", RegexOption.IGNORE_CASE)

// Upper case only: a lower-case "vi" or "ix" is more often a word.
private val RomanSuffix = Regex("""(.+?)\s+(II|III|IV|VI|VII|VIII|IX)""")
private val RomanSeasons = mapOf("II" to 2, "III" to 3, "IV" to 4, "VI" to 6, "VII" to 7, "VIII" to 8, "IX" to 9)

private val NumberSuffix = Regex("""(.+?)\s+(\d{1,2})""")

// A title ending in a bare number above these, such as "Area 88", is more often a name than a season.
private val BareSeasons = 2..19
private val Counters = setOf("no", "№", "#", "part", "cour", "vol", "volume", "movie", "film", "ova", "часть", "фильм")
