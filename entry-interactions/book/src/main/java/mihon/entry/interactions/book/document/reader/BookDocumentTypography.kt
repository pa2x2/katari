package mihon.entry.interactions.book.document.reader

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import mihon.book.api.document.BookDocumentAlignment
import mihon.book.api.document.BookDocumentBlock
import mihon.book.api.document.BookDocumentBlockKind
import mihon.entry.interactions.reader.settings.BookDocumentLineSpacing
import mihon.entry.interactions.reader.settings.BookDocumentMargins
import mihon.entry.interactions.reader.settings.BookDocumentTextAlignment
import mihon.presentation.kit.content.ReadingMargin
import mihon.presentation.kit.content.ReadingMetrics

/**
 * The reader's typography settings over the book's own. [textScale] scales all publication text while keeping its
 * document-relative sizes; the other settings replace the book's values for body text unless they are BOOK. Whatever
 * draws text and whatever measures it off-screen for page breaks read the same value, so pages fit what is drawn.
 */
@Immutable
internal data class BookDocumentTypography(
    val textScale: Float = 1f,
    val lineSpacing: BookDocumentLineSpacing = BookDocumentLineSpacing.BOOK,
    val margins: BookDocumentMargins = BookDocumentMargins.BOOK,
    val alignment: BookDocumentTextAlignment = BookDocumentTextAlignment.BOOK,
) {
    val blockInset: Dp
        get() = ReadingMetrics.blockInset(
            when (margins) {
                BookDocumentMargins.BOOK -> ReadingMargin.Standard
                BookDocumentMargins.NARROW -> ReadingMargin.Narrow
                BookDocumentMargins.NORMAL -> ReadingMargin.Normal
                BookDocumentMargins.WIDE -> ReadingMargin.Wide
            },
        )

    fun lineHeightScale(block: BookDocumentBlock): Float = when {
        block.role.kind == BookDocumentBlockKind.HEADING -> block.style.lineHeightScale
        else -> when (lineSpacing) {
            BookDocumentLineSpacing.BOOK -> block.style.lineHeightScale
            BookDocumentLineSpacing.TIGHT -> 1.3f
            BookDocumentLineSpacing.NORMAL -> 1.5f
            BookDocumentLineSpacing.LOOSE -> 1.8f
        }
    }

    /** Null when neither the book nor the reader aligns [block], which reads as start-aligned. */
    fun alignment(block: BookDocumentBlock): BookDocumentAlignment? {
        val own = block.style.alignment
        val overridable = own == null || own == BookDocumentAlignment.START || own == BookDocumentAlignment.JUSTIFY
        if (block.role.kind == BookDocumentBlockKind.HEADING || !overridable) return own
        return when (alignment) {
            BookDocumentTextAlignment.BOOK -> own
            BookDocumentTextAlignment.LEFT -> BookDocumentAlignment.START
            BookDocumentTextAlignment.JUSTIFY -> BookDocumentAlignment.JUSTIFY
        }
    }
}

internal val LocalBookDocumentTypography = staticCompositionLocalOf { BookDocumentTypography() }

internal const val BOOK_DOCUMENT_BASE_TEXT_SIZE_SP = 16f
