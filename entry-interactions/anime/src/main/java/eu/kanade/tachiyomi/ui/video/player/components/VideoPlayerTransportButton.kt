package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import mihon.presentation.kit.surface.OverlayPill
import mihon.presentation.kit.surface.OverlayStyle
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme

/** A round button on the media shade; [content] fills [contentSize] in the tone for its enabled state. */
@Composable
internal fun VideoPlayerTransportButton(
    onClick: () -> Unit,
    enabled: Boolean,
    contentSize: Dp,
    modifier: Modifier = Modifier,
    content: @Composable (tone: ContentTone) -> Unit,
) {
    OverlayPill(modifier = modifier, style = OverlayStyle.Scrim, onClick = onClick, enabled = enabled) {
        Box(
            modifier = Modifier
                .padding(KatariTheme.spacing.l)
                .size(contentSize),
            contentAlignment = Alignment.Center,
        ) {
            content(if (enabled) ContentTone.OnMedia else ContentTone.OnMediaVariant)
        }
    }
}
