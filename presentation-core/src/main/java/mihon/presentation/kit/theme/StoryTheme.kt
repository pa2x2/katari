package mihon.presentation.kit.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember

/**
 * Themes a story and everything over it, such as its controls, menus and dialogs, with [palette]: text inherits the
 * palette's ink, [ContentTone.Primary] is its accent and [ContentTone.Secondary] its muted colour, and filled buttons
 * are accent on the background colour.
 *
 * @param page lays text out in the story page's own type scale, for the fixed-size page itself; the controls around
 * the page keep the app's.
 */
@Composable
fun StoryTheme(
    palette: StoryPalette,
    page: Boolean = false,
    content: @Composable () -> Unit,
) {
    val inheritedScheme = MaterialTheme.colorScheme
    val scheme = remember(inheritedScheme, palette) {
        inheritedScheme
            .withReadingPalette(
                ReadingPalette(
                    background = palette.background,
                    foreground = palette.ink,
                    accent = palette.accent,
                    warning = inheritedScheme.error,
                ),
            )
            .copy(onSurfaceVariant = palette.muted, outline = palette.muted, outlineVariant = palette.faint)
    }
    val colors = remember(scheme) { KatariColors(scheme, isDark = true) }
    val inheritedTypography = MaterialTheme.typography
    val typography = remember(inheritedTypography, page) {
        if (page) storyTypography(inheritedTypography) else inheritedTypography
    }
    MaterialTheme(colorScheme = scheme, typography = typography, shapes = MaterialTheme.shapes) {
        CompositionLocalProvider(
            LocalKatariColors provides colors,
            LocalContentColor provides palette.ink,
            content = content,
        )
    }
}
