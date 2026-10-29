package mihon.entry.interactions.book.document.reader.font

import android.content.ContentResolver
import android.graphics.fonts.Font
import android.net.Uri
import android.provider.OpenableColumns
import mihon.entry.interactions.reader.settings.BookDocumentReaderFont
import java.io.IOException

/** Imports the document picked at [uri], kept only when Android loads it as a font. */
internal fun BookDocumentReaderFontStore.import(
    contentResolver: ContentResolver,
    profileId: Long,
    uri: Uri,
): BookDocumentReaderFont.Imported? = import(
    profileId = profileId,
    open = { contentResolver.openInputStream(uri) },
    isFont = { file ->
        try {
            Font.Builder(file).build()
            true
        } catch (_: IOException) {
            false
        } catch (_: IllegalArgumentException) {
            false
        }
    },
    displayName = {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
            ?: uri.lastPathSegment
    },
)
