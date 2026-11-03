package mihon.presentation.kit.content

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.LocalContentColor
import mihon.presentation.kit.theme.ReadingPalette
import mihon.presentation.kit.theme.TextRole

/** A few lines of reading text on a page in [palette]'s colours, previewing colours before they are chosen. */
@Composable
fun ReadingPageSample(palette: ReadingPalette, text: String, modifier: Modifier = Modifier) {
    val shape = KatariTheme.shapes.medium
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(palette.background)
            .border(KatariTheme.sizes.border, KatariTheme.colors.outlineVariant, shape)
            .padding(KatariTheme.spacing.l),
    ) {
        CompositionLocalProvider(LocalContentColor provides palette.foreground) {
            Text(text, role = TextRole.Body)
        }
    }
}

/** A small page in [palette]'s colours with "Aa" on it, marking a choice of reading colours in a list. */
@Composable
fun ReadingPaletteSwatch(palette: ReadingPalette, modifier: Modifier = Modifier) {
    val shape = KatariTheme.shapes.small
    Box(
        modifier = modifier
            .size(KatariTheme.sizes.iconLarge)
            .clip(shape)
            .background(palette.background)
            .border(KatariTheme.sizes.border, KatariTheme.colors.outlineVariant, shape),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides palette.foreground) {
            Text(SWATCH_SAMPLE, role = TextRole.Label)
        }
    }
}

// The usual sample of a typeface or text colour; not translated, like a font's own specimen.
private const val SWATCH_SAMPLE = "Aa"
