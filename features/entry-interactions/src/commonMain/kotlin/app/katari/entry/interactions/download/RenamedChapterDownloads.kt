package app.katari.entry.interactions.download

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.model.RenamedChapter
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository

/**
 * Moves the download of every chapter whose name, scanlator or URL changed to the name the chapter has now, as the
 * database records the changes ([EntryChapterRepository.subscribeRenamedChapters]), whichever refresh or sync made
 * them. A download is stored under a name made from those three, so without the move the chapter would show as not
 * downloaded and its download would stay behind, counted for no chapter. A change whose move fails stays recorded and
 * is tried again when the next one arrives.
 */
internal class RenamedChapterDownloads(
    private val chapterRepository: EntryChapterRepository,
    private val entryRepository: EntryRepository,
    private val downloads: EntryDownloadInteraction,
    private val scope: CoroutineScope,
) {
    private var started = false

    fun start() {
        if (started) return
        started = true

        chapterRepository.subscribeRenamedChapters()
            .distinctUntilChanged()
            .onEach { renames ->
                val moved = renames.filter { move(it) }
                if (moved.isNotEmpty()) chapterRepository.removeRenamedChapters(moved.map(RenamedChapter::id))
            }
            .launchIn(scope)
    }

    /** Returns whether [renamed] is done with: its download moved, or none to move. */
    private suspend fun move(renamed: RenamedChapter): Boolean {
        return try {
            val chapter = chapterRepository.getChapterById(renamed.chapterId) ?: return true
            val entry = entryRepository.getEntryById(renamed.entryId, renamed.profileId) ?: return true
            val former = chapter.copy(
                name = renamed.formerName,
                scanlator = renamed.formerScanlator,
                url = renamed.formerUrl,
            )
            downloads.moveRenamedDownload(entry, former, chapter)
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Failed to move the download of renamed chapter ${renamed.chapterId}" }
            false
        }
    }
}
