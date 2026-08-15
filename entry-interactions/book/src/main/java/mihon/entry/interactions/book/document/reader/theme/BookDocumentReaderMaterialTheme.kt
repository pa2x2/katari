package mihon.entry.interactions.book.document.reader.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import mihon.entry.interactions.book.document.reader.settings.BookDocumentReaderThemeMode

/** Applies the active BOOK palette to every Material surface in the reading session. */
@Composable
internal fun BookDocumentReaderMaterialTheme(
    mode: BookDocumentReaderThemeMode,
    palette: BookDocumentReaderPalette,
    content: @Composable () -> Unit,
) {
    val inheritedColorScheme = MaterialTheme.colorScheme
    val colorScheme = remember(inheritedColorScheme, mode, palette) {
        if (mode == BookDocumentReaderThemeMode.APP) {
            inheritedColorScheme
        } else {
            inheritedColorScheme.withBookDocumentReaderPalette(palette)
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides colorScheme.onBackground,
            content = content,
        )
    }
}

private fun ColorScheme.withBookDocumentReaderPalette(
    palette: BookDocumentReaderPalette,
): ColorScheme {
    val surfaceContainerLow = palette.foreground.compositeOver(palette.background, alpha = 0.04f)
    val surfaceContainer = palette.foreground.compositeOver(palette.background, alpha = 0.08f)
    val surfaceContainerHigh = palette.foreground.compositeOver(palette.background, alpha = 0.12f)
    val surfaceContainerHighest = palette.foreground.compositeOver(palette.background, alpha = 0.16f)
    return copy(
        primary = palette.accent,
        onPrimary = palette.background,
        primaryContainer = palette.accent.compositeOver(palette.background, alpha = 0.16f),
        onPrimaryContainer = palette.foreground,
        inversePrimary = palette.accent,
        secondary = palette.accent,
        onSecondary = palette.background,
        secondaryContainer = surfaceContainer,
        onSecondaryContainer = palette.foreground,
        tertiary = palette.accent,
        onTertiary = palette.background,
        tertiaryContainer = palette.accent.compositeOver(palette.background, alpha = 0.12f),
        onTertiaryContainer = palette.foreground,
        background = palette.background,
        onBackground = palette.foreground,
        surface = palette.background,
        onSurface = palette.foreground,
        surfaceVariant = surfaceContainer,
        onSurfaceVariant = palette.foreground.copy(alpha = 0.8f),
        surfaceTint = palette.accent,
        inverseSurface = palette.foreground,
        inverseOnSurface = palette.background,
        error = palette.warning,
        onError = palette.background,
        errorContainer = palette.warning.compositeOver(palette.background, alpha = 0.16f),
        onErrorContainer = palette.foreground,
        outline = palette.outline,
        outlineVariant = palette.foreground.copy(alpha = 0.18f),
        surfaceBright = surfaceContainerHighest,
        surfaceDim = palette.background,
        surfaceContainerLowest = palette.background,
        surfaceContainerLow = surfaceContainerLow,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh,
        surfaceContainerHighest = surfaceContainerHighest,
    )
}

private fun Color.compositeOver(background: Color, alpha: Float): Color =
    copy(alpha = alpha).compositeOver(background)
