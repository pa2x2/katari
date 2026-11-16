package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import app.katari.entry.viewer.chrome.ui.pointer.mediaControl
import app.katari.presentation.kit.feedback.HoverTooltip
import app.katari.presentation.kit.shortcut.KeyboardShortcut
import app.katari.presentation.kit.surface.OverlayPill
import app.katari.presentation.kit.surface.OverlayStyle
import app.katari.presentation.kit.theme.KatariTheme

/**
 * A round button on the media shade; [content] fills [contentSize]. Hovered with a mouse, it names [label] and the keys
 * of [shortcut].
 */
@Composable
internal fun VideoPlayerTransportButton(
    onClick: () -> Unit,
    contentSize: Dp,
    label: String,
    shortcut: KeyboardShortcut,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    HoverTooltip(label, shortcut = shortcut) {
        OverlayPill(
            modifier = modifier.mediaControl(),
            style = OverlayStyle.Scrim,
            onClick = onClick,
        ) {
            Box(
                modifier = Modifier
                    .padding(KatariTheme.spacing.l)
                    .size(contentSize),
                contentAlignment = Alignment.Center,
            ) {
                content()
            }
        }
    }
}
