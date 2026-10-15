package mihon.presentation.kit.surface

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.LocalContentColor

/** The edge of the video or page image that a [MediaScrim] darkens. */
enum class MediaScrimEdge { Top, Bottom }

/**
 * A shade that fades from the media scrim at [edge] to nothing, laid behind controls and captions placed along that
 * edge of a video or page image so they stay readable while the image shows through. Its content takes the media
 * content colour.
 */
@Composable
fun MediaScrim(
    edge: MediaScrimEdge,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = KatariTheme.colors
    val shade = when (edge) {
        MediaScrimEdge.Top -> listOf(colors.mediaScrim, Color.Transparent)
        MediaScrimEdge.Bottom -> listOf(Color.Transparent, colors.mediaScrim)
    }
    CompositionLocalProvider(LocalContentColor provides colors.onMedia) {
        Box(modifier = modifier.background(Brush.verticalGradient(shade)), content = content)
    }
}
