package mihon.entry.interactions.book.download

import android.content.ComponentName
import android.content.IntentFilter
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.database.MatrixCursor
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import android.provider.DocumentsProvider
import android.webkit.MimeTypeMap
import com.hippo.unifile.UniFile
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowContentResolver
import java.io.File
import java.nio.file.Files

/** Filesystem-backed document provider with Android's MIME-based filename creation. */
internal class BookDownloadDocumentsProviderFixture : DocumentsProvider() {
    private val root = Files.createTempDirectory("book-documents-provider").toFile()

    fun downloadsDirectory(): UniFile {
        val context = RuntimeEnvironment.getApplication()
        attachInfo(
            context,
            ProviderInfo().apply {
                authority = AUTHORITY
                exported = true
                grantUriPermissions = true
                readPermission = "android.permission.MANAGE_DOCUMENTS"
                writePermission = "android.permission.MANAGE_DOCUMENTS"
            },
        )
        ShadowContentResolver.registerProviderInternal(AUTHORITY, this)
        val component = ComponentName(context.packageName, javaClass.name)
        shadowOf(context.packageManager).addOrUpdateProvider(
            ProviderInfo().apply {
                authority = AUTHORITY
                packageName = component.packageName
                name = component.className
                applicationInfo = context.applicationInfo
            },
        )
        shadowOf(context.packageManager).addIntentFilterForProvider(
            component,
            IntentFilter(DocumentsContract.PROVIDER_INTERFACE),
        )
        return checkNotNull(UniFile.fromUri(context, DocumentsContract.buildTreeDocumentUri(AUTHORITY, "root")))
    }

    override fun onCreate() = true

    override fun queryRoots(projection: Array<out String>?): Cursor = MatrixCursor(projection ?: emptyArray())

    override fun queryDocument(documentId: String, projection: Array<out String>?): Cursor =
        MatrixCursor(projection ?: COLUMNS).apply { addDocument(file(documentId)) }

    override fun queryChildDocuments(
        parentDocumentId: String,
        projection: Array<out String>?,
        sortOrder: String?,
    ): Cursor = MatrixCursor(projection ?: COLUMNS).apply {
        file(parentDocumentId).listFiles().orEmpty().forEach { addDocument(it) }
    }

    override fun createDocument(parentDocumentId: String, mimeType: String, displayName: String): String {
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
        val name = if (mimeType != Document.MIME_TYPE_DIR && mimeType != "application/octet-stream" &&
            extension != null && !displayName.endsWith(".$extension")
        ) {
            "$displayName.$extension"
        } else {
            displayName
        }
        val target = file(parentDocumentId).resolve(name)
        if (mimeType == Document.MIME_TYPE_DIR) check(target.mkdir()) else check(target.createNewFile())
        return id(target)
    }

    override fun openDocument(documentId: String, mode: String, signal: CancellationSignal?): ParcelFileDescriptor =
        ParcelFileDescriptor.open(file(documentId), ParcelFileDescriptor.parseMode(mode))

    override fun deleteDocument(documentId: String) {
        check(file(documentId).deleteRecursively())
    }

    override fun renameDocument(documentId: String, displayName: String): String {
        val original = file(documentId)
        val target = checkNotNull(original.parentFile).resolve(displayName)
        check(original.renameTo(target))
        return id(target)
    }

    override fun isChildDocument(parentDocumentId: String, documentId: String): Boolean =
        documentId.startsWith("$parentDocumentId/")

    private fun file(documentId: String): File =
        if (documentId == "root") root else root.resolve(documentId.removePrefix("root/"))

    private fun id(file: File): String = if (file == root) "root" else "root/${file.relativeTo(root).path}"

    private fun MatrixCursor.addDocument(file: File) {
        check(file.exists())
        addRow(
            columnNames.map { column ->
                when (column) {
                    Document.COLUMN_DOCUMENT_ID -> id(file)
                    Document.COLUMN_DISPLAY_NAME -> file.name
                    Document.COLUMN_MIME_TYPE -> if (file.isDirectory) {
                        Document.MIME_TYPE_DIR
                    } else {
                        "application/octet-stream"
                    }
                    Document.COLUMN_SIZE -> file.length()
                    Document.COLUMN_LAST_MODIFIED -> file.lastModified()
                    Document.COLUMN_FLAGS ->
                        Document.FLAG_SUPPORTS_WRITE or Document.FLAG_SUPPORTS_DELETE or
                            Document.FLAG_SUPPORTS_RENAME or Document.FLAG_DIR_SUPPORTS_CREATE
                    else -> null
                }
            },
        )
    }

    private companion object {
        const val AUTHORITY = "katari.test.book.documents"
        val COLUMNS = arrayOf(
            Document.COLUMN_DOCUMENT_ID,
            Document.COLUMN_DISPLAY_NAME,
            Document.COLUMN_MIME_TYPE,
            Document.COLUMN_SIZE,
            Document.COLUMN_LAST_MODIFIED,
            Document.COLUMN_FLAGS,
        )
    }
}
