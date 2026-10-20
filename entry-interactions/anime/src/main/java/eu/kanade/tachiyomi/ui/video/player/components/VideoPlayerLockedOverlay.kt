package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mihon.entry.interactions.source.EntryChildWebViewResolution
import mihon.presentation.kit.surface.MediaScrim
import mihon.presentation.kit.surface.MediaScrimEdge
import mihon.presentation.kit.theme.KatariTheme

@Composable
internal fun VideoPlayerLockedOverlay(
    onUnlock: () -> Unit,
    showPictureInPictureButton: Boolean,
    childWebView: EntryChildWebViewResolution.Available?,
    modifier: Modifier = Modifier,
) {
    MediaScrim(edge = MediaScrimEdge.Top, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .videoPlayerSafeContentPadding(includeTop = true)
                .padding(KatariTheme.spacing.s),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VideoPlayerTopBarActions(
                locked = true,
                onToggleLock = onUnlock,
                showPictureInPictureButton = showPictureInPictureButton,
                onEnterPictureInPicture = {},
                onOpenSettings = {},
                childWebView = childWebView,
                onChildWebViewAction = { _, _ -> },
            )
        }
    }
}
