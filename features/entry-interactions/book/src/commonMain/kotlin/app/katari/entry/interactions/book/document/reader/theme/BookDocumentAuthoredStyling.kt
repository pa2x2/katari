package app.katari.entry.interactions.book.document.reader.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import app.katari.presentation.kit.theme.ReadingPalette
import app.katari.presentation.kit.theme.documentInheritedTextColor
import app.katari.presentation.kit.theme.documentLineColor
import app.katari.presentation.kit.theme.documentShadowColor
import app.katari.presentation.kit.theme.documentSurfaceColor
import app.katari.presentation.kit.theme.documentTextColor

/**
 * Whether the reader draws a book's own colours, boxes and borders, the Publisher styles setting, and how: on the
 * reading palette, as DocumentColors.kt describes. A `surface` is the colour behind what is drawn: unspecified for the
 * page, or a background the book paints. Disabled, every authored colour is unspecified and boxes take no room.
 */
@Immutable
internal data class BookDocumentAuthoredStyling(val palette: ReadingPalette, val enabled: Boolean) {
    /** A background the book paints, or the border or shadow of a box that paints one. */
    fun surface(argb: Long?): Color = if (enabled && argb != null) documentSurfaceColor(argb) else Color.Unspecified

    fun line(argb: Long?, surface: Color): Color =
        if (enabled && argb != null) palette.documentLineColor(argb, surface) else Color.Unspecified

    fun shadowColor(argb: Long?, surface: Color): Color =
        if (enabled && argb != null) palette.documentShadowColor(argb, surface) else Color.Unspecified

    fun text(argb: Long?, surface: Color): Color =
        if (enabled && argb != null) palette.documentTextColor(argb, surface) else Color.Unspecified

    /** Text the book leaves uncoloured, in the [inherited] colour, kept readable on [surface]. */
    fun inheritedText(inherited: Color, surface: Color): Color =
        if (enabled) palette.documentInheritedTextColor(inherited, surface) else inherited
}

internal val LocalBookDocumentAuthoredStyling = staticCompositionLocalOf<BookDocumentAuthoredStyling> {
    error("Book document authored styling is not provided")
}

/** The surface behind a block's text: the innermost background the book paints around it, or unspecified: the page. */
internal val LocalBookDocumentTextBackdrop = staticCompositionLocalOf { Color.Unspecified }
