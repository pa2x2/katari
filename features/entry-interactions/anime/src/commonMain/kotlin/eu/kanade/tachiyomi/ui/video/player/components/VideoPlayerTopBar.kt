package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.katari.entry.interactions.source.EntryChildWebViewAction
import app.katari.entry.interactions.source.EntryChildWebViewActionsMenu
import app.katari.entry.interactions.source.EntryChildWebViewResolution
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Lock
import app.katari.icons.materialsymbols.rounded.LockOpen
import app.katari.icons.materialsymbols.rounded.PictureInPictureAlt
import app.katari.icons.materialsymbols.rounded.Settings
import app.katari.presentation.kit.button.IconButton
import app.katari.presentation.kit.button.IconButtonSize
import app.katari.presentation.kit.surface.MediaScrim
import app.katari.presentation.kit.surface.MediaScrimEdge
import app.katari.presentation.kit.surface.MediaScrimWeight
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.KatariTheme
import eu.kanade.tachiyomi.ui.video.player.gesture.VideoPlayerShortcuts
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun VideoPlayerTopBar(
    videoTitle: String,
    episodeName: String,
    onBack: () -> Unit,
    showPictureInPictureButton: Boolean,
    onEnterPictureInPicture: () -> Unit,
    onToggleLock: (() -> Unit)?,
    onOpenSettings: () -> Unit,
    childWebView: EntryChildWebViewResolution.Available?,
    childWebViewActions: List<EntryChildWebViewAction>,
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
                childWebViewActions = childWebViewActions,
                onChildWebViewAction = onChildWebViewAction,
            )
        }
    }
}

/**
 * The top bar's buttons. While [locked], only the lock button is drawn, as Unlock, and every other button leaves an
 * empty slot of its size so Unlock stays exactly where Lock was. Without [onToggleLock] there is no lock button.
 */
@Composable
internal fun VideoPlayerTopBarActions(
    locked: Boolean,
    onToggleLock: (() -> Unit)?,
    showPictureInPictureButton: Boolean,
    onEnterPictureInPicture: () -> Unit,
    onOpenSettings: () -> Unit,
    childWebView: EntryChildWebViewResolution.Available?,
    onChildWebViewAction: (EntryChildWebViewAction, EntryChildWebViewResolution.Available) -> Unit,
    modifier: Modifier = Modifier,
    childWebViewActions: List<EntryChildWebViewAction> = EntryChildWebViewAction.entries,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        when {
            onToggleLock == null -> Unit
            locked -> IconButton(
                icon = MaterialSymbols.Rounded.LockOpen,
                contentDescription = stringResource(MR.strings.anime_playback_unlock_controls),
                onClick = onToggleLock,
                modifier = Modifier.videoPlayerControl(),
                size = IconButtonSize.Large,
                tone = ContentTone.OnMedia,
            )
            else -> IconButton(
                icon = MaterialSymbols.Rounded.Lock,
                contentDescription = stringResource(MR.strings.anime_playback_lock_controls),
                onClick = onToggleLock,
                modifier = Modifier.videoPlayerControl(),
                size = IconButtonSize.Large,
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
                    modifier = Modifier.videoPlayerControl(),
                    size = IconButtonSize.Large,
                    tone = ContentTone.OnMedia,
                    shortcut = VideoPlayerShortcuts.PictureInPicture,
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
                modifier = Modifier.videoPlayerControl(),
                size = IconButtonSize.Large,
                tone = ContentTone.OnMedia,
            )
        }
        if (childWebView != null) {
            if (locked) {
                EmptyActionSlot()
            } else {
                Box(modifier = Modifier.videoPlayerControl()) {
                    EntryChildWebViewActionsMenu(
                        resolution = childWebView,
                        actions = childWebViewActions,
                        onAction = onChildWebViewAction,
                        size = IconButtonSize.Large,
                        tone = ContentTone.OnMedia,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyActionSlot() {
    Box(modifier = Modifier.size(KatariTheme.sizes.touchTargetLarge))
}
