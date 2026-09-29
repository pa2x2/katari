package mihon.entry.interactions.translate

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryTranslationQueueItem
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.repository.EntryTranslationQueueRepository

/** Translates queued chapters one at a time, in queue order; a finished chapter leaves the queue. */
internal class EntryTranslateQueueRunner(
    private val repository: EntryTranslationQueueRepository,
    private val translate: EntryTranslateInteraction,
    private val entries: EntryRepository,
    private val chapters: EntryChapterRepository,
) {
    private val mutableActive = MutableStateFlow<Active?>(null)

    /** The chapter being translated, if any. */
    val active: StateFlow<Active?> = mutableActive.asStateFlow()

    @Volatile
    private var work: Deferred<EntryTranslateResult>? = null

    suspend fun hasPendingWork(): Boolean = next() != null

    /** Translates queued chapters until none is left. */
    suspend fun runUntilIdle() = coroutineScope {
        while (true) {
            val item = next() ?: return@coroutineScope
            val entry = entries.getEntryById(item.entryId)
            val chapter = chapters.getChapterById(item.chapterId)
            val setup = item.setup?.let(EntryTranslateSetupCodec::decode)
            if (entry == null || chapter == null) {
                repository.delete(listOf(item.chapterId))
                continue
            }
            if (setup == null) {
                // Retrying prepares the chapter again with the current settings.
                logcat(LogPriority.WARN) { "Queued setup of chapter ${item.chapterId} cannot be read: ${item.setup}" }
                repository.fail(item.chapterId, EntryTranslateFailure.SetupRequired)
                continue
            }
            mutableActive.value = Active(entry, chapter, EntryTranslateProgress(0, 0))
            val result = try {
                async { translate(entry, chapter, setup) }.also { work = it }.await()
            } catch (_: CancellationException) {
                // Cancelling only this chapter leaves the runner going; it was already removed from the queue.
                ensureActive()
                null
            } finally {
                work = null
                mutableActive.value = null
            }
            when (result) {
                EntryTranslateResult.Translated -> repository.delete(listOf(item.chapterId))
                is EntryTranslateResult.Failed -> repository.fail(item.chapterId, result.failure)
                null -> Unit
            }
        }
    }

    /** Stops translating [chapterIds] if one of them is being translated; nothing of it is kept. */
    fun stop(chapterIds: Collection<Long>) {
        if (mutableActive.value?.chapter?.id in chapterIds) work?.cancel()
    }

    private suspend fun translate(entry: Entry, chapter: EntryChapter, setup: EntryTranslateSetup) = try {
        translate.translate(entry, chapter, setup) { progress ->
            mutableActive.update { it?.copy(progress = progress) }
        }
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        logcat(LogPriority.ERROR, error) { "Translating chapter ${chapter.id} failed" }
        EntryTranslateResult.Failed(EntryTranslateFailure.Error(error.message))
    }

    private suspend fun next(): EntryTranslationQueueItem? =
        repository.getAll().firstOrNull { it.state == EntryTranslationQueueItem.State.Queued }

    data class Active(
        val entry: Entry,
        val chapter: EntryChapter,
        val progress: EntryTranslateProgress,
    )
}
