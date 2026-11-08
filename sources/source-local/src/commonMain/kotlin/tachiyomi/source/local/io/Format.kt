package tachiyomi.source.local.io

import tachiyomi.core.common.storage.StorageFile
import tachiyomi.core.common.storage.extension
import tachiyomi.source.local.io.Archive.isSupported as isArchiveSupported

sealed interface Format {
    data class Directory(val file: StorageFile) : Format
    data class Archive(val file: StorageFile) : Format
    data class Epub(val file: StorageFile) : Format

    class UnknownFormatException : Exception()

    companion object {

        fun valueOf(file: StorageFile) = when {
            file.isDirectory -> Directory(file)
            file.extension.equals("epub", true) -> Epub(file)
            isArchiveSupported(file) -> Archive(file)
            else -> throw UnknownFormatException()
        }
    }
}
