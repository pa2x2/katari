package tachiyomi.source.local.io

import mihon.core.archive.ArchiveReader
import mihon.core.common.platform.PlatformFacility
import tachiyomi.core.common.storage.StorageFile

/** Opens an archive chapter, which libarchive reads from the platform's own handle on the file. */
@PlatformFacility
fun interface ArchiveOpener {
    fun open(file: StorageFile): ArchiveReader
}
