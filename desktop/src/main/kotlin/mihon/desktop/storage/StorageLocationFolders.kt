package mihon.desktop.storage

import dev.zacsweers.metro.Inject
import tachiyomi.core.common.storage.PathStorageFile
import tachiyomi.core.common.storage.StorageFile
import tachiyomi.domain.storage.service.StorageFolders
import tachiyomi.domain.storage.service.StoragePreferences
import java.io.IOException
import java.nio.file.Files

/**
 * The app's [StorageFolders] in the storage location the user picked: the local source's, and the one automatic backups
 * go into. Each is made when the location lacks it.
 */
@Inject
class StorageLocationFolders(private val storagePreferences: StoragePreferences) {
    /** Null until a storage location is picked, while it is no folder, or when `local` can't be made in it. */
    fun localSource(): StorageFile? = folder(StorageFolders.LOCAL_SOURCE)

    /** Null until a storage location is picked, while it is no folder, or when `autobackup` can't be made in it. */
    fun automaticBackups(): StorageFile? = folder(StorageFolders.AUTOMATIC_BACKUPS)

    private fun folder(name: String): StorageFile? {
        val location = storagePreferences.pickedStorageDirectory()?.toFolderPath()
            ?.takeIf { Files.isDirectory(it) }
            ?: return null
        return try {
            PathStorageFile(Files.createDirectories(location.resolve(name)))
        } catch (_: IOException) {
            null
        }
    }
}
