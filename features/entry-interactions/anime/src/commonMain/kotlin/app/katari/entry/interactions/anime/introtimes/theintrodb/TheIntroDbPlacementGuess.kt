package app.katari.entry.interactions.anime.introtimes.theintrodb

import app.katari.entry.interactions.anime.introtimes.IntroTimesScope
import app.katari.entry.interactions.anime.introtimes.season
import tachiyomi.domain.entry.model.TheIntroDbPlacement
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/**
 * Where [scope]'s episodes most likely sit in a series that aired [aired]. When the source dates its first episode,
 * that is the aired episode that came out the same days, which finds a second part numbered from 1 within a season.
 * Otherwise the season is the one the scope's group or title names, or the first, numbered as the source numbers it;
 * in a series whose seasons [aired] numbers by year, it is where the source's own number falls counted through them.
 */
internal fun guessPlacement(scope: IntroTimesScope, aired: List<AiredEpisode>): TheIntroDbPlacement {
    scope.firstAired?.let { day ->
        aired
            .mapNotNull { episode -> episode.aired?.let { episode to abs(ChronoUnit.DAYS.between(it, day)) } }
            .filter { (_, days) -> days <= AIR_DAY_SLACK }
            .minByOrNull { (_, days) -> days }
            ?.let { (episode, _) -> return TheIntroDbPlacement(season = episode.season, firstEpisode = episode.number) }
    }
    // A series TVmaze numbers by year runs all year round, and its sources number its episodes through.
    if (aired.numbersSeasonsByYear) {
        aired.getOrNull((scope.firstNumber ?: 1) - 1)?.let { return TheIntroDbPlacement(it.season, it.number) }
    }
    return TheIntroDbPlacement(season = scope.season ?: 1, firstEpisode = scope.firstNumber ?: 1)
}

// Simulcasts go up the day an episode airs, or the next where time zones differ, and a source may date it a day late.
private const val AIR_DAY_SLACK = 2L
