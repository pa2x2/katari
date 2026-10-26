package mihon.entry.interactions.book.document.reader.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.graphics.ColorUtils
import mihon.entry.interactions.reader.settings.BookDocumentReaderTheme
import mihon.presentation.kit.theme.ReadingPalette

/** Null for the app theme, whose own light or dark look decides. */
internal fun bookDocumentReaderThemeHasDarkBackground(theme: BookDocumentReaderTheme): Boolean? = when (theme) {
    BookDocumentReaderTheme.App -> null
    BookDocumentReaderTheme.Paper -> false
    BookDocumentReaderTheme.Dusk,
    BookDocumentReaderTheme.Black,
    -> true
    is BookDocumentReaderTheme.Custom -> ColorUtils.calculateLuminance(theme.background) < DARK_PAGE_LUMINANCE
}

internal val LocalBookDocumentReaderPalette = staticCompositionLocalOf<ReadingPalette> {
    error("Book document reader palette is not provided")
}

@Composable
@ReadOnlyComposable
internal fun bookDocumentReaderPalette(theme: BookDocumentReaderTheme): ReadingPalette = when (theme) {
    BookDocumentReaderTheme.App -> ReadingPalette.App
    BookDocumentReaderTheme.Paper -> ReadingPalette.Paper
    BookDocumentReaderTheme.Dusk -> ReadingPalette.Dusk
    BookDocumentReaderTheme.Black -> ReadingPalette.Black
    is BookDocumentReaderTheme.Custom -> ReadingPalette.custom(theme.background, theme.text)
}

private const val DARK_PAGE_LUMINANCE = 0.5
