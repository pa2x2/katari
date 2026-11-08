package app.katari.entry.interactions.download

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo

/**
 * Graph entry points of the download workers, which WorkManager constructs. The runtime they drive is internal to this
 * module, which a contributed graph interface can't expose, so the graph hands out [EntryDownloadWorkerDependencies].
 */
@ContributesTo(AppScope::class)
interface EntryDownloadWorkerGraph {
    val entryDownloadWorkController: EntryDownloadWorkController
    val entryDownloadWorkerDependencies: EntryDownloadWorkerDependencies
}

class EntryDownloadWorkerDependencies internal constructor(
    internal val run: EntryDownloadRun,
    internal val notificationProvider: EntryDownloadForegroundNotificationProvider,
)
