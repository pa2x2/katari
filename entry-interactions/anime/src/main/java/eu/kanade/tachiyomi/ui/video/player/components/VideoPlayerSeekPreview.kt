package eu.kanade.tachiyomi.ui.video.player.components

import android.view.TextureView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.exoplayer.ExoPlayer
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerSeekPreviewState
import eu.kanade.tachiyomi.ui.video.player.formatPlaybackTimestamp
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.progress.LoadingIndicator
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import kotlin.math.roundToInt

/**
 * The frame above the timeline while scrubbing: a live preview of the target position when the preview player has
 * one, otherwise the target timestamp. It follows the scrub position along the timeline without leaving the screen.
 */
@Composable
internal fun VideoPlayerSeekPreview(
    positionMs: Long,
    previewFraction: Float,
    previewState: VideoPlayerSeekPreviewState?,
    previewPlayer: ExoPlayer?,
    previewAspectRatio: Float?,
) {
    val showPlayer = previewState?.visible == true && previewState.available && previewPlayer != null
    val showLoading = previewState?.visible == true && previewState.loading
    val spacing = KatariTheme.spacing
    val windowLongSide = with(LocalDensity.current) {
        LocalWindowInfo.current.containerSize.run { maxOf(width, height) }.toDp()
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val previewSize = fitVideoPreviewSize(
            maxWidth = minOf(maxWidth, windowLongSide * PREVIEW_MAX_WIDTH_FRACTION).value,
            maxHeight = (windowLongSide * PREVIEW_MAX_HEIGHT_FRACTION).value,
            aspectRatio = previewAspectRatio,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(previewSize.height.dp + spacing.s),
        ) {
            Surface(
                modifier = Modifier.layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                    val width = constraints.maxWidth
                    val targetCenterX = (width * previewFraction.coerceIn(0f, 1f)).roundToInt()
                    val x = (targetCenterX - (placeable.width / 2))
                        .coerceIn(0, (width - placeable.width).coerceAtLeast(0))

                    layout(width, placeable.height) {
                        placeable.placeRelative(x, 0)
                    }
                },
                container = SurfaceContainer.MediaScrim,
                shape = if (showPlayer) KatariTheme.shapes.medium else KatariTheme.shapes.full,
            ) {
                if (showPlayer) {
                    Box {
                        SeekPreviewVideo(
                            player = previewPlayer,
                            modifier = Modifier.size(width = previewSize.width.dp, height = previewSize.height.dp),
                        )
                        SeekPreviewTimestamp(
                            positionMs = previewState.positionMs,
                            loading = showLoading,
                            role = TextRole.Badge,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(spacing.xs),
                        )
                    }
                } else {
                    SeekPreviewTimestamp(positionMs = positionMs, loading = showLoading, role = TextRole.Label)
                }
            }
        }
    }
}

@Composable
private fun SeekPreviewVideo(player: ExoPlayer, modifier: Modifier) {
    var attachedTextureView by remember(player) { mutableStateOf<TextureView?>(null) }
    AndroidView(
        modifier = modifier,
        factory = { context -> TextureView(context) },
        update = { textureView ->
            if (attachedTextureView !== textureView) {
                attachedTextureView?.let { previousTextureView ->
                    runCatching { player.clearVideoTextureView(previousTextureView) }
                }
                runCatching { player.setVideoTextureView(textureView) }
                attachedTextureView = textureView
            }
        },
        onRelease = { textureView ->
            if (attachedTextureView === textureView) {
                runCatching { player.clearVideoTextureView(textureView) }
                attachedTextureView = null
            }
        },
    )
}

@Composable
private fun SeekPreviewTimestamp(
    positionMs: Long,
    loading: Boolean,
    role: TextRole,
    modifier: Modifier = Modifier,
) {
    val spacing = KatariTheme.spacing
    Surface(modifier = modifier, container = SurfaceContainer.MediaScrim, shape = KatariTheme.shapes.full) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.m, vertical = spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (loading) {
                LoadingIndicator(
                    modifier = Modifier.size(KatariTheme.sizes.iconSmall),
                    color = KatariTheme.colors.onMedia,
                )
            }
            Text(text = formatPlaybackTimestamp(positionMs), role = role)
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

// The preview grows with the screen, capped at these shares of its longer side: about 350 by 200dp on a phone.
private const val PREVIEW_MAX_WIDTH_FRACTION = 0.38f
private const val PREVIEW_MAX_HEIGHT_FRACTION = 0.22f
private const val DEFAULT_SEEK_PREVIEW_ASPECT_RATIO = 16f / 9f
