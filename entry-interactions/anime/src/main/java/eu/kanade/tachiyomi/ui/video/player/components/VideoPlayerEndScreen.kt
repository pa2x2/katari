package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.roundedfilled.PlayArrow
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.progress.CircularProgressIndicator
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Dims the last frame once an episode finishes and offers the next one. With auto-play on, [countdown] fills the ring
 * around Play and [secondsLeft] counts down to the episode starting on its own; with it off both are null and the
 * screen waits for a tap.
 */
@Composable
internal fun VideoPlayerEndScreen(
    visible: Boolean,
    nextEpisodeName: String,
    countdown: Float?,
    secondsLeft: Int?,
    onPlayNext: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(visible = visible, modifier = modifier, enter = fadeIn(), exit = fadeOut()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(KatariTheme.colors.mediaScrim)
                .videoPlayerSafeContentPadding(includeTop = true, includeBottom = true),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = KatariTheme.spacing.xxl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m),
            ) {
                Text(
                    text = if (secondsLeft != null) {
                        stringResource(MR.strings.anime_playback_up_next_in, secondsLeft)
                    } else {
                        stringResource(MR.strings.anime_playback_up_next)
                    },
                    role = TextRole.Label,
                    tone = ContentTone.OnMediaVariant,
                )
                Box(contentAlignment = Alignment.Center) {
                    VideoPlayerTransportButton(
                        onClick = onPlayNext,
                        enabled = true,
                        contentSize = KatariTheme.sizes.iconLarge,
                    ) { tone ->
                        Icon(
                            imageVector = MaterialSymbols.RoundedFilled.PlayArrow,
                            contentDescription = stringResource(MR.strings.anime_playback_play_now),
                            modifier = Modifier.fillMaxSize(),
                            tone = tone,
                        )
                    }
                    if (countdown != null) {
                        CircularProgressIndicator(modifier = Modifier.matchParentSize(), progress = { countdown })
                    }
                }
                Text(
                    text = nextEpisodeName,
                    modifier = Modifier.fillMaxWidth(),
                    role = TextRole.SectionTitle,
                    tone = ContentTone.OnMedia,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Button(
                    label = stringResource(
                        if (countdown !=
                            null
                        ) {
                            MR.strings.action_cancel
                        } else {
                            MR.strings.action_close
                        },
                    ),
                    onClick = onDismiss,
                    style = ButtonStyle.Text,
                )
            }
        }
    }
}
