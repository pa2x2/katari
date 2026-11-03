package tachiyomi.domain.entry.interactor

import dev.zacsweers.metro.Inject
import tachiyomi.domain.entry.model.Entry

/**
 * Reads the rest of an entry's chapter list now, part after part, for work that needs the whole list, such as
 * migration carrying read progress chapter by chapter or downloading every chapter. A part that fails stops it with
 * the failure, which the walk keeps for the background walk to retry.
 */
@Inject
class FinishChapterListWalk(
    private val syncOlderChapters: SyncOlderChapters,
) {
    suspend operator fun invoke(entry: Entry) {
        do {
            val result = syncOlderChapters(entry)
        } while (result is SyncOlderChapters.Result.More)
    }
}
