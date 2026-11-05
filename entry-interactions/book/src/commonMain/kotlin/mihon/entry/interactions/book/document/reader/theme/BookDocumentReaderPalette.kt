package mihon.entry.interactions.book.document.reader.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import mihon.entry.interactions.reader.settings.BookDocumentReaderTheme
import mihon.presentation.kit.theme.ReadingPalette

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
