package app.katari.entry.interactions.anime.introtimes.theintrodb

import app.katari.entry.interactions.anime.introtimes.IntroTimesScope
import tachiyomi.domain.entry.model.TheIntroDbPlacement
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/**
 * Where [scope]'s episodes most likely sit in a series that aired [aired]. When the source dates its first episode,
 * that is the aired episode that came out the same days, which finds a second part numbered from 1 within a season.
 * Otherwise the season is the one the scope's season name gives, or the first, numbered as the source numbers it.
 */
internal fun guessPlacement(scope: IntroTimesScope, aired: List<AiredEpisode>): TheIntroDbPlacement {
    scope.firstAired?.let { day ->
        aired
            .mapNotNull { episode -> episode.aired?.let { episode to abs(ChronoUnit.DAYS.between(it, day)) } }
            .filter { (_, days) -> days <= AIR_DAY_SLACK }
            .minByOrNull { (_, days) -> days }
            ?.let { (episode, _) -> return TheIntroDbPlacement(season = episode.season, firstEpisode = episode.number) }
    }
    return TheIntroDbPlacement(season = seasonNumber(scope.seasonName) ?: 1, firstEpisode = scope.firstNumber ?: 1)
}

/** The number a group's title gives its season, as in "Season 2", "2nd Season", "Сезон 2", "2 сезон" or "S2". */
internal fun seasonNumber(name: String?): Int? {
    val match = name?.let(SeasonName::find) ?: return null
    return match.groupValues.drop(1).firstOrNull(String::isNotEmpty)?.toInt()
}

private val SeasonName =
    Regex(
        """(?:season|сезон|\bs)\s*(\d{1,4})\b|\b(\d{1,4})(?:st|nd|rd|th|-?й)?\s*(?:season|сезон)""",
        RegexOption.IGNORE_CASE,
    )

// Simulcasts go up the day an episode airs, or the next where time zones differ, and a source may date it a day late.
private const val AIR_DAY_SLACK = 2L
