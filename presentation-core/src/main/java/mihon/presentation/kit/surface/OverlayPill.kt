package mihon.presentation.kit.surface

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.presentation.kit.theme.KatariTheme

/** What an [OverlayPill] floats over, which sets its fill. */
enum class OverlayPillStyle {
    /** The app's own full-screen views, such as the reader or a cover viewer: on the chrome colour. */
    Chrome,

    /** Media such as a video or artwork: on a translucent shade, with media content colours. */
    Scrim,
}

/**
 * A pill floating over a full-screen view or media, holding controls or a label, such as a cover viewer's actions or
 * a lock mark on an avatar. Clickable when [onClick] is set.
 */
@Composable
fun OverlayPill(
    modifier: Modifier = Modifier,
    style: OverlayPillStyle = OverlayPillStyle.Chrome,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val container = when (style) {
        OverlayPillStyle.Chrome -> SurfaceContainer.Chrome
        OverlayPillStyle.Scrim -> SurfaceContainer.MediaScrim
    }
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier,
            container = container,
            shape = KatariTheme.shapes.full,
            content = content,
        )
    } else {
        Surface(modifier = modifier, container = container, shape = KatariTheme.shapes.full, content = content)
    }
}
