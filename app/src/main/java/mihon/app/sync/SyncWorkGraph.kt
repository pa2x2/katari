package mihon.app.sync

import app.katari.sync.client.trigger.SyncBackgroundRun

/** Graph entry point of the sync job, which WorkManager constructs. */
interface SyncWorkGraph {
    val syncBackgroundRun: SyncBackgroundRun
}
