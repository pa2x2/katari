package eu.kanade.tachiyomi.ui.entry.olderchapters

import android.app.Application
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.data.chapterlist.ChapterListWalkJob
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.interactor.FinishChapterListWalk
import tachiyomi.domain.entry.model.ChapterListWalk
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.repository.EntryChapterListWalkRepository

/**
 * The older chapters still to come for entries whose sources give a chapter list in parts, newest first, leaving the
 * older parts to the background walk: for the entry screen, which covers an entry and each merged member's own list,
 * and for actions that need every chapter.
 */
@Inject
class EntryOlderChapters(
    private val application: Application,
    private val walks: EntryChapterListWalkRepository,
    private val finishChapterListWalk: FinishChapterListWalk,
) {
    /** The walks of [entryIds] as one status; null once every list is complete. */
    fun subscribe(entryIds: List<Long>): Flow<OlderChaptersStatus?> = walks.subscribe(entryIds).map { it.toStatus() }

    /** Goes on with the walks of [entryIds] that stopped, each from the part it stopped at. */
    suspend fun retry(entryIds: List<Long>) {
        walks.subscribe(entryIds).first()
            .filter { it.failure != null }
            .forEach { walks.setFailure(it.entryId, null) }
        ChapterListWalkJob.start(application, restart = true)
    }

    /**
     * Reads the rest of [entries]' lists now, for an action that needs every chapter. A list whose part fails keeps
     * the chapters read so far and shows why it stopped, so the action goes on with those.
     */
    suspend fun finish(entries: Collection<Entry>) {
        entries.forEach { entry ->
            try {
                finishChapterListWalk(entry)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                logcat(LogPriority.WARN, error) { "Older chapters of entry ${entry.id} stopped loading" }
            }
        }
    }
}

/**
 * [loaded] of [total] of the sites' pages of the lists are read, when every source says. [failure] is why a list
 * stopped loading, while one did.
 */
data class OlderChaptersStatus(val loaded: Int?, val total: Int?, val failure: String?)

private fun List<ChapterListWalk>.toStatus(): OlderChaptersStatus? {
    if (isEmpty()) return null
    val loaded = mapNotNull(ChapterListWalk::loaded).takeIf { it.size == size }?.sum()
    val total = mapNotNull(ChapterListWalk::total).takeIf { it.size == size }?.sum()
    return OlderChaptersStatus(
        loaded = loaded.takeIf { total != null },
        total = total.takeIf { loaded != null },
        failure = firstNotNullOfOrNull(ChapterListWalk::failure),
    )
}
