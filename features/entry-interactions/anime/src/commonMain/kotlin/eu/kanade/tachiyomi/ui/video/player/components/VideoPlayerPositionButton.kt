package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.katari.presentation.kit.navigation.MediaToolbarPosition
import eu.kanade.tachiyomi.ui.video.player.playback.coerceToPlaybackDuration
import eu.kanade.tachiyomi.ui.video.player.playback.formatPlaybackTimestamp
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The playback time in the controls' toolbar: the position and the duration, or the time left while
 * [showsRemainingTime], which a click switches. Short of room it shows the position alone.
 */
@Composable
internal fun VideoPlayerPositionButton(
    positionMs: Long,
    durationMs: Long,
    showsRemainingTime: Boolean,
    onToggleRemainingTime: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val effectiveDurationMs = durationMs.coerceAtLeast(0L)
    val clampedPositionMs = positionMs.coerceToPlaybackDuration(effectiveDurationMs)
    val position = formatPlaybackTimestamp(clampedPositionMs)
    val text = when {
        showsRemainingTime && effectiveDurationMs > 0L ->
            "−${formatPlaybackTimestamp(effectiveDurationMs - clampedPositionMs)}"
        effectiveDurationMs > 0L -> "$position / ${formatPlaybackTimestamp(effectiveDurationMs)}"
        else -> "$position / --:--"
    }
    MediaToolbarPosition(
        text = text,
        compactText = if (showsRemainingTime && effectiveDurationMs > 0L) text else position,
        onClick = onToggleRemainingTime,
        onClickLabel = stringResource(
            if (showsRemainingTime) {
                MR.strings.anime_playback_show_elapsed_time
            } else {
                MR.strings.anime_playback_show_remaining_time
            },
        ),
        modifier = modifier.videoPlayerControl(),
    )
}
