package mihon.desktop.storage

import dev.zacsweers.metro.Inject
import tachiyomi.core.common.storage.PathStorageFile
import tachiyomi.core.common.storage.StorageFile
import tachiyomi.domain.storage.service.StoragePreferences
import java.io.IOException
import java.nio.file.Files

/**
 * The app's folders in the storage location, as Android's storage manager names them: `local`, which the local source
 * reads, and `autobackup`, which automatic backups go into. Each is made when the location lacks it.
 */
@Inject
class StorageLocationFolders(private val storagePreferences: StoragePreferences) {
    /** Null while the storage location is no folder, or `local` can't be made in it. */
    fun localSource(): StorageFile? = folder(LOCAL_SOURCE_PATH)

    /** Null while the storage location is no folder, or `autobackup` can't be made in it. */
    fun automaticBackups(): StorageFile? = folder(AUTOMATIC_BACKUPS_PATH)

    private fun folder(name: String): StorageFile? {
        val location = storagePreferences.baseStorageDirectory.get().toFolderPath()
            ?.takeIf { Files.isDirectory(it) }
            ?: return null
        return try {
            PathStorageFile(Files.createDirectories(location.resolve(name)))
        } catch (_: IOException) {
            null
        }
    }
}

private const val LOCAL_SOURCE_PATH = "local"
private const val AUTOMATIC_BACKUPS_PATH = "autobackup"
