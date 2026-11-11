package app.katari.entry.interactions.anime.introtimes.theintrodb

import tachiyomi.domain.entry.model.TheIntroDbPlacement

/** An episode as TheIntroDB can be asked for it. */
internal sealed interface TheIntroDbEpisode {
    /** By its season and number there, as TVDB numbers them. */
    data class InSeason(val season: Int, val episode: Int) : TheIntroDbEpisode

    /** By its [number] counted through the whole series, which TVDB keeps for series numbered that way too. */
    data class Running(val number: Int) : TheIntroDbEpisode
}

/**
 * Where the [number]-th episode of a scope placed at [placement] is in [aired], a series' episodes in the order they
 * came out: counted on from the placement into the seasons after it, or back into those before. Null when [aired]
 * doesn't have the placement's season; outside [aired] when the series has no such episode.
 */
internal fun airedIndex(placement: TheIntroDbPlacement, number: Int, aired: List<AiredEpisode>): Int? {
    val seasonStart = aired.indexOfFirst { it.season == placement.season }.takeIf { it >= 0 } ?: return null
    return seasonStart + (placement.firstEpisode - aired[seasonStart].number) + number - 1
}

/** The placement that makes the scope's [number]-th episode the one at [index] of [aired]. */
internal fun placementAt(index: Int, number: Int, aired: List<AiredEpisode>): TheIntroDbPlacement {
    val first = index - (number - 1)
    // A scope that starts before the series does is placed from the series' first episode, counted back.
    val anchor = aired[first.coerceAtLeast(0)]
    return TheIntroDbPlacement(season = anchor.season, firstEpisode = anchor.number + first.coerceAtMost(0))
}

/**
 * The ways to ask TheIntroDB for the [number]-th episode of a scope placed at [placement], the likeliest first; empty
 * when the series has no such episode. [aired] is TVmaze's list of the series, empty when it has none.
 */
internal fun episodeAsks(
    placement: TheIntroDbPlacement,
    number: Int,
    aired: List<AiredEpisode>,
): List<TheIntroDbEpisode> {
    val index = airedIndex(placement, number, aired)
    if (index == null) {
        // Without the season's episodes its length isn't known: past the first season's end only a running number
        // finds the episode.
        val episode = placement.firstEpisode + number - 1
        if (episode < 1) return emptyList()
        return listOfNotNull(
            TheIntroDbEpisode.InSeason(placement.season, episode),
            TheIntroDbEpisode.Running(episode).takeIf { placement.season == 1 },
        )
    }
    val episode = aired.getOrNull(index) ?: return emptyList()
    val inSeason = TheIntroDbEpisode.InSeason(episode.season, episode.number)
    // TVmaze numbers some long series' seasons by year where TVDB numbers them by story, so its season isn't TVDB's.
    return if (aired.numbersSeasonsByYear) listOf(TheIntroDbEpisode.Running(index + 1), inSeason) else listOf(inSeason)
}

/** Whether the seasons are years, as TVmaze numbers a series that runs all year round. */
internal val List<AiredEpisode>.numbersSeasonsByYear: Boolean
    get() = isNotEmpty() && all { it.season >= FIRST_YEAR }

private const val FIRST_YEAR = 1900
