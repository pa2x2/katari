package tachiyomi.domain.entry.interactor

import eu.kanade.tachiyomi.source.entry.ChapterListPartsSource
import eu.kanade.tachiyomi.source.entry.ChapterListRest
import eu.kanade.tachiyomi.source.entry.EmptyChapterListSource
import eu.kanade.tachiyomi.source.entry.MergedEntrySource
import eu.kanade.tachiyomi.source.entry.SEntry
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import tachiyomi.domain.chapter.model.NoChaptersException
import tachiyomi.domain.entry.adapter.toSEntry
import tachiyomi.domain.entry.adapter.toSEntryChapter
import tachiyomi.domain.entry.model.ChapterListWalk
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryChapterGroupRepository
import tachiyomi.domain.entry.repository.EntryChapterListWalkRepository
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.repository.EntrySourceSyncRepository
import tachiyomi.domain.entry.service.ChapterListWalkLocks
import tachiyomi.domain.entry.service.ChapterListWalkScheduler
import tachiyomi.domain.entry.service.EntryMetadataChangeNotifier
import tachiyomi.domain.entry.service.nextupdate.FetchInterval
import tachiyomi.domain.entry.service.numberKey
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.source.model.SourceNotInstalledException
import tachiyomi.domain.source.service.SourceManager
import kotlin.time.Clock
import kotlin.time.Instant

class SyncEntryWithSource(
    private val entryRepository: EntryRepository,
    private val entryChapterRepository: EntryChapterRepository,
    private val entryChapterGroupRepository: EntryChapterGroupRepository,
    private val entryProgressRepository: EntryProgressRepository,
    private val sourceManager: SourceManager,
    private val libraryPreferences: LibraryPreferences,
    private val fetchInterval: FetchInterval,
    private val metadataChangeNotifier: EntryMetadataChangeNotifier,
    private val entryChapterListWalkRepository: EntryChapterListWalkRepository,
    private val chapterListWalkScheduler: ChapterListWalkScheduler,
    private val walkLocks: ChapterListWalkLocks,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {
    private val chapterSaver =
        SourceChapterSaver(entryChapterRepository, entryChapterGroupRepository, entryProgressRepository)

    suspend operator fun invoke(
        entry: Entry,
        fetchDetails: Boolean = true,
        fetchChapters: Boolean = true,
        manualFetch: Boolean = false,
        fetchWindow: Pair<Long, Long> = Pair(0L, 0L),
    ): SyncResult = sync(
        entry = entry,
        fetchDetails = fetchDetails,
        fetchChapters = fetchChapters,
        manualFetch = manualFetch,
        fetchWindow = fetchWindow,
        explicitProfileId = null,
        updateLibraryTitles = libraryPreferences.updateMangaTitles.get(),
        firstFetchIsBackCatalogue = true,
    )

    suspend fun syncStrictly(
        entry: Entry,
        profileId: Long,
        updateLibraryTitles: Boolean,
        fetchDetails: Boolean = true,
        fetchChapters: Boolean = true,
        manualFetch: Boolean = false,
        fetchWindow: Pair<Long, Long> = Pair(0L, 0L),
        firstFetchIsBackCatalogue: Boolean = true,
    ): SyncResult {
        require(entry.profileId == profileId) { "Strict Entry synchronization cannot cross profiles" }
        return sync(
            entry = entry,
            fetchDetails = fetchDetails,
            fetchChapters = fetchChapters,
            manualFetch = manualFetch,
            fetchWindow = fetchWindow,
            explicitProfileId = profileId,
            updateLibraryTitles = updateLibraryTitles,
            firstFetchIsBackCatalogue = firstFetchIsBackCatalogue,
        )
    }

    private suspend fun sync(
        entry: Entry,
        fetchDetails: Boolean,
        fetchChapters: Boolean,
        manualFetch: Boolean,
        fetchWindow: Pair<Long, Long>,
        explicitProfileId: Long?,
        updateLibraryTitles: Boolean,
        firstFetchIsBackCatalogue: Boolean,
    ): SyncResult {
        val source = sourceManager.get(entry.source) ?: throw SourceNotInstalledException()

        // Until an entry has stored chapters or members, its details can still turn it into a merged entry or out of
        // one, so its chapters are asked for only once the details have said which it is
        val mergedKindSettled = (entryRepository as? EntrySourceSyncRepository)?.hasStoredChildren(entry.id)
            ?: entryChapterRepository.getChaptersByEntryIdAwait(entry.id).isNotEmpty()
        // Once that is settled, one refresh brings both, and the chapters it returns are used below
        val chaptersWithDetails = fetchChapters && !entry.mergedBySource && (mergedKindSettled || !fetchDetails)
        val refresh = if (fetchDetails || chaptersWithDetails) {
            source.refresh(
                entry = entry.toSEntry(),
                existingChapters = if (chaptersWithDetails) {
                    entryChapterRepository.getChaptersByEntryIdAwait(entry.id).map(EntryChapter::toSEntryChapter)
                } else {
                    emptyList()
                },
                fetchDetails = fetchDetails,
                fetchChapters = chaptersWithDetails,
            )
        } else {
            null
        }

        val updatedEntry = if (refresh != null && fetchDetails) {
            val networkEntry = refresh.entry
            val thumbnailUrl = networkEntry.thumbnailUrl?.takeIf { it.isNotEmpty() }
            val coverLastModified = when {
                thumbnailUrl == null -> null
                !manualFetch && entry.thumbnailUrl == thumbnailUrl && entry.coverLastModified != 0L -> null
                else -> now()
            }
            val sourceTitle = networkEntry.title.takeIf { it.isNotBlank() && it != entry.title }
            // A member's own details often give its merged entry's title (a season page names its series), so the
            // name it was listed under ("Season 1") stays; the next listing applies any rename
            val updatedTitle = sourceTitle
                ?.takeIf { !entry.favorite || updateLibraryTitles }
                ?.takeUnless {
                    (entryRepository as? EntrySourceSyncRepository)?.isListedBySourceMerge(entry.id, entry.profileId)
                        ?: false
                }

            entry.copy(
                title = updatedTitle ?: entry.title,
                author = networkEntry.author.takeIf { !it.isNullOrBlank() && it != entry.author } ?: entry.author,
                artist = networkEntry.artist.takeIf { !it.isNullOrBlank() && it != entry.artist } ?: entry.artist,
                description =
                networkEntry.description.takeIf { !it.isNullOrBlank() && it != entry.description } ?: entry.description,
                genre = networkEntry.genre.takeIf { !it.isNullOrEmpty() && it != entry.genre } ?: entry.genre,
                status = tachiyomi.domain.entry.model.EntryStatus.from(networkEntry.status),
                thumbnailUrl = thumbnailUrl ?: entry.thumbnailUrl,
                backgroundUrl = networkEntry.backgroundUrl?.takeIf { it.isNotBlank() } ?: entry.backgroundUrl,
                coverLastModified = coverLastModified ?: entry.coverLastModified,
                updateStrategy = networkEntry.updateStrategy ?: entry.updateStrategy,
                initialized = true,
                memo = networkEntry.memo,
                mergedBySource = if (mergedKindSettled) entry.mergedBySource else networkEntry.merged,
            )
        } else {
            entry
        }

        val hasMetadataChanges = updatedEntry != entry
        val entryAfterMetadataSync = if (hasMetadataChanges) {
            entryRepository.updateForSync(
                entry = updatedEntry,
                explicitProfileId = explicitProfileId,
                updateDateAdded = false,
                updateTitle = updatedEntry.title != entry.title,
            ) ?: error("Failed to update entry ${entry.id}")
        } else {
            entry
        }
        if (hasMetadataChanges) {
            metadataChangeNotifier.changed(entry, updatedEntry)
        }

        if (!fetchChapters) {
            return SyncResult(
                insertedChapters = emptyList(),
                updatedChapters = 0,
                removedChapterIds = emptyList(),
                hasMetadataChanges = hasMetadataChanges,
            )
        }

        // A merged entry has no chapters of its own; its source lists the entries merged into it instead
        if (entryAfterMetadataSync.mergedBySource) {
            val mergedEntries = (source as? MergedEntrySource)
                ?.getMergedEntries(entryAfterMetadataSync.toSEntry())
                .orEmpty()
            if (mergedEntries.isEmpty()) throw NoChaptersException()
            return SyncResult(
                insertedChapters = emptyList(),
                updatedChapters = 0,
                removedChapterIds = emptyList(),
                hasMetadataChanges = hasMetadataChanges,
                mergedEntries = mergedEntries,
            )
        }

        val existingChapters = entryChapterRepository.getChaptersByEntryIdAwait(entry.id)
        // The first chapters of an entry added to the Library are its back catalogue, kept out of Updates by moving its
        // date added past them; those of a season its source added to a merge in the Library are new
        val isInitialLibraryChapterSync = firstFetchIsBackCatalogue &&
            entryAfterMetadataSync.favorite &&
            entryAfterMetadataSync.dateAdded > 0L &&
            entryAfterMetadataSync.fetchInterval == 0 &&
            existingChapters.isEmpty()
        val chapterRefresh = if (refresh != null && chaptersWithDetails) {
            refresh
        } else {
            source.refresh(
                entry = entryAfterMetadataSync.toSEntry(),
                existingChapters = existingChapters.map(EntryChapter::toSEntryChapter),
                fetchDetails = false,
                fetchChapters = true,
            )
        }
        val rawSourceChapters = chapterRefresh.chapters
        if (rawSourceChapters.isEmpty() && source !is EmptyChapterListSource) {
            throw NoChaptersException()
        }
        val now = now()
        // The walk's next part may be saved while the source answered. Reading the stored chapters again and saving
        // under the walk's lock keeps that part from being saved over a walk this refresh restarts, which would leave
        // the chapters before it unlisted and removed when the walk completes
        val locked = walkLocks.withLock(entry.id) {
            val storedChapters = entryChapterRepository.getChaptersByEntryIdAwait(entry.id)
            val walk = entryChapterListWalkRepository.get(entry.id)
            val rest = when (val rest = chapterRefresh.rest) {
                is ChapterListRest.More -> if (source is ChapterListPartsSource) rest else ChapterListRest.Unchanged
                // With nothing stored there is nothing older to keep
                ChapterListRest.Unchanged -> if (storedChapters.isEmpty()) ChapterListRest.Complete else rest
                ChapterListRest.Complete -> rest
            }
            val saved = chapterSaver.save(
                entry = entryAfterMetadataSync,
                source = source,
                existingChapters = storedChapters,
                chapters = rawSourceChapters,
                groups = chapterRefresh.chapterGroups,
                placement = if (rest == ChapterListRest.Complete) {
                    ChapterListPlacement.Whole
                } else {
                    ChapterListPlacement.Newest
                },
                removal = if (rest == ChapterListRest.Complete) ChapterRemoval.Unmatched else ChapterRemoval.None,
                now = now,
                commitAlways = rest is ChapterListRest.More || walk != null,
                afterSave = { listedChapterIds, nextSourceOrder ->
                    when (rest) {
                        is ChapterListRest.More -> {
                            entryChapterListWalkRepository.start(
                                ChapterListWalk(
                                    entryId = entry.id,
                                    profileId = entryAfterMetadataSync.profileId,
                                    token = rest.token,
                                    nextSourceOrder = nextSourceOrder,
                                    loaded = rest.loaded,
                                    total = rest.total,
                                ),
                            )
                            entryChapterListWalkRepository.markListed(entry.id, listedChapterIds)
                        }
                        // An unfinished walk goes on behind the newest part, and must not remove what it just listed;
                        // one that stopped on a failure is tried again
                        ChapterListRest.Unchanged -> {
                            entryChapterListWalkRepository.markListed(entry.id, listedChapterIds)
                            if (walk?.failure != null) entryChapterListWalkRepository.setFailure(entry.id, null)
                        }
                        ChapterListRest.Complete -> if (walk != null) entryChapterListWalkRepository.finish(entry.id)
                    }
                },
            )
            LockedSave(
                storedChapters,
                rest is ChapterListRest.More || (rest == ChapterListRest.Unchanged && walk != null),
                saved,
            )
        }
        val saved = locked.saved
        val insertedChapters = saved.inserted
        val chaptersToRemove = saved.removedIds
        val olderChaptersPending = locked.olderChaptersPending
        if (olderChaptersPending) chapterListWalkScheduler.schedule()

        val visibleChapterIds = if (insertedChapters.isEmpty()) {
            emptySet()
        } else {
            entryChapterRepository.getChaptersByEntryIdAwait(entry.id, applyScanlatorFilter = true)
                .mapTo(mutableSetOf(), EntryChapter::id)
        }
        val reportableInsertedChapters = insertedChapters.filter { it.id in visibleChapterIds }

        val hasChapterChanges =
            insertedChapters.isNotEmpty() || saved.updatedCount > 0 || chaptersToRemove.isNotEmpty()
        var entryAfterChapterSync = if (hasChapterChanges) {
            entryAfterMetadataSync.copy(
                lastUpdate = now,
                dateAdded = if (isInitialLibraryChapterSync) now else entryAfterMetadataSync.dateAdded,
            )
        } else {
            entryAfterMetadataSync
        }
        val shouldUpdateFetchInterval = hasChapterChanges || manualFetch || entryAfterMetadataSync.fetchInterval == 0 ||
            (fetchWindow.first != 0L && entryAfterMetadataSync.nextUpdate < fetchWindow.first)
        if (shouldUpdateFetchInterval) {
            val removedChapterIds = chaptersToRemove.toSet()
            val removedChapterNumbers = locked.storedChapters
                .filter { it.id in removedChapterIds && it.isRecognizedNumber }
                .mapTo(mutableSetOf()) { it.numberKey }
            // A first fetch lists the back catalogue, and chapters coming back under a removed number were out already
            val foundRelease = existingChapters.isNotEmpty() &&
                reportableInsertedChapters.any { !(it.isRecognizedNumber && it.numberKey in removedChapterNumbers) }
            val timeZone = TimeZone.currentSystemDefault()
            val withInterval = fetchInterval.update(
                // Only a found release counts as the last update a custom interval is counted from
                entry = entryAfterMetadataSync,
                dateTime = Instant.fromEpochMilliseconds(now).toLocalDateTime(timeZone),
                timeZone = timeZone,
                window = fetchWindow,
                foundRelease = foundRelease,
            )
            entryAfterChapterSync = entryAfterChapterSync.copy(
                nextUpdate = withInterval.nextUpdate,
                fetchInterval = withInterval.fetchInterval,
            )
        }
        if (entryAfterChapterSync != entryAfterMetadataSync) {
            entryRepository.updateForSync(
                entry = entryAfterChapterSync,
                explicitProfileId = explicitProfileId,
                updateDateAdded = isInitialLibraryChapterSync,
                updateTitle = false,
            ) ?: error("Failed to update entry ${entry.id}")
        }

        return SyncResult(
            insertedChapters = reportableInsertedChapters,
            insertedChaptersTotal = insertedChapters.size,
            updatedChapters = saved.updatedCount,
            removedChapterIds = chaptersToRemove,
            hasMetadataChanges = hasMetadataChanges,
            olderChaptersPending = olderChaptersPending,
        )
    }

    private class LockedSave(
        val storedChapters: List<EntryChapter>,
        val olderChaptersPending: Boolean,
        val saved: SavedChapters,
    )

    private suspend fun EntryRepository.updateForSync(
        entry: Entry,
        explicitProfileId: Long?,
        updateDateAdded: Boolean,
        updateTitle: Boolean,
    ): Entry? {
        val sourceSyncRepository = this as? EntrySourceSyncRepository
        if (sourceSyncRepository != null) {
            return sourceSyncRepository.updateFromSourceSync(
                entry = entry,
                profileId = explicitProfileId ?: entry.profileId,
                updateDateAdded = updateDateAdded,
                updateTitle = updateTitle,
            )
        }

        val updated = if (explicitProfileId == null) update(entry) else update(entry, explicitProfileId)
        return entry.takeIf { updated }
    }

    data class SyncResult(
        val insertedChapters: List<EntryChapter>,
        val insertedChaptersTotal: Int = insertedChapters.size,
        val updatedChapters: Int,
        val removedChapterIds: List<Long>,
        val hasMetadataChanges: Boolean,
        /** What the source of a merged entry listed as its members, in source order; null for any other entry. */
        val mergedEntries: List<SEntry>? = null,
        /** Whether older parts of the chapter list are still to be read, by [SyncOlderChapters]. */
        val olderChaptersPending: Boolean = false,
    ) {
        val removedChapters: Int
            get() = removedChapterIds.size

        val hasChanges: Boolean
            get() = insertedChaptersTotal > 0 || updatedChapters > 0 || removedChapters > 0
    }
}
