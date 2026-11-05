package mihon.entry.interactions.book.document.reader

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import mihon.entry.interactions.book.document.resource.BookDocumentResourceDecoding
import mihon.entry.interactions.book.preparation.BookPublicationResourceLoader

/** The open section's images and fonts; null where a block is drawn without its publication. */
internal val LocalBookDocumentResourceLoader = compositionLocalOf<BookPublicationResourceLoader?> { null }

/** How the platform decodes what [LocalBookDocumentResourceLoader] loads; the reader's host provides it. */
internal val LocalBookDocumentResourceDecoding = staticCompositionLocalOf<BookDocumentResourceDecoding> {
    error("Book document resource decoding is not provided")
}
