package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.video.player.playback.formatPlaybackTimestamp
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.progress.LoadingIndicator
import mihon.presentation.kit.surface.OverlayPanel
import mihon.presentation.kit.surface.OverlayPill
import mihon.presentation.kit.surface.OverlayStyle
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import mihon.presentation.kit.window.currentWindowLayout

internal data class VideoPlayerSeekPreviewState(
    val positionMs: Long = 0L,
    val visible: Boolean = false,
    val loading: Boolean = false,
    val available: Boolean = false,
)

/**
 * The frame the timeline floats above its bar while scrubbing or hovering: a live preview of the target position when
 * the preview player has one, drawn by [previewPicture], otherwise the target timestamp, with the hovered chapter's
 * [chapterName]. The preview grows with the window up to the timeline's cap.
 */
@Composable
internal fun VideoPlayerSeekPreview(
    positionMs: Long,
    previewState: VideoPlayerSeekPreviewState?,
    previewPicture: (@Composable (Modifier) -> Unit)?,
    previewAspectRatio: Float?,
    chapterName: String? = null,
) {
    val showPlayer = previewState?.visible == true && previewState.available && previewPicture != null
    val showLoading = previewState?.visible == true && previewState.loading
    val spacing = KatariTheme.spacing
    val windowLongSide = currentWindowLayout().size.run { maxOf(width, height) }

    BoxWithConstraints {
        val previewSize = fitVideoPreviewSize(
            maxWidth = minOf(maxWidth, windowLongSide * PREVIEW_MAX_WIDTH_FRACTION).value,
            maxHeight = (windowLongSide * PREVIEW_MAX_HEIGHT_FRACTION).value,
            aspectRatio = previewAspectRatio,
        )
        if (showPlayer) {
            OverlayPanel(style = OverlayStyle.Scrim) {
                Box {
                    previewPicture(Modifier.size(width = previewSize.width.dp, height = previewSize.height.dp))
                    SeekPreviewTimestamp(
                        positionMs = previewState.positionMs,
                        chapterName = chapterName,
                        loading = showLoading,
                        role = TextRole.Badge,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(spacing.xs),
                    )
                }
            }
        } else {
            SeekPreviewTimestamp(
                positionMs = positionMs,
                chapterName = chapterName,
                loading = showLoading,
                role = TextRole.Label,
            )
        }
    }
}

@Composable
private fun SeekPreviewTimestamp(
    positionMs: Long,
    chapterName: String?,
    loading: Boolean,
    role: TextRole,
    modifier: Modifier = Modifier,
) {
    val spacing = KatariTheme.spacing
    OverlayPill(modifier = modifier, style = OverlayStyle.Scrim) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.m, vertical = spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (loading) {
                LoadingIndicator(
                    modifier = Modifier.size(KatariTheme.sizes.iconSmall),
                    tone = ContentTone.OnMedia,
                )
            }
            val timestamp = formatPlaybackTimestamp(positionMs)
            Text(text = chapterName?.let { "$timestamp · $it" } ?: timestamp, role = role, maxLines = 1)
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

// The preview grows with the screen, capped at these shares of its longer side and at the timeline's own width cap.
private const val PREVIEW_MAX_WIDTH_FRACTION = 0.38f
private const val PREVIEW_MAX_HEIGHT_FRACTION = 0.22f
private const val DEFAULT_SEEK_PREVIEW_ASPECT_RATIO = 16f / 9f
