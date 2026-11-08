package app.katari.core.archive

import app.katari.core.common.platform.PlatformFacility
import tachiyomi.core.common.storage.StorageFile

/** Opens an archive, which libarchive reads from the platform's own handle on the file. */
@PlatformFacility
fun interface ArchiveOpener {
    fun open(file: StorageFile): ArchiveReader
}

fun ArchiveOpener.openEpub(file: StorageFile): EpubReader = EpubReader(open(file))
