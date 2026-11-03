package tachiyomi.source.local.io

import android.content.Context
import mihon.core.archive.ArchiveReader
import mihon.core.archive.archiveReader
import tachiyomi.core.common.storage.StorageFile
import tachiyomi.core.common.storage.toUniFile

class UniFileArchiveOpener(private val context: Context) : ArchiveOpener {
    override fun open(file: StorageFile): ArchiveReader = file.toUniFile().archiveReader(context)
}
