package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import eu.kanade.tachiyomi.ui.video.player.gesture.VideoPlayerSideGestureFeedbackState
import eu.kanade.tachiyomi.ui.video.player.gesture.VideoPlayerSideGestureType
import kotlinx.coroutines.delay
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.VolumeOff
import mihon.icons.materialsymbols.automirroredrounded.VolumeUp
import mihon.icons.materialsymbols.rounded.Brightness6
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.progress.MediaLevelIndicator
import mihon.presentation.kit.surface.OverlayPill
import mihon.presentation.kit.surface.OverlayStyle
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import java.text.NumberFormat

private const val SIDE_GESTURE_FEEDBACK_VISIBLE_DURATION_MS = 900L

@Composable
internal fun VideoPlayerSideGestureFeedback(
    feedbackState: VideoPlayerSideGestureFeedbackState?,
    onDismissed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var displayedFeedback by remember { mutableStateOf<VideoPlayerSideGestureFeedbackState?>(null) }
    var visible by remember { mutableStateOf(false) }
    val latestFeedbackState by rememberUpdatedState(feedbackState)
    val latestOnDismissed by rememberUpdatedState(onDismissed)

    LaunchedEffect(feedbackState?.sequence) {
        if (feedbackState == null) {
            visible = false
            displayedFeedback = null
            return@LaunchedEffect
        }

        displayedFeedback = feedbackState
        visible = true
        delay(SIDE_GESTURE_FEEDBACK_VISIBLE_DURATION_MS)
        if (latestFeedbackState?.sequence == feedbackState.sequence) {
            visible = false
            delay(120L)
            if (latestFeedbackState?.sequence == feedbackState.sequence) {
                displayedFeedback = null
                latestOnDismissed()
            }
        }
    }

    Box(modifier = modifier) {
        val activeFeedback = displayedFeedback ?: return@Box
        val alignment = when (activeFeedback.type) {
            VideoPlayerSideGestureType.Brightness -> Alignment.CenterStart
            VideoPlayerSideGestureType.Volume -> Alignment.CenterEnd
        }
        VideoPlayerChromeContainer(
            modifier = Modifier
                .fillMaxSize()
                .videoPlayerSafeContentPadding(includeTop = true, includeBottom = true),
            matchParentHeight = true,
        ) {
            AnimatedVisibility(
                visible = visible,
                modifier = Modifier
                    .align(alignment)
                    .padding(horizontal = KatariTheme.spacing.m),
                enter = fadeIn(animationSpec = tween(100)),
                exit = fadeOut(animationSpec = tween(120)),
            ) {
                VideoPlayerSideGestureFeedbackCard(feedbackState = activeFeedback)
            }
        }
    }
}

@Composable
private fun VideoPlayerSideGestureFeedbackCard(
    feedbackState: VideoPlayerSideGestureFeedbackState,
    modifier: Modifier = Modifier,
) {
    val level = if (feedbackState.maxLevel > 0) {
        feedbackState.level / feedbackState.maxLevel.toFloat()
    } else {
        0f
    }
    val icon = when (feedbackState.type) {
        VideoPlayerSideGestureType.Brightness -> MaterialSymbols.Rounded.Brightness6
        VideoPlayerSideGestureType.Volume -> if (feedbackState.level == 0) {
            MaterialSymbols.AutoMirroredRounded.VolumeOff
        } else {
            MaterialSymbols.AutoMirroredRounded.VolumeUp
        }
    }
    val boostFrom = feedbackState.boostFromLevel?.let { it / feedbackState.maxLevel.toFloat() }
    val percentFormat = remember { NumberFormat.getPercentInstance() }
    // The meter hugs the screen edge the gesture runs along, with its icon toward the middle of the video.
    val meterFirst = feedbackState.type == VideoPlayerSideGestureType.Brightness

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!meterFirst) {
            feedbackState.boostPercent?.let { percent ->
                SideGestureLabel(text = percentFormat.format(percent / 100f))
            }
            SideGestureIcon(icon = icon)
        }
        MediaLevelIndicator(level = level, boostFrom = boostFrom)
        if (meterFirst) SideGestureIcon(icon = icon)
    }
}

@Composable
private fun SideGestureLabel(text: String) {
    OverlayPill(style = OverlayStyle.Scrim) {
        Text(
            text = text,
            role = TextRole.Label,
            modifier = Modifier.padding(horizontal = KatariTheme.spacing.m, vertical = KatariTheme.spacing.s),
        )
    }
}

@Composable
private fun SideGestureIcon(icon: ImageVector) {
    OverlayPill(style = OverlayStyle.Scrim) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.padding(KatariTheme.spacing.s),
        )
    }
}
