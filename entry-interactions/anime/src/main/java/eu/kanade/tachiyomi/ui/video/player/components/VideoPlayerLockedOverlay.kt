package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.LockOpen
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.surface.MediaScrim
import mihon.presentation.kit.surface.MediaScrimEdge
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun VideoPlayerLockedOverlay(
    onUnlock: () -> Unit,
    showPictureInPictureButton: Boolean,
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
            IconButton(
                icon = MaterialSymbols.Rounded.LockOpen,
                contentDescription = stringResource(MR.strings.anime_playback_unlock_controls),
                onClick = onUnlock,
                tone = ContentTone.OnMedia,
            )
            if (showPictureInPictureButton) {
                // Preserve the top-bar spacing so the unlock button stays in the lock button slot.
                TopBarButtonPlaceholder()
            }
            TopBarButtonPlaceholder()
        }
    }
}

@Composable
private fun TopBarButtonPlaceholder() {
    Box(modifier = Modifier.size(KatariTheme.sizes.touchTarget))
}
