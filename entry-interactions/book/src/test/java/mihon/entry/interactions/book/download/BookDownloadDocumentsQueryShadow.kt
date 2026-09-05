package mihon.entry.interactions.book.download

import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsProvider
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.RealObject

/** Bridges Robolectric's legacy resolver dispatch to the document provider's current query API. */
@Implements(DocumentsProvider::class)
internal class BookDownloadDocumentsQueryShadow {
    @RealObject
    private lateinit var provider: DocumentsProvider

    @Implementation
    fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?,
    ): Cursor? {
        check(selection == null && selectionArgs == null && sortOrder == null)
        return provider.query(uri, projection, Bundle(), null)
    }
}
