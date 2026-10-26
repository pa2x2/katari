package tachiyomi.domain.entry.interactor

import eu.kanade.tachiyomi.source.entry.ChapterNumberRecognitionSource
import eu.kanade.tachiyomi.source.entry.SEntryChapter
import eu.kanade.tachiyomi.source.entry.SEntryChapterGroup
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import tachiyomi.domain.entry.adapter.copyFrom
import tachiyomi.domain.entry.adapter.toDomainChapter
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryChapterGroup
import tachiyomi.domain.entry.model.progressResourceKey
import tachiyomi.domain.entry.repository.EntryChapterGroupRepository
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.entry.service.ChapterRecognition
import tachiyomi.domain.entry.service.mergeChapterGroups

/**
 * Saves chapters a source listed against the entry's stored ones: matches them, carries read state and progress over
 * to renamed chapters, removes what the source no longer lists and keeps the entry's chapter groups, all in one
 * transaction.
 *
 * The list may be the whole list or one part of a list that arrives in parts ([ChapterListPlacement]); a part never
 * removes stored chapters on its own, and the stored chapters it doesn't list keep their places.
 */
internal class SourceChapterSaver(
    private val entryChapterRepository: EntryChapterRepository,
    private val entryChapterGroupRepository: EntryChapterGroupRepository,
    private val entryProgressRepository: EntryProgressRepository,
) {

    suspend fun save(
        entry: Entry,
        source: UnifiedSource,
        existingChapters: List<EntryChapter>,
        chapters: List<SEntryChapter>,
        groups: List<SEntryChapterGroup>,
        placement: ChapterListPlacement,
        removal: ChapterRemoval,
        now: Long,
        dateFetch: Long = now,
        commitAlways: Boolean = false,
        afterSave: suspend (listedChapterIds: List<Long>, nextSourceOrder: Long) -> Unit = { _, _ -> },
    ): SavedChapters {
        val distinctSourceChapters = chapters.distinctBy { it.url }
        val chapterNumbers = if (source is ChapterNumberRecognitionSource) {
            recognizeChapterNumbers(entry.title, distinctSourceChapters)
        } else {
            distinctSourceChapters.map { it.chapterNumber }
        }
        val partUrls = distinctSourceChapters.mapTo(mutableSetOf(), SEntryChapter::url)
        val firstSourceOrder = when (placement) {
            ChapterListPlacement.Whole -> 0L
            // Just before the oldest stored chapters the part doesn't list, which keep their order
            ChapterListPlacement.Newest -> (
                existingChapters.filter { it.url !in partUrls }.minOfOrNull(EntryChapter::sourceOrder)
                    ?: distinctSourceChapters.size.toLong()
                ) - distinctSourceChapters.size
            is ChapterListPlacement.Older -> placement.firstSourceOrder
        }
        val declaredGroups = groups
            .filter { it.key.isNotBlank() }
            .distinctBy { it.key }
            .map { it.key to it.title.ifBlank { it.key } }
        val storedGroups = entryChapterGroupRepository.getByEntryId(entry.id)
        val mergedGroups = when (placement) {
            ChapterListPlacement.Whole -> declaredGroups.mapIndexed { position, (key, title) ->
                EntryChapterGroup(entryId = entry.id, key = key, title = title, position = position)
            }
            ChapterListPlacement.Newest -> mergeChapterGroups(entry.id, storedGroups, declaredGroups, null)
            is ChapterListPlacement.Older ->
                mergeChapterGroups(entry.id, storedGroups, declaredGroups, placement.newerGroupKey)
        }
        val groupPositions = mergedGroups.associate { it.key to it.position }
        val declaredKeys = declaredGroups.mapTo(mutableSetOf(), Pair<String, String>::first)
        val sourceChapters = distinctSourceChapters.mapIndexed { index, sourceChapter ->
            IndexedSourceChapter(
                chapter = sourceChapter,
                sourceOrder = firstSourceOrder + index,
                resolvedName = sourceChapter.name.ifBlank { sourceChapter.url },
                chapterNumber = chapterNumbers[index],
                // A key the part doesn't declare names no group
                groupKey = sourceChapter.group?.takeIf { it in declaredKeys },
                groupPosition = sourceChapter.group?.takeIf { it in declaredKeys }?.let(groupPositions::get),
            )
        }
        // In a part, stored chapters it doesn't list may be in another part, so none of them counts as gone from the
        // source: matching must not hand one of them to a part's chapter by its number or position
        val currentSourceUrls = sourceChapters.mapTo(mutableSetOf()) { it.chapter.url }
            .apply { if (placement != ChapterListPlacement.Whole) addAll(existingChapters.map(EntryChapter::url)) }
        val currentSourceNames = sourceChapters.mapTo(mutableSetOf(), IndexedSourceChapter::resolvedName)
            .apply { if (placement != ChapterListPlacement.Whole) addAll(existingChapters.map(EntryChapter::name)) }
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
        val representativeChapters = existingChapterGroups.map { sameOrder ->
            sameOrder.maxWithOrNull(
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
                    dateFetch = dateFetch,
                )
                chaptersToInsert += chapterToInsert.copy(
                    dateUpload = resolveDateUpload(sourceDateUpload = sourceDateUpload),
                    chapterNumber = sourceChapter.chapterNumber,
                    groupKey = sourceChapter.groupKey,
                    groupPosition = sourceChapter.groupPosition,
                )
                return@forEach
            }

            matchedChapterIds += existingChapter.id

            val updatedChapter = existingChapter.copyFrom(sourceChapter.chapter, sourceChapter.sourceOrder)
                .copy(
                    chapterNumber = sourceChapter.chapterNumber,
                    groupKey = sourceChapter.groupKey,
                    groupPosition = sourceChapter.groupPosition,
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

        val chaptersToRemove = when (removal) {
            ChapterRemoval.Unmatched -> existingChapters.filterNot { it.id in matchedChapterIds }.map(EntryChapter::id)
            ChapterRemoval.None -> emptyList()
            is ChapterRemoval.NotListed ->
                existingChapters
                    .filterNot { it.id in matchedChapterIds || it.id in removal.listedChapterIds }
                    .map(EntryChapter::id)
        }
        val removedIds = chaptersToRemove.toSet()
        // Stored chapters the part doesn't list follow their group's new position, or lose a group a whole list or a
        // finished walk no longer has
        val finalGroups = if (removal is ChapterRemoval.NotListed) {
            val usedKeys = (chaptersToInsert + chaptersToUpdate).mapNotNullTo(mutableSetOf(), EntryChapter::groupKey) +
                existingChapters.filter { it.id !in removedIds && it.id !in matchedChapterIds }
                    .mapNotNull(EntryChapter::groupKey)
            mergedGroups.filter { it.key in usedKeys }
        } else {
            mergedGroups
        }
        val finalPositions = finalGroups.associate { it.key to it.position }
        if (placement != ChapterListPlacement.Whole) {
            existingChapters
                .filter { it.id !in matchedChapterIds && it.id !in removedIds }
                .forEach { chapter ->
                    val position = chapter.groupKey?.let(finalPositions::get)
                    val regrouped = chapter.copy(
                        groupKey = chapter.groupKey.takeIf { position != null },
                        groupPosition = position,
                    )
                    if (regrouped != chapter) chaptersToUpdate += regrouped
                }
        }

        val nextSourceOrder = firstSourceOrder + sourceChapters.size
        val groupsChanged = finalGroups != storedGroups
        val hasChanges = chaptersToRemove.isNotEmpty() || chaptersToUpdate.isNotEmpty() ||
            chaptersToInsert.isNotEmpty() || groupsChanged
        val insertedChapters = if (hasChanges || commitAlways) {
            // One transaction, so a failure part way can't leave some of the changes written and others missing
            entryChapterRepository.applySourceSync(
                removedIds = chaptersToRemove,
                updated = chaptersToUpdate,
                inserted = chaptersToInsert,
                afterUpdates = {
                    if (groupsChanged) entryChapterGroupRepository.replace(entry.id, finalGroups)
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
                afterInserts = { inserted ->
                    afterSave(matchedChapterIds.toList() + inserted.map(EntryChapter::id), nextSourceOrder)
                },
            )
        } else {
            emptyList()
        }
        return SavedChapters(
            inserted = insertedChapters,
            updatedCount = chaptersToUpdate.size,
            removedIds = chaptersToRemove,
            nextSourceOrder = nextSourceOrder,
        )
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
        val groupKey: String?,
        val groupPosition: Int?,
    )

    private data class ProgressResourceRekey(
        val entryId: Long,
        val chapterId: Long,
        val oldResourceKey: String,
        val newResourceKey: String,
    )
}

/** Where the chapters a source listed sit in the entry's whole chapter list. */
internal sealed interface ChapterListPlacement {

    /** They are the whole list. */
    data object Whole : ChapterListPlacement

    /** They are the newest part, before every stored chapter they don't list. */
    data object Newest : ChapterListPlacement

    /**
     * They are an older part, following the parts a walk saved so far: the first gets [firstSourceOrder].
     * [newerGroupKey] is the group of the nearest newer chapter the walk saved, which new groups of the part go before.
     */
    data class Older(val firstSourceOrder: Long, val newerGroupKey: String?) : ChapterListPlacement
}

/** Which stored chapters a save removes. */
internal sealed interface ChapterRemoval {

    /** Those the chapters don't match: they were the whole list. */
    data object Unmatched : ChapterRemoval

    /** None: the chapters were one part. */
    data object None : ChapterRemoval

    /** Those neither the chapters nor the earlier parts of the walk listed: they ended a complete walk. */
    data class NotListed(val listedChapterIds: Set<Long>) : ChapterRemoval
}

internal data class SavedChapters(
    val inserted: List<EntryChapter>,
    val updatedCount: Int,
    val removedIds: List<Long>,
    /** [EntryChapter.sourceOrder] a following older part starts at. */
    val nextSourceOrder: Long,
)
