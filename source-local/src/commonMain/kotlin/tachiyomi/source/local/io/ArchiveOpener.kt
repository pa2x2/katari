package tachiyomi.source.local.io

import mihon.core.archive.ArchiveReader
import tachiyomi.core.common.storage.StorageFile

/** Opens an archive chapter, which libarchive reads from the platform's own handle on the file. */
fun interface ArchiveOpener {
    fun open(file: StorageFile): ArchiveReader
}
