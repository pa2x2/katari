package app.katari.android.sync

import app.katari.sync.client.trigger.SyncTriggers

/** Graph entry points of [InstantSyncService], which Android constructs. */
interface InstantSyncGraph {
    val syncTriggers: SyncTriggers
    val instantSyncNotification: InstantSyncNotification
}
