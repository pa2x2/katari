package tachiyomi.source.local.io

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import tachiyomi.core.common.storage.StorageFile
import tachiyomi.domain.storage.service.StorageManager

@Inject
@SingleIn(AppScope::class)
class LocalSourceFileSystem(private val storageManager: StorageManager) {

    /** The local source's folder in the storage location; null while there is none. */
    fun getBaseDirectory(): StorageFile? {
        return storageManager.getLocalSourceDirectory()
    }

    fun getFilesInBaseDirectory(): List<StorageFile> {
        return getBaseDirectory()?.listFiles().orEmpty()
    }

    fun getMangaDirectory(name: String): StorageFile? {
        return getBaseDirectory()
            ?.findFile(name)
            ?.takeIf { it.isDirectory }
    }

    fun getFilesInMangaDirectory(name: String): List<StorageFile> {
        return getMangaDirectory(name)?.listFiles().orEmpty()
    }
}
