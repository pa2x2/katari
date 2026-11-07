package mihon.app.sync

import app.katari.sync.client.status.SyncStatusFlow
import app.katari.sync.client.trigger.SyncBackgroundRun

/** Graph entry points of the sync job, which WorkManager constructs. */
interface SyncWorkGraph {
    val syncBackgroundRun: SyncBackgroundRun
    val syncStatusFlow: SyncStatusFlow
    val syncFirstDownloadNotification: SyncFirstDownloadNotification
}
