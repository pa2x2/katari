package mihon.entry.interactions.persistence.backup

/**
 * This install's key for each source key a backup was written with. Sources whose IDs aren't global get their keys per
 * install, so feature state that names a source must translate its keys through this before resolving them.
 */
fun interface EntryBackupSourceKeys {
    fun localKey(backupKey: Long): Long
}
