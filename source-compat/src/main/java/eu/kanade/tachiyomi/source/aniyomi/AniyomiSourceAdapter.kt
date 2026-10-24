package eu.kanade.tachiyomi.source.aniyomi

import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.source.aniyomi.catalogue.toEntryFilterList
import eu.kanade.tachiyomi.source.aniyomi.catalogue.withAnimeFilterValues
import eu.kanade.tachiyomi.source.aniyomi.entry.toEntryPageResult
import eu.kanade.tachiyomi.source.aniyomi.entry.toSEpisode
import eu.kanade.tachiyomi.source.aniyomi.playback.streamsOf
import eu.kanade.tachiyomi.source.aniyomi.refresh.mergedEntries
import eu.kanade.tachiyomi.source.aniyomi.refresh.refreshEpisodes
import eu.kanade.tachiyomi.source.aniyomi.refresh.refreshMergedDetails
import eu.kanade.tachiyomi.source.entry.ChapterNumberRecognitionSource
import eu.kanade.tachiyomi.source.entry.EntryCatalogueSource
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.entry.EntryMedia
import eu.kanade.tachiyomi.source.entry.EntryPageResult
import eu.kanade.tachiyomi.source.entry.EntryRefresh
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.MergedEntrySource
import eu.kanade.tachiyomi.source.entry.PlaybackDescriptor
import eu.kanade.tachiyomi.source.entry.PlaybackSelection
import eu.kanade.tachiyomi.source.entry.SEntry
import eu.kanade.tachiyomi.source.entry.SEntryChapter
import eu.kanade.tachiyomi.source.entry.SourceMetadata
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** The wrapped extension, for the capability bridges' default bodies. */
internal interface AniyomiSourceHolder {
    val source: AnimeSource
}

/**
 * An Aniyomi [AnimeSource] as an Entry SDK source of anime under [id], the key Katari stores it under; the extension's
 * own ID stays inside. This class holds what every source has. Katari tells capabilities apart by type, so those only
 * some sources have come from the bridges in AniyomiCapabilityBridges, combined in AniyomiSourceAdapters.
 */
internal open class AniyomiSourceAdapter(
    final override val source: AnimeSource,
    final override val id: Long,
) : EntryCatalogueSource,
    MergedEntrySource,
    SourceMetadata,
    ChapterNumberRecognitionSource,
    AniyomiSourceHolder {

    override val name: String get() = source.name
    override val lang: String get() = source.lang
    override val supportsLatest: Boolean get() = source.supportsLatest
    override val supportedEntryTypes: Set<EntryType> = setOf(EntryType.ANIME)

    private val filterRequests = Mutex()

    override fun getFilterList(): EntryFilterList = source.getFilterList().toEntryFilterList()

    override suspend fun getPopularContent(page: Int): EntryPageResult<SEntry> =
        callExtension { source.getPopularAnime(page).toEntryPageResult() }

    override suspend fun getLatestUpdates(page: Int): EntryPageResult<SEntry> =
        callExtension { source.getLatestUpdates(page).toEntryPageResult() }

    override suspend fun getSearchContent(
        page: Int,
        query: String,
        filters: EntryFilterList,
    ): EntryPageResult<SEntry> = filterRequests.withLock {
        filters.withAnimeFilterValues { animeFilters ->
            callExtension { source.getSearchAnime(page, query, animeFilters).toEntryPageResult() }
        }
    }

    override suspend fun refresh(
        entry: SEntry,
        existingChapters: List<SEntryChapter>,
        fetchDetails: Boolean,
        fetchChapters: Boolean,
    ): EntryRefresh = callExtension {
        if (entry.merged) {
            source.refreshMergedDetails(entry, existingChapters, fetchDetails)
        } else {
            source.refreshEpisodes(entry, existingChapters, fetchDetails, fetchChapters)
        }
    }

    override suspend fun getMergedEntries(entry: SEntry): List<SEntry> = callExtension { source.mergedEntries(entry) }

    override suspend fun getMedia(chapter: SEntryChapter, selection: PlaybackSelection): EntryMedia = callExtension {
        val videos = source.getVideoList(chapter.toSEpisode())
        EntryMedia.Playback(PlaybackDescriptor(streams = streamsOf(videos, (source as? AnimeHttpSource)?.headers)))
    }
}
