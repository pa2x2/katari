package app.katari.entry.interactions.anime.introtimes.aniskip

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** An episode as AniSkip keys its times: by the MyAnimeList id of its series and its number there. */
internal data class AniSkipEpisode(val malId: Long, val episode: Int)

/**
 * How AniSkip continues a show's episode numbers past its first season: the numbers in [from] are the episodes of the
 * series [malId] counted from [start]. It counts a special between seasons where those who number a show in one run do.
 */
internal data class AniSkipNumbering(val from: IntRange, val malId: Long, val start: Int)

/** The numberings in an AniSkip answer; one left open at its end runs on for as long as the show does. */
internal fun aniSkipNumberings(body: String): List<AniSkipNumbering> =
    json.decodeFromString<RulesResponse>(body).rules.orEmpty().map { rule ->
        AniSkipNumbering(rule.from.start..(rule.from.end ?: Int.MAX_VALUE), rule.to.malId, rule.to.start)
    }

/**
 * Where a show's [episode], counted from the start of the [index]-th of its [seasons], is asked of AniSkip when that
 * season doesn't have it: counted on into the seasons after it, or back into those before. A show numbered in one run
 * from its first season goes by AniSkip's own [numberings] of that season, where it has them. Null when the show has
 * no such episode.
 */
internal fun episodeInSeasons(
    seasons: List<AniListSeries>,
    index: Int,
    episode: Int,
    numberings: List<AniSkipNumbering>,
): AniSkipEpisode? {
    if (index == 0) {
        numberings.firstOrNull { episode in it.from }?.let { numbering ->
            return AniSkipEpisode(numbering.malId, numbering.start + episode - numbering.from.first)
        }
    }
    var at = index
    var number = episode
    while (number < 1) {
        at--
        number += seasons.getOrNull(at)?.episodes ?: return null
    }
    while (true) {
        val season = seasons.getOrNull(at) ?: return null
        // A season still airing has room for any number.
        val count = season.episodes ?: return AniSkipEpisode(season.malId, number)
        if (number <= count) return AniSkipEpisode(season.malId, number)
        number -= count
        at++
    }
}

@Serializable
private data class RulesResponse(val rules: List<Rule>? = null)

@Serializable
private data class Rule(val from: Range, val to: Target)

@Serializable
private data class Range(val start: Int, val end: Int? = null)

@Serializable
private data class Target(val malId: Long, val start: Int)

private val json = Json { ignoreUnknownKeys = true }
