package mihon.desktop.source

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import mihon.desktop.work.ExistingWorkPolicy
import mihon.desktop.work.InProcessWork
import mihon.desktop.work.InProcessWorkRequest
import mihon.desktop.work.ResumableWork
import tachiyomi.domain.entry.interactor.WalkPendingChapterLists
import tachiyomi.domain.entry.service.ChapterListWalkScheduler

/** Reads the older parts of chapter lists as [InProcessWork], online and one walk run after another. */
@Inject
@SingleIn(AppScope::class)
class InProcessChapterListWalkScheduler(
    private val work: InProcessWork,
    // Reading a part of a chapter list can start another walk, which schedules this.
    private val walkPendingChapterLists: () -> WalkPendingChapterLists,
) : ChapterListWalkScheduler, ResumableWork {
    override val kinds: Set<String> = setOf(NAME)

    override fun schedule(restart: Boolean) {
        work.enqueue(
            NAME,
            if (restart) ExistingWorkPolicy.Replace else ExistingWorkPolicy.Append,
            InProcessWorkRequest(
                resumeAs = NAME,
                requiresNetwork = true,
                backoff = WalkPendingChapterLists.BACKOFF,
            ) { run -> walkPendingChapterLists().await(run.attempt) },
        )
    }

    override suspend fun resume(kind: String, input: Map<String, String>) {
        schedule(restart = false)
    }

    private companion object {
        const val NAME = "ChapterListWalk"
    }
}
