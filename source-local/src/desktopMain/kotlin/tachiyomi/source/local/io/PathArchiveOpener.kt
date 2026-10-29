package tachiyomi.source.local.io

import mihon.core.archive.ArchiveReader
import tachiyomi.core.common.storage.StorageFile
import tachiyomi.core.common.storage.toPath

object PathArchiveOpener : ArchiveOpener {
    override fun open(file: StorageFile): ArchiveReader = ArchiveReader(file.toPath())
}
