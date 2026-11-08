package eu.kanade.tachiyomi.data.backup.restore

/**
 * How far a restore reaches when this device syncs (D046, D046a). On a device that doesn't sync both only run the
 * restore.
 */
interface SyncedRestore {
    /**
     * Runs [restore] so that what it writes counts as changed at [backupCreatedAt] (epoch milliseconds): an edit made
     * on any device after the backup survives, and the backup fills in only what is older or missing.
     */
    suspend fun <T> onThisDevice(backupCreatedAt: Long, restore: suspend () -> T): T

    /**
     * Empties the person's data in [profileIds] and runs [restore], both as changes made now, so every device of the
     * account returns to the backup in those profiles.
     */
    suspend fun <T> rollingBackAccount(profileIds: Set<Long>, restore: suspend () -> T): T
}
