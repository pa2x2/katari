package eu.kanade.tachiyomi.source.aniyomi.refresh

import eu.kanade.tachiyomi.animesource.model.FetchType
import eu.kanade.tachiyomi.animesource.model.SAnime

/*
 * A title made of seasons becomes a merge its source supplies, which Katari keeps one level deep and oldest first.
 * Extensions list seasons in their own order (FilmiFen newest first; Jellyfin, Movy and Stremio oldest first; Jellyfin
 * box sets by name), so the extension's own season numbers decide where every season has one. Titles are never parsed
 * for numbers: a box set's "Blade Runner 2049" is no season 2049. A listed title that has seasons itself, such as a
 * series in a Jellyfin box set, is replaced by its seasons, which keep their own titles.
 */

/**
 * The seasons of [root], oldest first, with nested titles replaced by their seasons at most [MAX_LEVELS] levels deep.
 * [seasonsOf] lists one title's seasons; a title listed a second time, such as a box set that lists itself, is skipped.
 */
internal suspend fun listSeasons(root: SAnime, seasonsOf: suspend (SAnime) -> List<SAnime>): List<SAnime> {
    val listed = mutableSetOf(root.url)
    suspend fun expand(title: SAnime, level: Int): List<SAnime> = oldestFirst(seasonsOf(title)).flatMap { season ->
        when {
            !listed.add(season.url) -> emptyList()
            season.fetch_type != FetchType.Seasons -> listOf(season)
            level < MAX_LEVELS -> expand(season, level + 1)
            else -> emptyList()
        }
    }
    return expand(root, level = 1)
}

/** [seasons] by their numbers when each has one, specials (season 0) first; otherwise as the extension listed them. */
internal fun oldestFirst(seasons: List<SAnime>): List<SAnime> =
    if (seasons.all { it.season_number >= 0 }) seasons.sortedBy { it.season_number } else seasons

private const val MAX_LEVELS = 3
