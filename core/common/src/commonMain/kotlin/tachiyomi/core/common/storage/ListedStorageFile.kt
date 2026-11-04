package tachiyomi.core.common.storage

/** A file as [StorageFile.listWithDetails] read it. [length] and [lastModified] are -1 where the storage didn't say. */
data class ListedStorageFile(
    val file: StorageFile,
    val name: String?,
    val isDirectory: Boolean,
    val length: Long,
    val lastModified: Long,
)
