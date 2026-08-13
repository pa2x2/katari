package mihon.entry.interactions.book.download

import android.content.Context
import android.provider.DocumentsContract
import com.hippo.unifile.UniFile

/** Lists child metadata in one provider query when the storage root is backed by Android documents. */
internal class BookDownloadDirectoryListing(
    private val context: Context? = null,
) {
    fun list(directory: UniFile): List<BookDownloadDirectoryEntry> {
        return listDocuments(directory) ?: listWithUniFile(directory)
    }

    private fun listDocuments(directory: UniFile): List<BookDownloadDirectoryEntry>? {
        val context = context ?: return null
        val directoryUri = directory.uri
        if (!UniFile.isTreeDocumentUri(context, directoryUri)) return null
        return runCatching {
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                directoryUri,
                DocumentsContract.getDocumentId(directoryUri),
            )
            context.contentResolver.query(childrenUri, DOCUMENT_PROJECTION, null, null, null)?.use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        val documentId = cursor.getString(DOCUMENT_ID_COLUMN)
                        val documentUri = DocumentsContract.buildDocumentUriUsingTree(directoryUri, documentId)
                        val file = UniFile.fromUri(context, documentUri) ?: continue
                        val type = cursor.getString(MIME_TYPE_COLUMN)
                        add(
                            BookDownloadDirectoryEntry(
                                file = file,
                                name = cursor.getString(DISPLAY_NAME_COLUMN),
                                isDirectory = type == DocumentsContract.Document.MIME_TYPE_DIR,
                                length = cursor.longOrUnknown(SIZE_COLUMN),
                                lastModified = cursor.longOrUnknown(LAST_MODIFIED_COLUMN),
                            ),
                        )
                    }
                }
            }
        }.getOrNull()
    }

    private fun listWithUniFile(directory: UniFile): List<BookDownloadDirectoryEntry> =
        directory.listFiles().orEmpty().map { file ->
            val isDirectory = file.isDirectory
            BookDownloadDirectoryEntry(
                file = file,
                name = file.name,
                isDirectory = isDirectory,
                length = if (isDirectory) UNKNOWN_LENGTH else file.length(),
                lastModified = file.lastModified(),
            )
        }

    private fun android.database.Cursor.longOrUnknown(column: Int): Long =
        if (isNull(column)) UNKNOWN_LENGTH else getLong(column)

    private companion object {
        const val DOCUMENT_ID_COLUMN = 0
        const val DISPLAY_NAME_COLUMN = 1
        const val MIME_TYPE_COLUMN = 2
        const val SIZE_COLUMN = 3
        const val LAST_MODIFIED_COLUMN = 4
        const val UNKNOWN_LENGTH = -1L

        val DOCUMENT_PROJECTION = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        )
    }
}

internal data class BookDownloadDirectoryEntry(
    val file: UniFile,
    val name: String?,
    val isDirectory: Boolean,
    val length: Long,
    val lastModified: Long,
)
