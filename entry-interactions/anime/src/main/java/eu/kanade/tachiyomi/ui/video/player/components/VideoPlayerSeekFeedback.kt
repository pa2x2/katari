package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerSeekDirection
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerSeekFeedbackState
import kotlinx.coroutines.delay
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

private const val SEEK_FEEDBACK_VISIBLE_DURATION_MS = 700L

@Composable
internal fun VideoPlayerSeekFeedback(
    feedbackState: VideoPlayerSeekFeedbackState?,
    onDismissed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var displayedFeedback by remember { mutableStateOf<VideoPlayerSeekFeedbackState?>(null) }
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
        delay(SEEK_FEEDBACK_VISIBLE_DURATION_MS)
        if (latestFeedbackState?.sequence == feedbackState.sequence) {
            visible = false
            delay(120L)
            if (latestFeedbackState?.sequence == feedbackState.sequence) {
                displayedFeedback = null
                latestOnDismissed()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .videoPlayerSafeContentPadding(includeTop = true, includeBottom = true),
    ) {
        val activeFeedback = displayedFeedback ?: return@Box
        val alignment = when (activeFeedback.direction) {
            VideoPlayerSeekDirection.Backward -> Alignment.CenterStart
            VideoPlayerSeekDirection.Forward -> Alignment.CenterEnd
        }

        AnimatedVisibility(
            visible = visible,
            modifier = Modifier
                .align(alignment)
                .padding(horizontal = 88.dp),
            enter = fadeIn(animationSpec = tween(90)),
            exit = fadeOut(animationSpec = tween(120)),
        ) {
            VideoPlayerSeekIndicator(
                direction = activeFeedback.direction,
                totalSeconds = activeFeedback.totalSeconds,
            )
        }
    }
}

@Composable
private fun VideoPlayerSeekIndicator(
    direction: VideoPlayerSeekDirection,
    totalSeconds: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        VideoPlayerSeekChevrons(direction = direction)
        Text(
            text = stringResource(MR.strings.seconds_short, totalSeconds),
            color = Color.White,
            style = MaterialTheme.typography.titleMedium.merge(
                TextStyle(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.45f),
                        blurRadius = 12f,
                    ),
                ),
            ),
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun VideoPlayerSeekChevrons(
    direction: VideoPlayerSeekDirection,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "seekFeedbackChevrons")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "seekFeedbackChevronProgress",
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { index ->
            val phase = ((progress - index * 0.18f) + 1f) % 1f
            val alpha = 0.22f + ((1f - phase) * 0.78f)
            val offsetX = if (direction == VideoPlayerSeekDirection.Backward) {
                ((phase - 0.5f) * 6f).dp
            } else {
                ((0.5f - phase) * 6f).dp
            }

            Text(
                text = if (direction == VideoPlayerSeekDirection.Backward) "<" else ">",
                modifier = Modifier.offset(x = offsetX),
                color = Color.White.copy(alpha = alpha),
                style = MaterialTheme.typography.titleLarge.merge(
                    TextStyle(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.4f),
                            blurRadius = 10f,
                        ),
                    ),
                ),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
