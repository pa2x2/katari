package tachiyomi.source.local.io

import tachiyomi.core.common.storage.StorageFile

/** [baseDirectory] is the local source's folder in the storage location; null while there is none. */
class LocalSourceFileSystem(
    private val baseDirectory: () -> StorageFile?,
) {

    fun getBaseDirectory(): StorageFile? {
        return baseDirectory()
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
