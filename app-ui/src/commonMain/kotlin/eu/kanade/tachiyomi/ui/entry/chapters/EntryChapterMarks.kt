package eu.kanade.tachiyomi.ui.entry.chapters

import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import eu.kanade.tachiyomi.ui.entry.EntryStateAccess
import kotlinx.coroutines.CoroutineScope
import mihon.entry.interactions.state.EntryBookmarkFeature
import mihon.entry.interactions.state.EntryConsumptionFeature
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryRepository

/** Marking an entry screen's chapters read or bookmarked, each on the member it belongs to. */
@AssistedInject
class EntryChapterMarks(
    @Assisted private val scope: CoroutineScope,
    @Assisted private val state: EntryStateAccess,
    @Assisted private val selection: EntryChapterSelection,
    private val entryRepository: EntryRepository,
    private val entryConsumptionFeature: EntryConsumptionFeature,
    private val entryBookmarkFeature: EntryBookmarkFeature,
) {

    @AssistedFactory
    fun interface Factory {
        fun create(
            scope: CoroutineScope,
            state: EntryStateAccess,
            selection: EntryChapterSelection,
        ): EntryChapterMarks
    }

    fun markPreviousRead(pointer: EntryChapter) {
        val loaded = state.loaded ?: return
        val prevChapters = loaded.readingChapters.map { it.chapter }
        val pointerPos = prevChapters.indexOf(pointer)
        if (pointerPos != -1) markRead(prevChapters.take(pointerPos), true)
    }

    fun markRead(chapters: List<EntryChapter>, read: Boolean) {
        selection.setAll(false)
        if (chapters.isEmpty()) return
        scope.launchIO {
            chapters.groupBy { it.entryId }.forEach { (memberEntryId, memberChapters) ->
                val entry = entryRepository.getEntryById(memberEntryId) ?: return@forEach
                entryConsumptionFeature.setConsumed(entry, memberChapters, read)
            }
        }
    }

    fun bookmark(chapters: List<EntryChapter>, bookmarked: Boolean) {
        scope.launchIO {
            chapters.groupBy { it.entryId }.forEach { (memberEntryId, memberChapters) ->
                val entry = entryRepository.getEntryById(memberEntryId) ?: return@forEach
                entryBookmarkFeature.setBookmarked(entry, memberChapters, bookmarked)
            }
        }
        selection.setAll(false)
    }
}
