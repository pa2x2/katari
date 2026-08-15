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
    val accentArgb: Long,
    val warningArgb: Long,
)

internal fun resolveBookDocumentReaderPalette(
    mode: BookDocumentReaderThemeMode,
    appBackgroundArgb: Long,
    appForegroundArgb: Long,
    appAccentArgb: Long = appForegroundArgb,
    appWarningArgb: Long = appForegroundArgb,
): BookDocumentReaderPaletteValues = when (mode) {
    BookDocumentReaderThemeMode.APP -> BookDocumentReaderPaletteValues(
        backgroundArgb = appBackgroundArgb,
        foregroundArgb = appForegroundArgb,
        accentArgb = appAccentArgb,
        warningArgb = appWarningArgb,
    )
    BookDocumentReaderThemeMode.PAPER -> BookDocumentReaderPaletteValues(
        backgroundArgb = PAPER_BACKGROUND,
        foregroundArgb = PAPER_FOREGROUND,
        accentArgb = PAPER_ACCENT,
        warningArgb = PAPER_WARNING,
    )
    BookDocumentReaderThemeMode.DUSK -> BookDocumentReaderPaletteValues(
        backgroundArgb = DUSK_BACKGROUND,
        foregroundArgb = DUSK_FOREGROUND,
        accentArgb = DUSK_ACCENT,
        warningArgb = DUSK_WARNING,
    )
    BookDocumentReaderThemeMode.BLACK -> BookDocumentReaderPaletteValues(
        backgroundArgb = BLACK_BACKGROUND,
        foregroundArgb = BLACK_FOREGROUND,
        accentArgb = BLACK_FOREGROUND,
        warningArgb = appWarningArgb,
    )
}

internal fun bookDocumentReaderThemeHasDarkBackground(
    mode: BookDocumentReaderThemeMode,
): Boolean? = when (mode) {
    BookDocumentReaderThemeMode.APP -> null
    BookDocumentReaderThemeMode.PAPER -> false
    BookDocumentReaderThemeMode.DUSK,
    BookDocumentReaderThemeMode.BLACK,
    -> true
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
        appAccentArgb = scheme.primary.toArgbLong(),
        appWarningArgb = scheme.error.toArgbLong(),
    )
    val background = Color(values.backgroundArgb.toInt())
    val foreground = Color(values.foregroundArgb.toInt())
    return BookDocumentReaderPalette(
        background = background,
        foreground = foreground,
        accent = Color(values.accentArgb.toInt()),
        warning = Color(values.warningArgb.toInt()),
        outline = foreground.copy(alpha = 0.35f),
        surfaceVariant = foreground.copy(alpha = 0.08f),
    )
}

private fun Color.toArgbLong(): Long = toArgb().toLong() and 0xFFFFFFFFL

private const val BLACK_BACKGROUND = 0xFF000000L
private const val BLACK_FOREGROUND = 0xFFE6E6E6L
private const val PAPER_BACKGROUND = 0xFFF5EEDCL
private const val PAPER_FOREGROUND = 0xFF2B2721L
private const val PAPER_ACCENT = 0xFF735C2EL
private const val PAPER_WARNING = 0xFF9F3D37L
private const val DUSK_BACKGROUND = 0xFF1E1B18L
private const val DUSK_FOREGROUND = 0xFFE3DACAL
private const val DUSK_ACCENT = 0xFFD5A85BL
private const val DUSK_WARNING = 0xFFFFB4ABL
