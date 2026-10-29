package eu.kanade.tachiyomi.source.aniyomi.refresh

import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.model.SAnimeEpisodeUpdate
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.source.aniyomi.entry.toSAnime
import eu.kanade.tachiyomi.source.aniyomi.entry.toSEntry
import eu.kanade.tachiyomi.source.aniyomi.entry.toSEntryChapter
import eu.kanade.tachiyomi.source.aniyomi.entry.toSEpisode
import eu.kanade.tachiyomi.source.entry.EntryRefresh
import eu.kanade.tachiyomi.source.entry.SEntry
import eu.kanade.tachiyomi.source.entry.SEntryChapter

/**
 * Refreshes a title with episodes of its own through extensions-lib 17's combined call, which [AnimeCatalogueSource]
 * gives older extensions from their separate details and episode calls. A source that isn't a catalogue never had the
 * combined call, so it gets the separate ones directly.
 */
internal suspend fun AnimeSource.refreshEpisodes(
    entry: SEntry,
    existingEpisodes: List<SEntryChapter>,
    fetchDetails: Boolean,
    fetchEpisodes: Boolean,
): EntryRefresh {
    val anime = entry.toSAnime()
    val update = if (this is AnimeCatalogueSource) {
        getAnimeEpisodeUpdate(anime, existingEpisodes.map { it.toSEpisode() }, fetchDetails, fetchEpisodes)
    } else {
        SAnimeEpisodeUpdate(
            anime = if (fetchDetails) getAnimeDetails(anime) else anime,
            episodes = if (fetchEpisodes) getEpisodeList(anime) else emptyList(),
        )
    }
    val episodes = if (fetchEpisodes) {
        update.episodes.map { episode ->
            (this as? AnimeHttpSource)?.prepareNewEpisode(episode, anime)
            episode.toSEntryChapter()
        }
    } else {
        existingEpisodes
    }
    return EntryRefresh(
        entry = if (fetchDetails) update.anime.toSEntry(stored = entry) else entry,
        chapters = episodes,
    )
}
