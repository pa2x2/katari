package tachiyomi.domain.entry.interactor

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.source.entry.ChapterListPartsSource
import eu.kanade.tachiyomi.source.entry.ChapterListRest
import kotlinx.coroutines.CancellationException
import tachiyomi.domain.entry.adapter.toSEntry
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryChapterGroupRepository
import tachiyomi.domain.entry.repository.EntryChapterListWalkRepository
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.entry.service.ChapterListWalkLocks
import tachiyomi.domain.source.model.SourceNotInstalledException
import tachiyomi.domain.source.service.SourceManager
import kotlin.time.Clock

/**
 * Reads and saves the next older part of an entry's chapter list, for a walk [SyncEntryWithSource] started when the
 * source returned only the newest part.
 *
 * Its chapters follow the parts saved before them. They are the entry's back catalogue, so they stay out of Updates
 * even when they are new to Katari. Stored chapters the walk didn't list are removed only when its last part says the
 * list is complete.
 */
@Inject
class SyncOlderChapters(
    private val entryChapterRepository: EntryChapterRepository,
    entryChapterGroupRepository: EntryChapterGroupRepository,
    entryProgressRepository: EntryProgressRepository,
    private val entryChapterListWalkRepository: EntryChapterListWalkRepository,
    private val sourceManager: SourceManager,
    private val walkLocks: ChapterListWalkLocks,
) {
    private val chapterSaver = SourceChapterSaver(
        entryChapterRepository = entryChapterRepository,
        entryChapterGroupRepository = entryChapterGroupRepository,
        entryProgressRepository = entryProgressRepository,
    )

    /**
     * Saves [entry]'s next older part; [Result.Finished] when the entry has no walk to go on with. A failure is recorded
     * on the walk, under its lock so it never lands on a walk a refresh has restarted since, and rethrown.
     */
    suspend operator fun invoke(entry: Entry): Result = walkLocks.withLock(entry.id) {
        try {
            syncNextPart(entry)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            entryChapterListWalkRepository.setFailure(entry.id, error.message ?: error::class.simpleName)
            throw error
        }
    }

    private suspend fun syncNextPart(entry: Entry): Result {
        val walk = entryChapterListWalkRepository.get(entry.id) ?: return Result.Finished(removedChapters = 0)
        val source = sourceManager.get(entry.source) ?: throw SourceNotInstalledException()
        // The source no longer reads lists in parts; the next refresh brings the whole list
        if (source !is ChapterListPartsSource) {
            entryChapterListWalkRepository.finish(entry.id)
            return Result.Finished(removedChapters = 0)
        }
        val part = source.getOlderChapters(entry.toSEntry(), walk.token)
        check((part.rest as? ChapterListRest.More)?.token != walk.token) {
            "The source gave the same part of the chapter list again"
        }

        val existingChapters = entryChapterRepository.getChaptersByEntryIdAwait(entry.id)
        val listedChapterIds = entryChapterListWalkRepository.getListedChapterIds(entry.id)
        val listedChapters = existingChapters.filter { it.id in listedChapterIds }
        val listedUrls = listedChapters.mapTo(mutableSetOf(), EntryChapter::url)
        val now = Clock.System.now().toEpochMilliseconds()
        val saved = chapterSaver.save(
            entry = entry,
            source = source,
            existingChapters = existingChapters,
            chapters = part.chapters.filterNot { it.url in listedUrls },
            groups = part.chapterGroups,
            placement = ChapterListPlacement.Older(
                firstSourceOrder = walk.nextSourceOrder,
                newerGroupKey = listedChapters
                    .filter { it.groupKey != null }
                    .maxByOrNull(EntryChapter::sourceOrder)
                    ?.groupKey,
            ),
            removal = when (part.rest) {
                ChapterListRest.Complete -> ChapterRemoval.NotListed(listedChapterIds)
                ChapterListRest.Unchanged, is ChapterListRest.More -> ChapterRemoval.None
            },
            now = now,
            // At or before the moment the entry joined the Library, which is what keeps chapters out of Updates
            dateFetch = if (entry.favorite && entry.dateAdded > 0L) minOf(now, entry.dateAdded) else now,
            commitAlways = true,
            afterSave = { savedChapterIds, nextSourceOrder ->
                when (val rest = part.rest) {
                    is ChapterListRest.More -> {
                        entryChapterListWalkRepository.markListed(entry.id, savedChapterIds)
                        entryChapterListWalkRepository.advance(
                            walk.copy(
                                token = rest.token,
                                nextSourceOrder = nextSourceOrder,
                                loaded = rest.loaded,
                                total = rest.total,
                                failure = null,
                            ),
                        )
                    }
                    ChapterListRest.Complete, ChapterListRest.Unchanged ->
                        entryChapterListWalkRepository.finish(entry.id)
                }
            },
        )
        return when (val rest = part.rest) {
            is ChapterListRest.More ->
                Result.More(insertedChapters = saved.inserted.size, loaded = rest.loaded, total = rest.total)
            ChapterListRest.Complete, ChapterListRest.Unchanged ->
                Result.Finished(removedChapters = saved.removedIds.size)
        }
    }

    sealed interface Result {

        /** Older parts follow; [loaded] of [total] are the site's pages of the list, when the source says. */
        data class More(val insertedChapters: Int, val loaded: Int?, val total: Int?) : Result

        /** The walk is over, and [removedChapters] it didn't list were removed. */
        data class Finished(val removedChapters: Int) : Result
    }
}
