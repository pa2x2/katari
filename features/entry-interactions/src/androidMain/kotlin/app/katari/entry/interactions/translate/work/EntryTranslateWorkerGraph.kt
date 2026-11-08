package app.katari.entry.interactions.translate.work

import app.katari.entry.interactions.translate.notification.EntryTranslateNotifier
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Inject

/**
 * Graph entry point of the translation worker, which WorkManager constructs. The queue it drains is internal to this
 * module, which a contributed graph interface can't expose, so the graph hands out [EntryTranslateWorkerDependencies].
 */
@ContributesTo(AppScope::class)
interface EntryTranslateWorkerGraph {
    val entryTranslateWorkerDependencies: EntryTranslateWorkerDependencies
}

@Inject
class EntryTranslateWorkerDependencies internal constructor(
    internal val run: EntryTranslateRun,
    internal val notifier: EntryTranslateNotifier,
)
