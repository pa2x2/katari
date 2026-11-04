package mihon.core.archive

import android.content.Context
import android.net.Uri
import tachiyomi.core.common.storage.StorageFile
import java.io.FileNotFoundException

/** Maps the file through the descriptor the content resolver opens, for plain files and documents alike. */
class AndroidArchiveOpener(private val context: Context) : ArchiveOpener {
    override fun open(file: StorageFile): ArchiveReader =
        context.contentResolver.openFileDescriptor(Uri.parse(file.uri), "r")
            ?.use(::ArchiveReader)
            ?: throw FileNotFoundException("No descriptor opens the archive ${file.displayablePath}")
}
