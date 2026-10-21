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
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerSeekDirection
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerSeekFeedbackState
import kotlinx.coroutines.delay
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.ChevronLeft
import mihon.icons.materialsymbols.rounded.ChevronRight
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.surface.OverlayPanel
import mihon.presentation.kit.surface.OverlayStyle
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

private const val SEEK_FEEDBACK_VISIBLE_DURATION_MS = 700L

/** How much of a chevron fades out over one sweep. */
private const val CHEVRON_FADE = 0.78f

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
                .padding(horizontal = KatariTheme.spacing.xxl * 2),
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
    val spacing = KatariTheme.spacing
    OverlayPanel(modifier = modifier, style = OverlayStyle.Scrim) {
        Column(
            modifier = Modifier.padding(horizontal = spacing.l, vertical = spacing.s),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            VideoPlayerSeekChevrons(direction = direction)
            Text(text = stringResource(MR.strings.seconds_short, totalSeconds), role = TextRole.SectionTitle)
        }
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
    val drift = KatariTheme.spacing.s

    Row(
        modifier = modifier,
        // The chevron glyph fills only the middle of its icon box, so the boxes overlap to keep the arrows close.
        horizontalArrangement = Arrangement.spacedBy(-KatariTheme.spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Each chevron fades and drifts a little behind the one before it, so they sweep in the seek direction.
        repeat(3) { index ->
            val phase = ((progress - index * 0.18f) + 1f) % 1f
            val offsetX = if (direction == VideoPlayerSeekDirection.Backward) {
                drift * (phase - 0.5f)
            } else {
                drift * (0.5f - phase)
            }

            Icon(
                imageVector = if (direction == VideoPlayerSeekDirection.Backward) {
                    MaterialSymbols.Rounded.ChevronLeft
                } else {
                    MaterialSymbols.Rounded.ChevronRight
                },
                contentDescription = null,
                modifier = Modifier
                    .size(KatariTheme.sizes.iconLarge)
                    .offset(x = offsetX)
                    .graphicsLayer { alpha = 1f - phase * CHEVRON_FADE },
            )
        }
    }
}
