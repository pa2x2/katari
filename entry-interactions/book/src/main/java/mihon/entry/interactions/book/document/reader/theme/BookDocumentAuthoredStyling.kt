package mihon.entry.interactions.book.document.reader.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import mihon.presentation.kit.theme.ReadingPalette
import mihon.presentation.kit.theme.documentColor
import mihon.presentation.kit.theme.documentTextColor

/**
 * Whether the reader draws a book's own colours, boxes and borders, the Publisher styles setting, and how: fitted to
 * the reading palette. Disabled, every authored colour is unspecified and boxes take no room.
 */
@Immutable
internal data class BookDocumentAuthoredStyling(val palette: ReadingPalette, val enabled: Boolean) {
    fun fill(argb: Long?): Color = if (enabled && argb != null) palette.documentColor(argb) else Color.Unspecified

    /** A text colour kept readable on [backdrop], the fitted colour behind the text. */
    fun text(argb: Long?, backdrop: Color): Color =
        if (enabled && argb != null) palette.documentTextColor(argb, backdrop) else Color.Unspecified
}

internal val LocalBookDocumentAuthoredStyling = staticCompositionLocalOf<BookDocumentAuthoredStyling> {
    error("Book document authored styling is not provided")
}

/** The fitted colour behind a block's text: the innermost box background around it, or the page. */
internal val LocalBookDocumentTextBackdrop = staticCompositionLocalOf { Color.Unspecified }
