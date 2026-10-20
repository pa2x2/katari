package mihon.presentation.kit.surface

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.LocalContentColor

/** The edge of the video or page image that a [MediaScrim] darkens. */
enum class MediaScrimEdge { Top, Bottom }

/** How much of the image a [MediaScrim] holds back. */
enum class MediaScrimWeight {
    /** Fades out across its content; enough for a caption or a few icons. */
    Light,

    /**
     * Stays dark behind its content and fades out past it, so rows of text and controls stay readable over bright
     * video.
     */
    Controls,
}

/**
 * A shade that fades from the media scrim at [edge] to nothing, laid behind controls and captions placed along that
 * edge of a video or page image so they stay readable while the image shows through. Its content takes the media
 * content colour.
 */
@Composable
fun MediaScrim(
    edge: MediaScrimEdge,
    modifier: Modifier = Modifier,
    weight: MediaScrimWeight = MediaScrimWeight.Light,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = KatariTheme.colors
    val fromEdge = when (weight) {
        MediaScrimWeight.Light -> arrayOf(0f to colors.mediaScrim, 1f to Color.Transparent)
        MediaScrimWeight.Controls -> arrayOf(
            0f to colors.mediaScrimEdge,
            CONTROLS_CONTENT_END to colors.mediaScrim,
            1f to Color.Transparent,
        )
    }
    val stops = when (edge) {
        MediaScrimEdge.Top -> fromEdge
        MediaScrimEdge.Bottom -> fromEdge.reversedArray().map { (at, color) -> 1f - at to color }.toTypedArray()
    }
    val fade = when (weight) {
        MediaScrimWeight.Light -> Modifier
        MediaScrimWeight.Controls -> when (edge) {
            MediaScrimEdge.Top -> Modifier.padding(bottom = KatariTheme.spacing.xxl)
            MediaScrimEdge.Bottom -> Modifier.padding(top = KatariTheme.spacing.xxl)
        }
    }
    CompositionLocalProvider(LocalContentColor provides colors.onMedia) {
        Box(modifier = modifier.background(Brush.verticalGradient(*stops)).then(fade), content = content)
    }
}

// Roughly where the content ends once the fade-out space is added past it.
private const val CONTROLS_CONTENT_END = 0.6f
