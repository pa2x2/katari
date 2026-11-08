package tachiyomi.source.local.io

import tachiyomi.core.common.storage.StorageFile
import tachiyomi.core.common.storage.extension

object Archive {

    private val SUPPORTED_ARCHIVE_TYPES = listOf("zip", "cbz", "rar", "cbr", "7z", "cb7", "tar", "cbt")

    fun isSupported(file: StorageFile): Boolean {
        return file.extension?.lowercase() in SUPPORTED_ARCHIVE_TYPES
    }
}
