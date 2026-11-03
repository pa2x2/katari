package mihon.desktop.translation

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import mihon.desktop.work.ExistingWorkPolicy
import mihon.desktop.work.InProcessWork
import mihon.desktop.work.InProcessWorkRequest
import mihon.desktop.work.ResumableWork
import mihon.entry.interactions.translate.work.EntryTranslateRun
import mihon.entry.interactions.translate.work.EntryTranslateWorkController
import tachiyomi.domain.work.WorkOutcome

/** Drains the translation queue as [InProcessWork], one run after another, as Android's translation worker does. */
@Inject
@SingleIn(AppScope::class)
class InProcessEntryTranslateWorkController(
    private val work: InProcessWork,
    private val run: () -> EntryTranslateRun,
) : EntryTranslateWorkController, ResumableWork {
    override val kinds: Set<String> = setOf(NAME)

    override fun start() {
        // Work arriving after the running pass's last check for more is picked up by the appended one.
        work.enqueue(
            NAME,
            ExistingWorkPolicy.Append,
            InProcessWorkRequest(resumeAs = NAME) {
                run().run()
                WorkOutcome.Done
            },
        )
    }

    override suspend fun resume(kind: String, input: Map<String, String>) {
        start()
    }

    private companion object {
        const val NAME = "EntryTranslator"
    }
}
