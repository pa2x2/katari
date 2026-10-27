package tachiyomi.core.common.storage

import android.content.Context
import android.database.Cursor
import android.provider.DocumentsContract
import com.hippo.unifile.UniFile

/**
 * Lists a directory's files with their type, size and modification time in a fixed number of storage queries. A file
 * in an Android documents tree asks its provider again for each of those, so reading them file by file costs a query
 * per file and property: thousands for a folder of video segments.
 */
class DirectoryListing(
    private val context: Context? = null,
) {
    fun list(directory: UniFile): List<ListedFile> {
        return listDocuments(directory) ?: listWithUniFile(directory)
    }

    private fun listDocuments(directory: UniFile): List<ListedFile>? {
        val context = context ?: return null
        val directoryUri = directory.uri
        if (!UniFile.isTreeDocumentUri(context, directoryUri)) return null
        return runCatching {
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                directoryUri,
                DocumentsContract.getDocumentId(directoryUri),
            )
            val metadataById = context.contentResolver.query(childrenUri, DOCUMENT_PROJECTION, null, null, null)
                ?.use { cursor ->
                    buildMap<String, DocumentMetadata> {
                        while (cursor.moveToNext()) put(cursor.getString(DOCUMENT_ID_COLUMN), cursor.metadata())
                    }
                }
                ?: return null
            // The directory makes its files itself, without the provider lookups UniFile.fromUri does for each one.
            directory.listFiles().orEmpty().mapNotNull { file ->
                val metadata = metadataById[DocumentsContract.getDocumentId(file.uri)] ?: return@mapNotNull null
                ListedFile(
                    file = file,
                    name = metadata.name,
                    isDirectory = metadata.isDirectory,
                    length = metadata.length,
                    lastModified = metadata.lastModified,
                )
            }
        }.getOrNull()
    }

    private fun listWithUniFile(directory: UniFile): List<ListedFile> =
        directory.listFiles().orEmpty().map { file ->
            val isDirectory = file.isDirectory
            ListedFile(
                file = file,
                name = file.name,
                isDirectory = isDirectory,
                length = if (isDirectory) UNKNOWN_LENGTH else file.length(),
                lastModified = file.lastModified(),
            )
        }

    private fun Cursor.metadata() = DocumentMetadata(
        name = getString(DISPLAY_NAME_COLUMN),
        isDirectory = getString(MIME_TYPE_COLUMN) == DocumentsContract.Document.MIME_TYPE_DIR,
        length = longOrUnknown(SIZE_COLUMN),
        lastModified = longOrUnknown(LAST_MODIFIED_COLUMN),
    )

    private fun Cursor.longOrUnknown(column: Int): Long =
        if (isNull(column)) UNKNOWN_LENGTH else getLong(column)

    private class DocumentMetadata(
        val name: String?,
        val isDirectory: Boolean,
        val length: Long,
        val lastModified: Long,
    )

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

/** [length] and [lastModified] are -1 when the storage doesn't know them. */
data class ListedFile(
    val file: UniFile,
    val name: String?,
    val isDirectory: Boolean,
    val length: Long,
    val lastModified: Long,
)
