package mihon.presentation.kit.surface

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import mihon.presentation.kit.theme.KatariTheme

/**
 * The media background shown where a page or video is still loading or failed to. [image] draws a preview of it, such
 * as a thumbnail, with the modifier it is given; the preview is blurred and shaded so controls and messages laid on
 * top stay readable.
 */
@Composable
fun MediaBackdrop(
    modifier: Modifier = Modifier,
    image: (@Composable (Modifier) -> Unit)? = null,
) {
    Box(modifier = modifier.background(KatariTheme.colors.media)) {
        if (image != null) {
            image(Modifier.fillMaxSize().blur(MediaBackdropBlur))
            Box(modifier = Modifier.fillMaxSize().background(KatariTheme.colors.mediaScrim))
        }
    }
}

private val MediaBackdropBlur = 12.dp
