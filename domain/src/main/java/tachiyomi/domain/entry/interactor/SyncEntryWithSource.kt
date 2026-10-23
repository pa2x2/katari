package tachiyomi.domain.entry.interactor

import eu.kanade.tachiyomi.source.entry.ChapterNumberRecognitionSource
import eu.kanade.tachiyomi.source.entry.EmptyChapterListSource
import eu.kanade.tachiyomi.source.entry.MergedEntrySource
import eu.kanade.tachiyomi.source.entry.SEntry
import eu.kanade.tachiyomi.source.entry.SEntryChapter
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import tachiyomi.domain.chapter.model.NoChaptersException
import tachiyomi.domain.entry.adapter.copyFrom
import tachiyomi.domain.entry.adapter.toDomainChapter
import tachiyomi.domain.entry.adapter.toSEntry
import tachiyomi.domain.entry.adapter.toSEntryChapter
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.progressResourceKey
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.repository.EntrySourceSyncRepository
import tachiyomi.domain.entry.service.ChapterRecognition
import tachiyomi.domain.entry.service.EntryMetadataChangeNotifier
import tachiyomi.domain.entry.service.nextupdate.FetchInterval
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.source.model.SourceNotInstalledException
import tachiyomi.domain.source.service.SourceManager
import kotlin.time.Clock
import kotlin.time.Instant

class SyncEntryWithSource(
    private val entryRepository: EntryRepository,
    private val entryChapterRepository: EntryChapterRepository,
    private val entryProgressRepository: EntryProgressRepository,
    private val sourceManager: SourceManager,
    private val libraryPreferences: LibraryPreferences,
    private val fetchInterval: FetchInterval,
    private val metadataChangeNotifier: EntryMetadataChangeNotifier,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {

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
    )

    suspend fun syncStrictly(
        entry: Entry,
        profileId: Long,
        updateLibraryTitles: Boolean,
        fetchDetails: Boolean = true,
        fetchChapters: Boolean = true,
        manualFetch: Boolean = false,
        fetchWindow: Pair<Long, Long> = Pair(0L, 0L),
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
            val updatedTitle = sourceTitle?.takeIf { !entry.favorite || updateLibraryTitles }

            entry.copy(
                title = updatedTitle ?: entry.title,
                author = networkEntry.author.takeIf { !it.isNullOrBlank() && it != entry.author } ?: entry.author,
                artist = networkEntry.artist.takeIf { !it.isNullOrBlank() && it != entry.artist } ?: entry.artist,
                description =
                networkEntry.description.takeIf { !it.isNullOrBlank() && it != entry.description } ?: entry.description,
                genre = networkEntry.genre.takeIf { !it.isNullOrEmpty() && it != entry.genre } ?: entry.genre,
                status = tachiyomi.domain.entry.model.EntryStatus.from(networkEntry.status),
                thumbnailUrl = thumbnailUrl ?: entry.thumbnailUrl,
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
        val isInitialLibraryChapterSync = entryAfterMetadataSync.favorite &&
            entryAfterMetadataSync.dateAdded > 0L &&
            entryAfterMetadataSync.fetchInterval == 0 &&
            existingChapters.isEmpty()
        val rawSourceChapters = if (refresh != null && chaptersWithDetails) {
            refresh.chapters
        } else {
            source.refresh(
                entry = entryAfterMetadataSync.toSEntry(),
                existingChapters = existingChapters.map(EntryChapter::toSEntryChapter),
                fetchDetails = false,
                fetchChapters = true,
            ).chapters
        }
        if (rawSourceChapters.isEmpty() && source !is EmptyChapterListSource) {
            throw NoChaptersException()
        }
        val distinctSourceChapters = rawSourceChapters.distinctBy { it.url }
        val chapterNumbers = if (source is ChapterNumberRecognitionSource) {
            recognizeChapterNumbers(entryAfterMetadataSync.title, distinctSourceChapters)
        } else {
            distinctSourceChapters.map { it.chapterNumber }
        }
        val sourceChapters = distinctSourceChapters.mapIndexed { index, sourceChapter ->
            IndexedSourceChapter(
                chapter = sourceChapter,
                sourceOrder = index.toLong(),
                resolvedName = sourceChapter.name.ifBlank { sourceChapter.url },
                chapterNumber = chapterNumbers[index],
            )
        }
        val currentSourceUrls = sourceChapters
            .asSequence()
            .map { it.chapter.url }
            .toSet()
        val currentSourceNames = sourceChapters
            .asSequence()
            .map(IndexedSourceChapter::resolvedName)
            .toSet()
        val now = now()
        val existingChapterGroups = existingChapters.groupBy { it.sourceOrder }.values
        val progressRankByChapterId = if (existingChapterGroups.any { it.size > 1 }) {
            entryProgressRepository.getByEntryId(entry.id)
                .mapNotNull { progress ->
                    val chapterId = progress.chapterId ?: return@mapNotNull null
                    val rank = when {
                        progress.completed -> 2
                        !progress.locator.isEmpty -> 1
                        else -> 0
                    }
                    chapterId to rank
                }
                .groupingBy(Pair<Long, Int>::first)
                .fold(0) { rank, progress -> maxOf(rank, progress.second) }
        } else {
            emptyMap()
        }
        val representativeChapters = existingChapterGroups.map { chapters ->
            chapters.maxWithOrNull(
                compareBy<EntryChapter>(
                    { stateRank(it, progressRankByChapterId[it.id] ?: 0) },
                    { if (it.url in currentSourceUrls) 1 else 0 },
                    EntryChapter::lastModifiedAt,
                    EntryChapter::id,
                ),
            )!!
        }
        val chaptersToInsert = mutableListOf<EntryChapter>()
        val chaptersToUpdate = mutableListOf<EntryChapter>()
        val progressResourcesToRekey = mutableListOf<ProgressResourceRekey>()
        val matchedChapterIds = mutableSetOf<Long>()
        val chapterMatcher = EntrySourceChapterMatcher(
            chapters = representativeChapters,
            currentSourceUrls = currentSourceUrls,
            currentSourceNames = currentSourceNames,
        )
        var maxSeenUploadDate = 0L

        fun resolveDateUpload(sourceDateUpload: Long, existingDateUpload: Long? = null): Long {
            return when {
                sourceDateUpload != 0L -> {
                    maxSeenUploadDate = maxOf(maxSeenUploadDate, sourceDateUpload)
                    sourceDateUpload
                }
                existingDateUpload != null && existingDateUpload != 0L -> existingDateUpload
                else -> if (maxSeenUploadDate == 0L) now else maxSeenUploadDate
            }
        }

        sourceChapters.forEach { sourceChapter ->
            val sourceDateUpload = sourceChapter.chapter.dateUpload
            val existingChapter = chapterMatcher.match(
                sourceUrl = sourceChapter.chapter.url,
                resolvedName = sourceChapter.resolvedName,
                chapterNumber = sourceChapter.chapterNumber,
                sourceOrder = sourceChapter.sourceOrder,
            )

            if (existingChapter == null) {
                val chapterToInsert = sourceChapter.chapter.toDomainChapter(
                    entryId = entry.id,
                    sourceOrder = sourceChapter.sourceOrder,
                    dateFetch = now,
                )
                chaptersToInsert += chapterToInsert.copy(
                    dateUpload = resolveDateUpload(sourceDateUpload = sourceDateUpload),
                    chapterNumber = sourceChapter.chapterNumber,
                )
                return@forEach
            }

            matchedChapterIds += existingChapter.id

            val updatedChapter = existingChapter.copyFrom(sourceChapter.chapter, sourceChapter.sourceOrder)
                .copy(
                    chapterNumber = sourceChapter.chapterNumber,
                    dateUpload = resolveDateUpload(
                        sourceDateUpload = sourceDateUpload,
                        existingDateUpload = existingChapter.dateUpload,
                    ),
                )
            if (updatedChapter != existingChapter) {
                chaptersToUpdate += updatedChapter
                if (updatedChapter.url != existingChapter.url) {
                    progressResourcesToRekey += ProgressResourceRekey(
                        entryId = existingChapter.entryId,
                        chapterId = existingChapter.id,
                        oldResourceKey = existingChapter.progressResourceKey,
                        newResourceKey = updatedChapter.progressResourceKey,
                    )
                }
            }
        }

        val chaptersToRemove = existingChapters
            .filterNot { it.id in matchedChapterIds }
            .map(EntryChapter::id)

        val insertedChapters =
            if (chaptersToRemove.isNotEmpty() || chaptersToUpdate.isNotEmpty() || chaptersToInsert.isNotEmpty()) {
                // One transaction, so a failure part way can't leave some of the changes written and others missing
                entryChapterRepository.applySourceSync(
                    removedIds = chaptersToRemove,
                    updated = chaptersToUpdate,
                    inserted = chaptersToInsert,
                    afterUpdates = {
                        progressResourcesToRekey.forEach { rekey ->
                            entryProgressRepository.rekey(
                                entryId = rekey.entryId,
                                chapterId = rekey.chapterId,
                                oldContentKey = "",
                                oldResourceKey = rekey.oldResourceKey,
                                newContentKey = "",
                                newResourceKey = rekey.newResourceKey,
                            )
                        }
                    },
                )
            } else {
                emptyList()
            }

        val visibleChapterIds = if (insertedChapters.isEmpty()) {
            emptySet()
        } else {
            entryChapterRepository.getChaptersByEntryIdAwait(entry.id, applyScanlatorFilter = true)
                .mapTo(mutableSetOf(), EntryChapter::id)
        }
        val reportableInsertedChapters = insertedChapters.filter { it.id in visibleChapterIds }

        val hasChapterChanges =
            insertedChapters.isNotEmpty() || chaptersToUpdate.isNotEmpty() || chaptersToRemove.isNotEmpty()
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
            val removedChapterNumbers = existingChapters
                .filter { it.id in removedChapterIds && it.isRecognizedNumber }
                .mapTo(mutableSetOf(), EntryChapter::chapterNumber)
            // A first fetch lists the back catalogue, and chapters coming back under a removed number were out already
            val foundRelease = existingChapters.isNotEmpty() &&
                reportableInsertedChapters.any { !(it.isRecognizedNumber && it.chapterNumber in removedChapterNumbers) }
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
            ) ?: error("Failed to update entry ${entry.id}")
        }

        return SyncResult(
            insertedChapters = reportableInsertedChapters,
            insertedChaptersTotal = insertedChapters.size,
            updatedChapters = chaptersToUpdate.size,
            removedChapterIds = chaptersToRemove,
            hasMetadataChanges = hasMetadataChanges,
        )
    }

    private suspend fun EntryRepository.updateForSync(
        entry: Entry,
        explicitProfileId: Long?,
        updateDateAdded: Boolean,
    ): Entry? {
        val sourceSyncRepository = this as? EntrySourceSyncRepository
        if (sourceSyncRepository != null) {
            return sourceSyncRepository.updateFromSourceSync(
                entry = entry,
                profileId = explicitProfileId ?: entry.profileId,
                updateDateAdded = updateDateAdded,
            )
        }

        val updated = if (explicitProfileId == null) update(entry) else update(entry, explicitProfileId)
        return entry.takeIf { updated }
    }

    /**
     * Recognizes the chapter numbers from the whole list, since names alone don't always tell. A number the source
     * gave is kept as it is, so chapters aren't rewritten only because recognition formats it.
     */
    private fun recognizeChapterNumbers(entryTitle: String, chapters: List<SEntryChapter>): List<Double> {
        val numberings = ChapterRecognition.parseChapterNumbers(
            entryTitle,
            chapters.map { ChapterRecognition.SourceChapter(it.name, it.chapterNumber) },
        )
        return chapters.zip(numberings) { chapter, numbering ->
            chapter.chapterNumber.takeIf(ChapterRecognition::isKnown)
                ?: numbering.chapter?.let(ChapterRecognition::toDouble)
                ?: -1.0
        }
    }

    private fun stateRank(chapter: EntryChapter, progressRank: Int): Int {
        return maxOf(if (chapter.read) 2 else 0, progressRank)
    }

    private data class IndexedSourceChapter(
        val chapter: SEntryChapter,
        val sourceOrder: Long,
        val resolvedName: String,
        val chapterNumber: Double,
    )

    private data class ProgressResourceRekey(
        val entryId: Long,
        val chapterId: Long,
        val oldResourceKey: String,
        val newResourceKey: String,
    )

    data class SyncResult(
        val insertedChapters: List<EntryChapter>,
        val insertedChaptersTotal: Int = insertedChapters.size,
        val updatedChapters: Int,
        val removedChapterIds: List<Long>,
        val hasMetadataChanges: Boolean,
        /** What the source of a merged entry listed as its members, in source order; null for any other entry. */
        val mergedEntries: List<SEntry>? = null,
    ) {
        val removedChapters: Int
            get() = removedChapterIds.size

        val hasChanges: Boolean
            get() = insertedChaptersTotal > 0 || updatedChapters > 0 || removedChapters > 0
    }
}
