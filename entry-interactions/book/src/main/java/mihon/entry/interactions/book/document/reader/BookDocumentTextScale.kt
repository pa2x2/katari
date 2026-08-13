package mihon.entry.interactions.book.document.reader

import androidx.compose.runtime.staticCompositionLocalOf

/** User-controlled scale applied to publication text while preserving document-relative typography. */
internal val LocalBookDocumentTextScale = staticCompositionLocalOf { 1f }

internal const val BOOK_DOCUMENT_BASE_TEXT_SIZE_SP = 16f
