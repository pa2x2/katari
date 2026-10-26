package mihon.presentation.kit.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance

/**
 * Themes a reading surface and everything over it, such as reader chrome, sheets and dialogs, with [palette]. Every
 * colour role is derived from the palette's four colours, so kit components match the page. A null [palette] keeps
 * the app's colours. Content inside starts from the page's text colour and the body text style either way.
 */
@Composable
fun ReadingPaletteTheme(
    palette: ReadingPalette?,
    content: @Composable () -> Unit,
) {
    val inheritedScheme = MaterialTheme.colorScheme
    val inheritedColors = KatariTheme.colors
    val scheme = remember(inheritedScheme, palette) {
        palette?.let(inheritedScheme::withReadingPalette)
            ?: inheritedScheme
    }
    val colors = remember(scheme, palette, inheritedColors) {
        palette?.let { KatariColors(scheme, isDark = it.background.luminance() < DARK_BACKGROUND_LUMINANCE) }
            ?: inheritedColors
    }
    MaterialTheme(
        colorScheme = scheme,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes,
    ) {
        CompositionLocalProvider(
            LocalKatariColors provides colors,
            LocalContentColor provides scheme.onBackground,
            content = content,
        )
    }
}

internal fun ColorScheme.withReadingPalette(palette: ReadingPalette): ColorScheme {
    val surfaceContainerLow = palette.foreground.compositeOver(palette.background, alpha = 0.04f)
    val surfaceContainer = palette.foreground.compositeOver(palette.background, alpha = 0.08f)
    val surfaceContainerHigh = palette.foreground.compositeOver(palette.background, alpha = 0.12f)
    val surfaceContainerHighest = palette.foreground.compositeOver(palette.background, alpha = 0.16f)
    return copy(
        primary = palette.accent,
        onPrimary = palette.background,
        primaryContainer = palette.accent.compositeOver(palette.background, alpha = 0.16f),
        onPrimaryContainer = palette.foreground,
        // Actions on inverse surfaces, such as snackbar actions, sit on the foreground colour.
        inversePrimary = palette.accent.takeIf { it.contrastWith(palette.foreground) >= MINIMUM_ACTION_CONTRAST }
            ?: palette.background,
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

internal fun Color.contrastWith(other: Color): Float {
    val lighter = maxOf(luminance(), other.luminance())
    val darker = minOf(luminance(), other.luminance())
    return (lighter + 0.05f) / (darker + 0.05f)
}

/** WCAG's minimum contrast for normal-size text. */
private const val MINIMUM_ACTION_CONTRAST = 4.5f
private const val DARK_BACKGROUND_LUMINANCE = 0.5f
