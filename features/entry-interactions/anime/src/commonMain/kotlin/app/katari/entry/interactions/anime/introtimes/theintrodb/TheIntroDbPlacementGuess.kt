package app.katari.entry.interactions.anime.introtimes.theintrodb

import app.katari.entry.interactions.anime.introtimes.IntroTimesScope
import app.katari.entry.interactions.anime.introtimes.season
import app.katari.entry.interactions.anime.introtimes.titleSeason
import tachiyomi.domain.entry.model.TheIntroDbPlacement
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/**
 * Where [scope]'s episodes most likely sit in a series that aired [aired], or null when that can't be told. When the
 * source dates its first and last episodes the days two of [aired] as far apart came out, the first is there, which
 * finds a second part numbered from 1 within a season. Otherwise the season is the one the scope's source or title
 * names, or the first, numbered as the source numbers it; a part of a season numbered from 1 starts after the weeks
 * the season was off air before it. In a series whose seasons [aired] numbers by year, the scope is where the
 * source's own number falls counted through them.
 */
internal fun guessPlacement(scope: IntroTimesScope, aired: List<AiredEpisode>): TheIntroDbPlacement? {
    airedWhenDated(scope, aired)?.let { return TheIntroDbPlacement(season = it.season, firstEpisode = it.number) }
    // A series TVmaze numbers by year runs all year round, and its sources number its episodes through.
    if (aired.numbersSeasonsByYear) {
        aired.getOrNull((scope.firstNumber ?: 1) - 1)?.let { return TheIntroDbPlacement(it.season, it.number) }
    }
    val season = scope.season ?: 1
    val firstNumber = scope.firstNumber ?: 1
    val part = titleSeason(scope.title)?.part?.takeIf { scope.listedSeason == null } ?: 1
    if (part == 1 || firstNumber > 1) return TheIntroDbPlacement(season = season, firstEpisode = firstNumber)
    return partStart(aired, season, part)?.let { TheIntroDbPlacement(season = season, firstEpisode = it) }
}

/**
 * The number the [part]-th part of [season] starts at in [aired], told by the weeks off air between a season's parts;
 * null when the season didn't break that many times.
 */
internal fun partStart(aired: List<AiredEpisode>, season: Int, part: Int): Int? =
    aired.filter { it.season == season }
        .zipWithNext()
        .filter { (before, after) -> daysBetween(before.aired, after.aired)?.let { it > PART_BREAK_DAYS } == true }
        .getOrNull(part - 2)
        ?.second
        ?.number

/**
 * The episode of [aired] that came out the day [scope]'s source dates its first, when the last is dated the day the
 * one as far on came out too: a source that put a season up at once long after it aired may date its first episode
 * the day another one aired, but not both ends.
 */
private fun airedWhenDated(scope: IntroTimesScope, aired: List<AiredEpisode>): AiredEpisode? {
    val firstDay = scope.firstAired ?: return null
    val lastDay = scope.lastAired ?: return null
    val length = (scope.lastNumber ?: return null) - (scope.firstNumber ?: return null)
    return aired.withIndex()
        .mapNotNull { (index, episode) -> daysBetween(episode.aired, firstDay)?.let { index to it } }
        .filter { (index, days) ->
            days <= AIR_DAY_SLACK &&
                daysBetween(aired.getOrNull(index + length)?.aired, lastDay)?.let { it <= AIR_DAY_SLACK } == true
        }
        .minByOrNull { (_, days) -> days }
        ?.let { (index, _) -> aired[index] }
}

private fun daysBetween(one: LocalDate?, other: LocalDate?): Long? =
    if (one == null || other == null) null else abs(ChronoUnit.DAYS.between(one, other))

// Simulcasts go up the day an episode airs, or the next where time zones differ, and a source may date it a day late.
private const val AIR_DAY_SLACK = 2L

// A season's parts air a quarter of a year or more apart; a week skipped for a holiday or a recap isn't a new part.
private const val PART_BREAK_DAYS = 42L
