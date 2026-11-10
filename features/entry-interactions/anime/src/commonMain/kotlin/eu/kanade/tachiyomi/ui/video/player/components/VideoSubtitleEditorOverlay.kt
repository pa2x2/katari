package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.video.engine.spi.player.SubtitleCue
import eu.kanade.tachiyomi.ui.video.player.posture.VideoPlayerPosture
import eu.kanade.tachiyomi.ui.video.player.posture.currentVideoPlayerPosture
import eu.kanade.tachiyomi.ui.video.player.subtitle.VideoSubtitleAppearance
import eu.kanade.tachiyomi.ui.video.player.subtitle.videoSubtitleLayout
import tachiyomi.domain.entry.model.PictureFraming

/**
 * The subtitle editor: [previewCues] where the player would draw subtitles of the [draftAppearance] in a picture of
 * [aspectRatio] framed as [framing], at their real size and place, and the style panel over it. The preview is laid out
 * over the whole player, as the player lays out its subtitles, so where it sits is where they will show. The panel
 * steps aside while the subtitles are dragged, and its handle slides it away to see the whole picture.
 */
@Composable
internal fun VideoSubtitleEditorOverlay(
    draftAppearance: VideoSubtitleAppearance,
    keepInPicture: Boolean,
    aspectRatio: Float?,
    framing: PictureFraming,
    subtitleKeepsOwnStyle: Boolean,
    previewCues: List<SubtitleCue>,
    onDraftChange: (VideoSubtitleAppearance) -> Unit,
    onKeepInPictureChange: (Boolean) -> Unit,
    onDismissRequest: () -> Unit,
    onReset: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pictureAreaSize by remember { mutableStateOf(IntSize.Zero) }
    var drawerExpanded by remember { mutableStateOf(true) }
    var dragging by remember { mutableStateOf(false) }
    val normalizedAppearance = draftAppearance.normalized()
    val gapPx = with(LocalDensity.current) { KatariTheme.spacing.s.toPx() }
    val layout = videoSubtitleLayout(
        playerWidth = pictureAreaSize.width.toFloat(),
        playerHeight = pictureAreaSize.height.toFloat(),
        aspectRatio = aspectRatio,
        framing = framing,
        appearance = normalizedAppearance,
        keepInPicture = keepInPicture,
        gapPx = gapPx,
    )
    // Half-open on a table the picture, and so the subtitles, keep to the upper half
    val posture = currentVideoPlayerPosture()
    val pictureArea = when (posture) {
        is VideoPlayerPosture.Tabletop -> Modifier.fillMaxWidth().height(posture.pictureBottom)
        else -> Modifier.fillMaxSize()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Box(modifier = pictureArea.onSizeChanged { pictureAreaSize = it }) {
            VideoSubtitleEditorPreview(
                appearance = normalizedAppearance,
                layout = layout,
                previewCues = previewCues,
                onDraftChange = onDraftChange,
                onDraggingChange = { dragging = it },
                modifier = Modifier.fillMaxSize(),
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight(0.92f)
                .videoPlayerSafeContentPadding(includeTop = true, includeBottom = true)
                .padding(end = KatariTheme.spacing.m, top = KatariTheme.spacing.m, bottom = KatariTheme.spacing.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedVisibility(
                // Out of the way while subtitles are dragged, as they may be dragged under it
                visible = drawerExpanded && !dragging,
                enter = fadeIn() + expandHorizontally(expandFrom = Alignment.End),
                exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.End),
            ) {
                VideoSubtitleEditorPanel(
                    appearance = normalizedAppearance,
                    subtitleKeepsOwnStyle = subtitleKeepsOwnStyle,
                    keepInPicture = keepInPicture,
                    onDraftChange = onDraftChange,
                    onKeepInPictureChange = onKeepInPictureChange,
                    onReset = onReset,
                    onCancel = onDismissRequest,
                    onDone = onDone,
                    modifier = Modifier.fillMaxHeight(),
                )
            }

            VideoSubtitleEditorPanelHandle(
                expanded = drawerExpanded,
                onToggle = { drawerExpanded = !drawerExpanded },
                modifier = Modifier.padding(start = KatariTheme.spacing.s),
            )
        }
    }
}
