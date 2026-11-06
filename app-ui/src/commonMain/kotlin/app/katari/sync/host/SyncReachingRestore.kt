package app.katari.sync.host

import app.katari.sync.client.restore.AccountRollback
import app.katari.sync.client.restore.RestoreStamping
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.data.backup.restore.SyncedRestore

/** Backup restores reaching the sync account through the sync client's stamping and rollback. */
@Inject
@ContributesBinding(AppScope::class)
class SyncReachingRestore(
    private val stamping: RestoreStamping,
    private val rollback: AccountRollback,
) : SyncedRestore {
    override suspend fun <T> onThisDevice(backupCreatedAt: Long, restore: suspend () -> T): T =
        stamping.restoreOnThisDevice(backupCreatedAt, restore)

    override suspend fun <T> rollingBackAccount(profileIds: Set<Long>, restore: suspend () -> T): T =
        rollback.rollBack(profileIds, restore)
}
