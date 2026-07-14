package tachiyomi.domain.entry.interactor

import eu.kanade.tachiyomi.source.entry.ChapterNumberRecognitionSource
import eu.kanade.tachiyomi.source.entry.EmptyChapterListSource
import eu.kanade.tachiyomi.source.entry.IncrementalChapterSource
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import tachiyomi.domain.chapter.model.NoChaptersException
import tachiyomi.domain.entry.adapter.copyFrom
import tachiyomi.domain.entry.adapter.toDomainChapter
import tachiyomi.domain.entry.adapter.toSEntry
import tachiyomi.domain.entry.adapter.toSEntryChapter
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.service.ChapterRecognition
import tachiyomi.domain.entry.service.EntryMetadataUpdateHooks
import tachiyomi.domain.entry.service.FetchInterval
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.source.model.SourceNotInstalledException
import tachiyomi.domain.source.service.SourceManager
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.abs

class SyncEntryWithSource(
    private val entryRepository: EntryRepository,
    private val entryChapterRepository: EntryChapterRepository,
    private val entryProgressRepository: EntryProgressRepository,
    private val sourceManager: SourceManager,
    private val libraryPreferences: LibraryPreferences,
    private val fetchInterval: FetchInterval,
    private val metadataUpdateHooks: EntryMetadataUpdateHooks,
    private val now: () -> Long = { Instant.now().toEpochMilli() },
) {

    suspend operator fun invoke(
        entry: Entry,
        fetchDetails: Boolean = true,
        fetchChapters: Boolean = true,
        manualFetch: Boolean = false,
        fetchWindow: Pair<Long, Long> = Pair(0L, 0L),
    ): SyncResult {
        val source = sourceManager.get(entry.source) ?: throw SourceNotInstalledException()

        val updatedEntry = if (fetchDetails) {
            val networkEntry = source.getContentDetails(entry.toSEntry())
            val thumbnailUrl = networkEntry.thumbnailUrl?.takeIf { it.isNotEmpty() }
            val coverLastModified = when {
                thumbnailUrl == null -> null
                !manualFetch && entry.thumbnailUrl == thumbnailUrl && entry.coverLastModified != 0L -> null
                else -> now()
            }
            val sourceTitle = networkEntry.title.takeIf { it.isNotBlank() && it != entry.title }
            val updatedTitle = sourceTitle?.takeIf { !entry.favorite || libraryPreferences.updateMangaTitles.get() }

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
            )
        } else {
            entry
        }

        val hasMetadataChanges = updatedEntry != entry
        if (hasMetadataChanges && !entryRepository.update(updatedEntry)) {
            error("Failed to update entry ${entry.id}")
        }
        if (updatedEntry.title != entry.title) {
            metadataUpdateHooks.onTitleChanged(entry, updatedEntry.title)
        }

        if (!fetchChapters) {
            return SyncResult(
                insertedChapters = emptyList(),
                updatedChapters = 0,
                removedChapters = 0,
                hasMetadataChanges = hasMetadataChanges,
            )
        }

        val existingChapters = entryChapterRepository.getChaptersByEntryIdAwait(entry.id)
        val sourceEntry = updatedEntry.toSEntry()
        val rawSourceChapters = if (source is IncrementalChapterSource) {
            source.getChapterList(sourceEntry, existingChapters.map(EntryChapter::toSEntryChapter))
        } else {
            source.getChapterList(sourceEntry)
        }
        if (rawSourceChapters.isEmpty() && source !is EmptyChapterListSource) {
            throw NoChaptersException()
        }
        val sourceChapters = rawSourceChapters
            .distinctBy { it.url }
            .mapIndexed { index, sourceChapter ->
                IndexedSourceChapter(
                    chapter = sourceChapter,
                    sourceOrder = index.toLong(),
                    resolvedName = sourceChapter.name.ifBlank { sourceChapter.url },
                    chapterNumber = if (source is ChapterNumberRecognitionSource) {
                        ChapterRecognition.parseChapterNumber(
                            updatedEntry.title,
                            sourceChapter.name,
                            sourceChapter.chapterNumber,
                        )
                    } else {
                        sourceChapter.chapterNumber
                    },
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
        val representativeChapters = existingChapters
            .groupBy { it.sourceOrder }
            .values
            .map { chapters ->
                chapters.maxWithOrNull(
                    compareBy<EntryChapter>(
                        { stateRank(it) },
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
        val remainingChapters = representativeChapters.toMutableList()
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

        fun matchChapter(
            sourceChapter: IndexedSourceChapter,
            predicate: (EntryChapter) -> Boolean,
        ): EntryChapter? {
            val candidates = remainingChapters.filter(predicate)
            val exactSourceOrderCandidate = candidates.firstOrNull { it.sourceOrder == sourceChapter.sourceOrder }
            val matchedChapter = exactSourceOrderCandidate
                ?: candidates.singleOrNull()
                ?: candidates.minByOrNull { abs(it.sourceOrder - sourceChapter.sourceOrder) }
            if (matchedChapter != null) {
                remainingChapters.remove(matchedChapter)
            }
            return matchedChapter
        }

        sourceChapters.forEach { sourceChapter ->
            val sourceDateUpload = sourceChapter.chapter.dateUpload
            val existingChapter = matchChapter(sourceChapter) { it.url == sourceChapter.chapter.url }
                ?: matchChapter(sourceChapter) {
                    it.name == sourceChapter.resolvedName && it.chapterNumber == sourceChapter.chapterNumber
                }
                ?: matchChapter(sourceChapter) { it.name == sourceChapter.resolvedName }
                ?: matchChapter(sourceChapter) {
                    sourceChapter.chapterNumber >= 0.0 &&
                        it.chapterNumber == sourceChapter.chapterNumber &&
                        it.url !in currentSourceUrls &&
                        it.name !in currentSourceNames
                }
                ?: matchChapter(sourceChapter) {
                    sourceChapter.chapterNumber < 0.0 && sourceChapter.resolvedName == sourceChapter.chapter.url &&
                        it.sourceOrder == sourceChapter.sourceOrder
                }

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
                        oldResourceKey = existingChapter.url,
                        newResourceKey = updatedChapter.url,
                    )
                }
            }
        }

        val chaptersToRemove = existingChapters
            .filterNot { it.id in matchedChapterIds }
            .map(EntryChapter::id)

        if (chaptersToRemove.isNotEmpty()) {
            entryChapterRepository.removeChaptersWithIds(chaptersToRemove)
        }

        if (chaptersToUpdate.isNotEmpty()) {
            if (!entryChapterRepository.updateAll(chaptersToUpdate)) {
                error("Failed to update chapters for entry ${entry.id}")
            }
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
        }

        val insertedChapters =
            if (chaptersToInsert.isNotEmpty()) {
                entryChapterRepository.insertOrUpdate(chaptersToInsert)
            } else {
                emptyList()
            }

        val hasChapterChanges =
            insertedChapters.isNotEmpty() || chaptersToUpdate.isNotEmpty() || chaptersToRemove.isNotEmpty()
        var entryAfterChapterSync = if (hasChapterChanges) {
            updatedEntry.copy(lastUpdate = now())
        } else {
            updatedEntry
        }
        val shouldUpdateFetchInterval = hasChapterChanges || manualFetch || updatedEntry.fetchInterval == 0 ||
            (fetchWindow.first != 0L && updatedEntry.nextUpdate < fetchWindow.first)
        if (shouldUpdateFetchInterval) {
            entryAfterChapterSync = fetchInterval.update(
                entry = entryAfterChapterSync,
                dateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(now()), ZoneId.systemDefault()),
                window = fetchWindow,
            )
        }
        if (entryAfterChapterSync != updatedEntry) {
            if (!entryRepository.update(entryAfterChapterSync)) {
                error("Failed to update entry ${entry.id}")
            }
        }

        val visibleChapterIds = if (insertedChapters.isEmpty()) {
            emptySet()
        } else {
            entryChapterRepository.getChaptersByEntryIdAwait(entry.id, applyScanlatorFilter = true)
                .mapTo(mutableSetOf(), EntryChapter::id)
        }
        val reportableInsertedChapters = insertedChapters.filter { it.id in visibleChapterIds }

        return SyncResult(
            insertedChapters = reportableInsertedChapters,
            insertedChaptersTotal = insertedChapters.size,
            updatedChapters = chaptersToUpdate.size,
            removedChapters = chaptersToRemove.size,
            hasMetadataChanges = hasMetadataChanges,
        )
    }

    private fun stateRank(chapter: EntryChapter): Int {
        return if (chapter.read) 1 else 0
    }

    private data class IndexedSourceChapter(
        val chapter: eu.kanade.tachiyomi.source.entry.SEntryChapter,
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
        val removedChapters: Int,
        val hasMetadataChanges: Boolean,
    ) {
        val insertedChaptersCount: Int
            get() = insertedChapters.size

        val hasChanges: Boolean
            get() = insertedChaptersTotal > 0 || updatedChapters > 0 || removedChapters > 0
    }
}
