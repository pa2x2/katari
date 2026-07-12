package eu.kanade.tachiyomi.ui.video.player.components

import android.view.TextureView
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.exoplayer.ExoPlayer
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerSeekPreviewState
import eu.kanade.tachiyomi.ui.video.player.coerceToPlaybackDuration
import eu.kanade.tachiyomi.ui.video.player.formatPlaybackTimestamp
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private val SeekPreviewWidth = 349.dp
private val SeekPreviewHeight = 199.dp
private const val DEFAULT_SEEK_PREVIEW_ASPECT_RATIO = 349f / 199f
private val SeekPreviewVerticalSpacing = 10.dp

@Composable
internal fun VideoPlayerTimeline(
    positionMs: Long,
    durationMs: Long,
    bufferedPositionMs: Long,
    isScrubbing: Boolean,
    seekPreviewState: VideoPlayerSeekPreviewState?,
    seekPreviewPlayer: ExoPlayer?,
    seekPreviewAspectRatio: Float?,
    onScrubStarted: () -> Unit,
    onScrubPositionChange: (Long) -> Unit,
    onScrubFinished: () -> Unit,
    footerContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val effectiveDurationMs = durationMs.coerceAtLeast(0L)
    val clampedPositionMs = positionMs.coerceToPlaybackDuration(effectiveDurationMs)
    val clampedBufferedPositionMs = bufferedPositionMs.coerceToPlaybackDuration(effectiveDurationMs)
    val playedFraction = if (effectiveDurationMs > 0L) {
        clampedPositionMs.toFloat() / effectiveDurationMs.toFloat()
    } else {
        0f
    }
    val bufferedFraction = if (effectiveDurationMs > 0L) {
        clampedBufferedPositionMs.toFloat() / effectiveDurationMs.toFloat()
    } else {
        0f
    }
    val trackHeight by animateDpAsState(
        targetValue = if (isScrubbing) 5.dp else 3.dp,
        animationSpec = tween(140),
        label = "timelineTrackHeight",
    )
    val thumbSize by animateDpAsState(
        targetValue = if (isScrubbing) 14.dp else 10.dp,
        animationSpec = tween(140),
        label = "timelineThumbSize",
    )
    val thumbHaloSize by animateDpAsState(
        targetValue = if (isScrubbing) 24.dp else 0.dp,
        animationSpec = tween(160),
        label = "timelineThumbHaloSize",
    )
    val timelineBarHeight = (if (thumbHaloSize > thumbSize) thumbHaloSize else thumbSize) + 8.dp
    val timeLabel = buildString {
        append(formatPlaybackTimestamp(clampedPositionMs))
        append(" / ")
        append(if (effectiveDurationMs > 0L) formatPlaybackTimestamp(effectiveDurationMs) else "--:--")
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                ),
            )
            .videoPlayerSafeContentPadding(includeBottom = true)
            .padding(horizontal = 18.dp)
            .padding(top = 6.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (isScrubbing && seekPreviewState?.visible == true) {
            VideoPlayerSeekPreview(
                positionMs = clampedPositionMs,
                previewFraction = playedFraction,
                previewState = seekPreviewState,
                previewPlayer = seekPreviewPlayer,
                previewAspectRatio = seekPreviewAspectRatio,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (isScrubbing) 8.dp else 0.dp),
        ) {
            Box(modifier = Modifier.padding(bottom = 8.dp)) {
                Surface(
                    color = Color.Black.copy(alpha = 0.34f),
                    shape = CircleShape,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                ) {
                    Text(
                        text = timeLabel,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        color = Color.White.copy(alpha = 0.94f),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }

            VideoPlayerTimelineBar(
                playedFraction = playedFraction,
                bufferedFraction = bufferedFraction,
                durationMs = effectiveDurationMs,
                thumbSize = thumbSize,
                thumbHaloSize = thumbHaloSize,
                trackHeight = trackHeight,
                onScrubStarted = onScrubStarted,
                onScrubPositionChange = onScrubPositionChange,
                onScrubFinished = onScrubFinished,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(timelineBarHeight),
            )

            if (footerContent != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                ) {
                    footerContent()
                }
            }
        }
    }
}

@Composable
private fun VideoPlayerSeekPreview(
    positionMs: Long,
    previewFraction: Float,
    previewState: VideoPlayerSeekPreviewState?,
    previewPlayer: ExoPlayer?,
    previewAspectRatio: Float?,
) {
    val showPlayer = previewState?.visible == true && previewState.available && previewPlayer != null
    val showLoading = previewState?.visible == true && previewState.loading
    val showLoadingTimestamp = showLoading && !showPlayer
    val shape = if (showPlayer) RoundedCornerShape(10.dp) else CircleShape
    var attachedTextureView by remember(previewPlayer) { mutableStateOf<TextureView?>(null) }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val previewSize = fitVideoPreviewSize(
            maxWidth = minOf(maxWidth, SeekPreviewWidth).value,
            maxHeight = SeekPreviewHeight.value,
            aspectRatio = previewAspectRatio,
        )
        val previewWidth = previewSize.width.dp
        val previewHeight = previewSize.height.dp

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(previewHeight + SeekPreviewVerticalSpacing),
        ) {
            Surface(
                modifier = Modifier
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                        val width = constraints.maxWidth
                        val targetCenterX = (width * previewFraction.coerceIn(0f, 1f)).roundToInt()
                        val x = (targetCenterX - (placeable.width / 2))
                            .coerceIn(0, (width - placeable.width).coerceAtLeast(0))

                        layout(width, placeable.height) {
                            placeable.placeRelative(x, 0)
                        }
                    },
                color = Color.Black,
                shape = shape,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
            ) {
                if (showPlayer) {
                    Box {
                        AndroidView(
                            modifier = Modifier.size(width = previewWidth, height = previewHeight),
                            factory = { context ->
                                TextureView(context)
                            },
                            update = { textureView ->
                                if (attachedTextureView !== textureView) {
                                    attachedTextureView?.let { previousTextureView ->
                                        runCatching { previewPlayer.clearVideoTextureView(previousTextureView) }
                                    }
                                    runCatching { previewPlayer.setVideoTextureView(textureView) }
                                    attachedTextureView = textureView
                                }
                            },
                            onRelease = { textureView ->
                                if (attachedTextureView === textureView) {
                                    runCatching { previewPlayer.clearVideoTextureView(textureView) }
                                    attachedTextureView = null
                                }
                            },
                        )
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(6.dp),
                            color = Color.Black.copy(alpha = 0.62f),
                            shape = CircleShape,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (showLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier
                                            .padding(end = 6.dp)
                                            .size(12.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                    )
                                }
                                Text(
                                    text = formatPlaybackTimestamp(previewState.positionMs),
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (showLoadingTimestamp) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(14.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                        }
                        Text(
                            text = formatPlaybackTimestamp(positionMs),
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}

internal data class VideoPreviewSize(
    val width: Float,
    val height: Float,
)

internal fun fitVideoPreviewSize(
    maxWidth: Float,
    maxHeight: Float,
    aspectRatio: Float?,
): VideoPreviewSize {
    if (!maxWidth.isFinite() || !maxHeight.isFinite() || maxWidth <= 0f || maxHeight <= 0f) {
        return VideoPreviewSize(width = 0f, height = 0f)
    }

    val resolvedAspectRatio = aspectRatio
        ?.takeIf { it.isFinite() && it > 0f }
        ?: DEFAULT_SEEK_PREVIEW_ASPECT_RATIO
    val width = minOf(maxWidth, maxHeight * resolvedAspectRatio)
    return VideoPreviewSize(
        width = width,
        height = width / resolvedAspectRatio,
    )
}

@Composable
private fun VideoPlayerTimelineBar(
    playedFraction: Float,
    bufferedFraction: Float,
    durationMs: Long,
    thumbSize: Dp,
    thumbHaloSize: Dp,
    trackHeight: Dp,
    onScrubStarted: () -> Unit,
    onScrubPositionChange: (Long) -> Unit,
    onScrubFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clampedPlayedFraction = playedFraction.coerceIn(0f, 1f)
    val clampedBufferedFraction = bufferedFraction.coerceIn(clampedPlayedFraction, 1f)
    val maxThumbSize = if (thumbHaloSize > thumbSize) thumbHaloSize else thumbSize
    val latestOnScrubStarted by rememberUpdatedState(onScrubStarted)
    val latestOnScrubPositionChange by rememberUpdatedState(onScrubPositionChange)
    val latestOnScrubFinished by rememberUpdatedState(onScrubFinished)
    val latestDurationMs by rememberUpdatedState(durationMs)
    val latestMaxThumbSize by rememberUpdatedState(maxThumbSize)
    val activeTrackColor = MaterialTheme.colorScheme.primary

    Canvas(
        modifier = modifier.pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val durationMs = latestDurationMs
                if (durationMs <= 0L) {
                    down.consume()
                    return@awaitEachGesture
                }

                val pointerId = down.id

                fun updateScrubPosition(touchX: Float) {
                    latestOnScrubPositionChange(
                        timelinePositionFromTouch(
                            touchX = touchX,
                            widthPx = size.width,
                            thumbInsetPx = latestMaxThumbSize.toPx() / 2f,
                            durationMs = latestDurationMs,
                        ),
                    )
                }

                latestOnScrubStarted()
                updateScrubPosition(down.position.x)
                down.consume()

                try {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                        updateScrubPosition(change.position.x)
                        change.consume()
                        if (!change.pressed) {
                            break
                        }
                    }
                } finally {
                    latestOnScrubFinished()
                }
            }
        },
    ) {
        val thumbInsetPx = maxThumbSize.toPx() / 2f
        val centerY = size.height / 2f
        val trackStartX = thumbInsetPx
        val trackEndX = size.width - thumbInsetPx
        val trackWidthPx = (trackEndX - trackStartX).coerceAtLeast(0f)
        val playedEndX = trackStartX + (trackWidthPx * clampedPlayedFraction)
        val bufferedEndX = trackStartX + (trackWidthPx * clampedBufferedFraction)
        val strokeWidthPx = trackHeight.toPx()

        drawLine(
            color = Color.White.copy(alpha = 0.28f),
            start = Offset(trackStartX, centerY),
            end = Offset(trackEndX, centerY),
            strokeWidth = strokeWidthPx,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = Color.White.copy(alpha = 0.42f),
            start = Offset(trackStartX, centerY),
            end = Offset(bufferedEndX, centerY),
            strokeWidth = strokeWidthPx,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = activeTrackColor,
            start = Offset(trackStartX, centerY),
            end = Offset(playedEndX, centerY),
            strokeWidth = strokeWidthPx,
            cap = StrokeCap.Round,
        )
        if (thumbHaloSize > 0.dp) {
            drawCircle(
                color = activeTrackColor.copy(alpha = 0.24f),
                radius = thumbHaloSize.toPx() / 2f,
                center = Offset(playedEndX, centerY),
            )
        }
        drawCircle(
            color = activeTrackColor,
            radius = thumbSize.toPx() / 2f,
            center = Offset(playedEndX, centerY),
        )
    }
}

private fun timelinePositionFromTouch(
    touchX: Float,
    widthPx: Int,
    thumbInsetPx: Float,
    durationMs: Long,
): Long {
    val trackWidthPx = (widthPx.toFloat() - (thumbInsetPx * 2f)).coerceAtLeast(1f)
    val fraction = ((touchX - thumbInsetPx) / trackWidthPx).coerceIn(0f, 1f)
    return (durationMs * fraction)
        .roundToLong()
        .coerceToPlaybackDuration(durationMs)
}
