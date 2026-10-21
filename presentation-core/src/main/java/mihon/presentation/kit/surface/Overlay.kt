package mihon.presentation.kit.surface

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import mihon.presentation.kit.theme.KatariTheme

/** What an overlay floats over, which sets its fill. */
enum class OverlayStyle {
    /** The app's own full-screen views, such as the reader or a cover viewer: on the chrome colour. */
    Chrome,

    /** Media such as a video or artwork: on a translucent shade, with media content colours. */
    Scrim,

    /** Anything, opaquely: on the surface colour, for a message that must read whatever the page shows. */
    Solid,

    /** Anything, raised on the high surface with a shadow, such as a handle beside a side panel. */
    Raised,
}

/**
 * A pill floating over a full-screen view or media, holding controls or a label, such as a cover viewer's actions, a
 * player's play button or a lock mark on an avatar. Clickable when [onClick] is set.
 */
@Composable
fun OverlayPill(
    modifier: Modifier = Modifier,
    style: OverlayStyle = OverlayStyle.Chrome,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    Overlay(modifier, style, KatariTheme.shapes.full, onClick, enabled, content)
}

/**
 * A rounded panel floating over a full-screen view or media, holding a message, a preview or a row of tools, such as a
 * reader's text tools, a seek preview or a page that failed to load. Clickable when [onClick] is set.
 */
@Composable
fun OverlayPanel(
    modifier: Modifier = Modifier,
    style: OverlayStyle = OverlayStyle.Chrome,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Overlay(modifier, style, KatariTheme.shapes.large, onClick, enabled = true, content)
}

@Composable
private fun Overlay(
    modifier: Modifier,
    style: OverlayStyle,
    shape: Shape,
    onClick: (() -> Unit)?,
    enabled: Boolean,
    content: @Composable () -> Unit,
) {
    val container = when (style) {
        OverlayStyle.Chrome -> SurfaceContainer.Chrome
        OverlayStyle.Scrim -> SurfaceContainer.MediaScrim
        OverlayStyle.Solid -> SurfaceContainer.Surface
        OverlayStyle.Raised -> SurfaceContainer.High
    }
    val shadow = style == OverlayStyle.Raised
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier,
            container = container,
            shape = shape,
            shadow = shadow,
            enabled = enabled,
            content = content,
        )
    } else {
        Surface(modifier = modifier, container = container, shape = shape, shadow = shadow, content = content)
    }
}
