package eu.kanade.tachiyomi.source.aniyomi.related

import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.source.aniyomi.capability.RelatedTitlesCall
import eu.kanade.tachiyomi.source.aniyomi.entry.toSAnime
import eu.kanade.tachiyomi.source.aniyomi.entry.toSEntry
import eu.kanade.tachiyomi.source.entry.RelatedEntryGroup
import eu.kanade.tachiyomi.source.entry.SEntry
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat

/** The titles the extension relates to [entry], in its groups; a blank group name stands for no name. */
internal suspend fun AnimeSource.relatedTitles(entry: SEntry, call: RelatedTitlesCall): List<RelatedEntryGroup> {
    val anime = entry.toSAnime()
    return when (call) {
        RelatedTitlesCall.GROUPED -> getRelatedAnimeList(anime).map { relation ->
            group(relation.name, relation.animes)
        }
        RelatedTitlesCall.PUSHED -> pushedGroups(anime)
        RelatedTitlesCall.FETCHED -> listOf(group("", (this as AnimeCatalogueSource).fetchRelatedAnimeList(anime)))
    }
}

/** A failed group is logged; when every group failed and none arrived, the first failure is the answer. */
private suspend fun AnimeSource.pushedGroups(anime: SAnime): List<RelatedEntryGroup> {
    val groups = mutableListOf<RelatedEntryGroup>()
    val failures = mutableListOf<Throwable>()
    getRelatedAnimeList(
        anime = anime,
        exceptionHandler = { failure ->
            logcat(LogPriority.WARN, failure) { "A group of related titles failed" }
            synchronized(failures) { failures += failure }
        },
        pushResults = { (label, titles), _ -> synchronized(groups) { groups += group(label, titles) } },
    )
    if (groups.isEmpty()) failures.firstOrNull()?.let { throw it }
    return groups
}

private fun group(name: String, titles: List<SAnime>) =
    RelatedEntryGroup(title = name.ifBlank { null }, entries = titles.map { it.toSEntry() })
