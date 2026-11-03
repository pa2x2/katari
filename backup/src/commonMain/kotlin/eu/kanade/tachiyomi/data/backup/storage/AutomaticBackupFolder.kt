package eu.kanade.tachiyomi.data.backup.storage

import tachiyomi.core.common.storage.StorageFile

/** The folder automatic backups go into, inside the storage location the user picked. */
fun interface AutomaticBackupFolder {
    /** Made when the storage location lacks it; null while there is no storage location to make it in. */
    fun directory(): StorageFile?
}
