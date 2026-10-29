package mihon.desktop.storage

import dev.zacsweers.metro.Inject
import tachiyomi.core.common.storage.PathStorageFile
import tachiyomi.core.common.storage.StorageFile
import tachiyomi.domain.storage.service.StoragePreferences
import java.io.IOException
import java.nio.file.Files

/** The `local` folder in the storage location, which the local source reads; made when the location lacks it. */
@Inject
class LocalSourceFolder(private val storagePreferences: StoragePreferences) {
    /** Null while the storage location is no folder, or the `local` one can't be made in it. */
    fun directory(): StorageFile? {
        val location = storagePreferences.baseStorageDirectory.get().toFolderPath()
            ?.takeIf { Files.isDirectory(it) }
            ?: return null
        return try {
            PathStorageFile(Files.createDirectories(location.resolve(LOCAL_SOURCE_PATH)))
        } catch (_: IOException) {
            null
        }
    }
}

private const val LOCAL_SOURCE_PATH = "local"
