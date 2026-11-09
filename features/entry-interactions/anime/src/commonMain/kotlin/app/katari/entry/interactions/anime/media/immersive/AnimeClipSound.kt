package app.katari.entry.interactions.anime.media.immersive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.katari.entry.viewer.chrome.ui.pointer.mediaControl
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.automirroredrounded.VolumeOff
import app.katari.icons.materialsymbols.automirroredrounded.VolumeUp
import app.katari.presentation.kit.button.ContentButton
import app.katari.presentation.kit.content.Icon
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.control.MediaVolumeControl
import app.katari.presentation.kit.control.MediaVolumeSlider
import app.katari.presentation.kit.surface.OverlayPill
import app.katari.presentation.kit.surface.OverlayStyle
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole
import app.katari.video.engine.spi.player.VideoPlayer
import eu.kanade.tachiyomi.ui.video.player.gesture.VideoPlayerShortcuts
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * A clip's sound button among its controls. Where clips play at the player's own volume ([volumePercent] not null), it
 * is the player's volume control, its slider opening while the mouse points at it and the wheel over it changing the
 * level; elsewhere it only mutes and unmutes, as the device's keys set the volume.
 */
@Composable
internal fun AnimeClipSoundButton(
    muted: Boolean,
    volumePercent: Int?,
    onToggleMute: () -> Unit,
    onVolumeChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (volumePercent != null) {
        MediaVolumeControl(
            level = volumePercent,
            maxLevel = VideoPlayer.MAX_VOLUME_BOOST_PERCENT,
            fullLevel = VideoPlayer.NO_VOLUME_BOOST_PERCENT,
            muted = muted,
            onLevelChange = onVolumeChange,
            onToggleMute = onToggleMute,
            modifier = modifier.mediaControl(),
            muteShortcut = VideoPlayerShortcuts.Mute,
            slider = MediaVolumeSlider.WhilePointed,
            wheelStep = VideoPlayerShortcuts.VOLUME_STEP_PERCENT,
        )
        return
    }
    val spacing = KatariTheme.spacing
    ContentButton(onClick = onToggleMute, modifier = modifier.size(KatariTheme.sizes.touchTarget).mediaControl()) {
        Box(contentAlignment = Alignment.Center) {
            OverlayPill(style = OverlayStyle.Scrim) {
                Icon(
                    imageVector = if (muted) {
                        MaterialSymbols.AutoMirroredRounded.VolumeOff
                    } else {
                        MaterialSymbols.AutoMirroredRounded.VolumeUp
                    },
                    contentDescription = stringResource(
                        if (muted) MR.strings.action_unmute else MR.strings.action_mute,
                    ),
                    modifier = Modifier
                        .padding(spacing.s)
                        .size(KatariTheme.sizes.iconSmall),
                )
            }
        }
    }
}

/** What a muted clip shows while its controls are hidden, so the silence is never a surprise; clicking it unmutes. */
@Composable
internal fun AnimeClipMutedBadge(onUnmute: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = KatariTheme.spacing
    OverlayPill(modifier = modifier.mediaControl(), style = OverlayStyle.Scrim, onClick = onUnmute) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.m, vertical = spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = MaterialSymbols.AutoMirroredRounded.VolumeOff,
                contentDescription = null,
                modifier = Modifier.size(KatariTheme.sizes.iconSmall),
            )
            Text(text = stringResource(MR.strings.browse_immersive_muted), role = TextRole.Label)
        }
    }
}
