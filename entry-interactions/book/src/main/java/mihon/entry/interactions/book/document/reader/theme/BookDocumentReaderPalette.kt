package mihon.entry.interactions.book.document.reader.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import mihon.entry.interactions.book.document.reader.settings.BookDocumentReaderThemeMode

internal data class BookDocumentReaderPaletteValues(
    val backgroundArgb: Long,
    val foregroundArgb: Long,
)

internal fun resolveBookDocumentReaderPalette(
    mode: BookDocumentReaderThemeMode,
    appBackgroundArgb: Long,
    appForegroundArgb: Long,
): BookDocumentReaderPaletteValues = when (mode) {
    BookDocumentReaderThemeMode.APP -> BookDocumentReaderPaletteValues(
        backgroundArgb = appBackgroundArgb,
        foregroundArgb = appForegroundArgb,
    )
    BookDocumentReaderThemeMode.BLACK -> BookDocumentReaderPaletteValues(
        backgroundArgb = BLACK_BACKGROUND,
        foregroundArgb = BLACK_FOREGROUND,
    )
}

internal data class BookDocumentReaderPalette(
    val background: Color,
    val foreground: Color,
    val accent: Color,
    val warning: Color,
    val outline: Color,
    val surfaceVariant: Color,
)

internal val LocalBookDocumentReaderPalette = staticCompositionLocalOf<BookDocumentReaderPalette> {
    error("Book document reader palette is not provided")
}

@Composable
internal fun bookDocumentReaderPalette(
    mode: BookDocumentReaderThemeMode,
): BookDocumentReaderPalette {
    val scheme = MaterialTheme.colorScheme
    val values = resolveBookDocumentReaderPalette(
        mode = mode,
        appBackgroundArgb = scheme.background.toArgbLong(),
        appForegroundArgb = scheme.onBackground.toArgbLong(),
    )
    val background = Color(values.backgroundArgb.toInt())
    val foreground = Color(values.foregroundArgb.toInt())
    return BookDocumentReaderPalette(
        background = background,
        foreground = foreground,
        accent = if (mode == BookDocumentReaderThemeMode.APP) scheme.primary else foreground,
        warning = scheme.error,
        outline = foreground.copy(alpha = 0.35f),
        surfaceVariant = foreground.copy(alpha = 0.08f),
    )
}

private fun Color.toArgbLong(): Long = toArgb().toLong() and 0xFFFFFFFFL

private const val BLACK_BACKGROUND = 0xFF000000L
private const val BLACK_FOREGROUND = 0xFFE6E6E6L
