package eu.kanade.tachiyomi.source.aniyomi.refresh

import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.source.aniyomi.entry.toSAnime
import eu.kanade.tachiyomi.source.aniyomi.entry.toSEntry
import eu.kanade.tachiyomi.source.entry.EntryRefresh
import eu.kanade.tachiyomi.source.entry.SEntry
import eu.kanade.tachiyomi.source.entry.SEntryChapter

/*
 * A merged title through extensions-lib 17's combined season call, which AnimeCatalogueSource gives older extensions
 * from their separate calls. The host has no stored seasons to pass, so the call always gets an empty list: a failed
 * fetch then returns no seasons, which the host treats as a failure instead of as "nothing changed".
 */

/** The details of a merged title; the host never asks a merged title for chapters. */
internal suspend fun AnimeSource.refreshMergedDetails(
    entry: SEntry,
    existingChapters: List<SEntryChapter>,
    fetchDetails: Boolean,
): EntryRefresh {
    if (!fetchDetails) return EntryRefresh(entry, existingChapters)
    val anime = entry.toSAnime()
    val details = if (this is AnimeCatalogueSource) {
        getAnimeSeasonUpdate(anime, emptyList(), fetchDetails = true, fetchSeasons = false).anime
    } else {
        getAnimeDetails(anime)
    }
    return EntryRefresh(details.toSEntry(stored = entry), existingChapters)
}

internal suspend fun AnimeSource.mergedEntries(entry: SEntry): List<SEntry> =
    listSeasons(entry.toSAnime(), ::seasonsOf).map { it.toSEntry() }

private suspend fun AnimeSource.seasonsOf(anime: SAnime): List<SAnime> = if (this is AnimeCatalogueSource) {
    getAnimeSeasonUpdate(anime, emptyList(), fetchDetails = false, fetchSeasons = true).seasons
} else {
    getSeasonList(anime)
}
