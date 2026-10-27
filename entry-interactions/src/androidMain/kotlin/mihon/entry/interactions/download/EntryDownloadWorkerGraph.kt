package mihon.entry.interactions.download

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import tachiyomi.domain.download.service.DownloadPreferences

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
    internal val downloads: EntryDownloadRuntimeCoordinator,
    internal val downloadPreferences: DownloadPreferences,
    internal val notificationProvider: EntryDownloadForegroundNotificationProvider,
)
