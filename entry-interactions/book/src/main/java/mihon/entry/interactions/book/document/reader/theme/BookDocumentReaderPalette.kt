package mihon.entry.interactions.book.document.reader.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import mihon.entry.interactions.book.document.reader.settings.BookDocumentReaderThemeMode
import mihon.presentation.kit.theme.ReadingPalette

internal fun bookDocumentReaderThemeHasDarkBackground(
    mode: BookDocumentReaderThemeMode,
): Boolean? = when (mode) {
    BookDocumentReaderThemeMode.APP -> null
    BookDocumentReaderThemeMode.PAPER -> false
    BookDocumentReaderThemeMode.DUSK,
    BookDocumentReaderThemeMode.BLACK,
    -> true
}

internal val LocalBookDocumentReaderPalette = staticCompositionLocalOf<ReadingPalette> {
    error("Book document reader palette is not provided")
}

@Composable
@ReadOnlyComposable
internal fun bookDocumentReaderPalette(
    mode: BookDocumentReaderThemeMode,
): ReadingPalette = when (mode) {
    BookDocumentReaderThemeMode.APP -> ReadingPalette.App
    BookDocumentReaderThemeMode.PAPER -> ReadingPalette.Paper
    BookDocumentReaderThemeMode.DUSK -> ReadingPalette.Dusk
    BookDocumentReaderThemeMode.BLACK -> ReadingPalette.Black
}
