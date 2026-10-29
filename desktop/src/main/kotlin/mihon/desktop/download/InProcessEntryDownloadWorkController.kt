package mihon.desktop.download

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import mihon.desktop.work.ExistingWorkPolicy
import mihon.desktop.work.InProcessWork
import mihon.desktop.work.InProcessWorkRequest
import mihon.desktop.work.ResumableWork
import mihon.entry.interactions.download.EntryDownloadRun
import mihon.entry.interactions.download.EntryDownloadWorkController
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.domain.work.WorkOutcome

/**
 * Runs the download queue as [InProcessWork], online and one run after another, as Android's download worker does.
 * Whether the user last asked downloads to go or to stop is kept across launches, so a queue restored at the next
 * launch only goes on when they were going.
 */
@Inject
@SingleIn(AppScope::class)
class InProcessEntryDownloadWorkController(
    private val work: InProcessWork,
    private val run: () -> EntryDownloadRun,
    preferenceStore: PreferenceStore,
) : EntryDownloadWorkController, ResumableWork {
    private val requested = preferenceStore.getBoolean(Preference.appStateKey(REQUESTED_KEY), true)

    override val kinds: Set<String> = setOf(NAME)

    override fun start() {
        requested.set(true)
        // Requests arriving after the running pass has made its final check for more run after it.
        work.enqueue(
            NAME,
            ExistingWorkPolicy.Append,
            InProcessWorkRequest(resumeAs = NAME, requiresNetwork = true) {
                run().run()
                WorkOutcome.Done
            },
        )
    }

    override fun stop() {
        requested.set(false)
        work.cancel(NAME)
    }

    override fun resumeIfRequested() {
        if (requested.get()) start()
    }

    override suspend fun resume(kind: String, input: Map<String, String>) {
        resumeIfRequested()
    }

    private companion object {
        const val NAME = "EntryDownloader"
        const val REQUESTED_KEY = "entry_download_execution_requested"
    }
}
