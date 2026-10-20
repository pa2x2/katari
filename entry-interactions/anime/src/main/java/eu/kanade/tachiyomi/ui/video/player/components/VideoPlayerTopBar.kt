package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mihon.entry.interactions.source.EntryChildWebViewAction
import mihon.entry.interactions.source.EntryChildWebViewActionsMenu
import mihon.entry.interactions.source.EntryChildWebViewResolution
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Lock
import mihon.icons.materialsymbols.rounded.LockOpen
import mihon.icons.materialsymbols.rounded.PictureInPictureAlt
import mihon.icons.materialsymbols.rounded.Settings
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.surface.MediaScrim
import mihon.presentation.kit.surface.MediaScrimEdge
import mihon.presentation.kit.surface.MediaScrimWeight
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun VideoPlayerTopBar(
    videoTitle: String,
    episodeName: String,
    onBack: () -> Unit,
    showPictureInPictureButton: Boolean,
    onEnterPictureInPicture: () -> Unit,
    onToggleLock: () -> Unit,
    onOpenSettings: () -> Unit,
    childWebView: EntryChildWebViewResolution.Available?,
    onChildWebViewAction: (EntryChildWebViewAction, EntryChildWebViewResolution.Available) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = KatariTheme.spacing
    MediaScrim(
        edge = MediaScrimEdge.Top,
        modifier = modifier.fillMaxWidth(),
        weight = MediaScrimWeight.Controls,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .videoPlayerSafeContentPadding(includeTop = true)
                .padding(spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VideoPlayerHeading(
                videoTitle = videoTitle,
                episodeName = episodeName,
                onBack = onBack,
                modifier = Modifier.weight(1f),
            )
            VideoPlayerTopBarActions(
                locked = false,
                onToggleLock = onToggleLock,
                showPictureInPictureButton = showPictureInPictureButton,
                onEnterPictureInPicture = onEnterPictureInPicture,
                onOpenSettings = onOpenSettings,
                childWebView = childWebView,
                onChildWebViewAction = onChildWebViewAction,
            )
        }
    }
}

/**
 * The top bar's buttons. While [locked], only the lock button is drawn, as Unlock, and every other button leaves an
 * empty slot of its size so Unlock stays exactly where Lock was.
 */
@Composable
internal fun VideoPlayerTopBarActions(
    locked: Boolean,
    onToggleLock: () -> Unit,
    showPictureInPictureButton: Boolean,
    onEnterPictureInPicture: () -> Unit,
    onOpenSettings: () -> Unit,
    childWebView: EntryChildWebViewResolution.Available?,
    onChildWebViewAction: (EntryChildWebViewAction, EntryChildWebViewResolution.Available) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (locked) {
            IconButton(
                icon = MaterialSymbols.Rounded.LockOpen,
                contentDescription = stringResource(MR.strings.anime_playback_unlock_controls),
                onClick = onToggleLock,
                tone = ContentTone.OnMedia,
            )
        } else {
            IconButton(
                icon = MaterialSymbols.Rounded.Lock,
                contentDescription = stringResource(MR.strings.anime_playback_lock_controls),
                onClick = onToggleLock,
                tone = ContentTone.OnMedia,
            )
        }
        if (showPictureInPictureButton) {
            if (locked) {
                EmptyActionSlot()
            } else {
                IconButton(
                    icon = MaterialSymbols.Rounded.PictureInPictureAlt,
                    contentDescription = stringResource(MR.strings.pref_enable_anime_picture_in_picture),
                    onClick = onEnterPictureInPicture,
                    tone = ContentTone.OnMedia,
                )
            }
        }
        if (locked) {
            EmptyActionSlot()
        } else {
            IconButton(
                icon = MaterialSymbols.Rounded.Settings,
                contentDescription = stringResource(MR.strings.label_settings),
                onClick = onOpenSettings,
                tone = ContentTone.OnMedia,
            )
        }
        if (childWebView != null) {
            if (locked) {
                EmptyActionSlot()
            } else {
                EntryChildWebViewActionsMenu(
                    resolution = childWebView,
                    onAction = onChildWebViewAction,
                    tone = ContentTone.OnMedia,
                )
            }
        }
    }
}

@Composable
private fun EmptyActionSlot() {
    Box(modifier = Modifier.size(KatariTheme.sizes.touchTarget))
}
