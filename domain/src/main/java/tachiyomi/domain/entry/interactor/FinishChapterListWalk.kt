package tachiyomi.domain.entry.interactor

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.repository.EntryChapterListWalkRepository

/**
 * Reads the rest of an entry's chapter list now, part after part, for work that needs the whole list, such as
 * migration carrying read progress chapter by chapter or downloading every chapter. A part that fails is recorded on
 * the walk, which the background walk retries, and rethrown.
 */
@Inject
class FinishChapterListWalk(
    private val syncOlderChapters: SyncOlderChapters,
    private val entryChapterListWalkRepository: EntryChapterListWalkRepository,
) {
    suspend operator fun invoke(entry: Entry) {
        while (true) {
            val result = try {
                syncOlderChapters(entry)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                entryChapterListWalkRepository.setFailure(entry.id, error.message ?: error::class.simpleName)
                throw error
            }
            if (result is SyncOlderChapters.Result.Finished) return
        }
    }
}
