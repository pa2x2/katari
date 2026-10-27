package mihon.entry.interactions.translate.work

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Inject
import mihon.entry.interactions.translate.notification.EntryTranslateNotifier

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
    internal val runner: EntryTranslateQueueRunner,
    internal val notifier: EntryTranslateNotifier,
    internal val conditions: EntryTranslateConditions,
)
