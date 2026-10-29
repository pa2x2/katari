package tachiyomi.domain.storage.service

/**
 * The folders the app keeps inside the storage location the user picks, named the same on every platform so a
 * location moved between devices is read the same.
 */
object StorageFolders {
    const val AUTOMATIC_BACKUPS = "autobackup"
    const val DOWNLOADS = "downloads"
    const val LOCAL_SOURCE = "local"
}
