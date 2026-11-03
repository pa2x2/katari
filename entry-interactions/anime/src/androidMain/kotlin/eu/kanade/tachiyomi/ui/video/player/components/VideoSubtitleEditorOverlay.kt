package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.text.Cue
import androidx.media3.ui.SubtitleView
import eu.kanade.tachiyomi.ui.video.player.MAX_SUBTITLE_OFFSET_X
import eu.kanade.tachiyomi.ui.video.player.MAX_SUBTITLE_OFFSET_Y
import eu.kanade.tachiyomi.ui.video.player.MIN_SUBTITLE_OFFSET_X
import eu.kanade.tachiyomi.ui.video.player.MIN_SUBTITLE_OFFSET_Y
import eu.kanade.tachiyomi.ui.video.player.VideoSubtitleAppearance
import eu.kanade.tachiyomi.ui.video.player.applyAppearance
import eu.kanade.tachiyomi.ui.video.player.applyLayout
import eu.kanade.tachiyomi.ui.video.player.posture.VideoPlayerPosture
import eu.kanade.tachiyomi.ui.video.player.posture.currentVideoPlayerPosture
import eu.kanade.tachiyomi.ui.video.player.subtitle.VideoControlsFootprint
import eu.kanade.tachiyomi.ui.video.player.subtitle.VideoSubtitleLayout
import eu.kanade.tachiyomi.ui.video.player.subtitle.videoSubtitleLayout
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.domain.entry.model.PictureFraming

/**
 * The subtitle editor: a sample line where the player would draw subtitles of the [draftAppearance] in a picture of
 * [aspectRatio] framed as [framing], at their real size, and the style panel beside it. The sample keeps to the space
 * the panel leaves, so it can be seen and dragged while the panel is open.
 */
@Composable
internal fun VideoSubtitleEditorOverlay(
    draftAppearance: VideoSubtitleAppearance,
    keepInPicture: Boolean,
    aspectRatio: Float?,
    framing: PictureFraming,
    subtitleKeepsOwnStyle: Boolean,
    previewCues: List<Cue>,
    previewText: String,
    onDraftChange: (VideoSubtitleAppearance) -> Unit,
    onKeepInPictureChange: (Boolean) -> Unit,
    onDismissRequest: () -> Unit,
    onReset: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pictureAreaSize by remember { mutableStateOf(IntSize.Zero) }
    var panelWidth by remember { mutableIntStateOf(0) }
    var drawerExpanded by remember { mutableStateOf(true) }
    val normalizedAppearance = draftAppearance.normalized()
    val gapPx = with(LocalDensity.current) { KatariTheme.spacing.s.toPx() }
    val layout = videoSubtitleLayout(
        playerWidth = pictureAreaSize.width.toFloat(),
        playerHeight = pictureAreaSize.height.toFloat(),
        aspectRatio = aspectRatio,
        framing = framing,
        appearance = normalizedAppearance,
        keepInPicture = keepInPicture,
        controls = VideoControlsFootprint.None,
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
            SubtitleEditorSample(
                appearance = normalizedAppearance,
                layout = layout,
                previewCues = previewCues,
                previewText = previewText,
                onDraftChange = onDraftChange,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = with(LocalDensity.current) { panelWidth.toDp() }),
            )
        }

        Row(
            modifier = Modifier
                .onSizeChanged { panelWidth = it.width }
                .align(Alignment.CenterEnd)
                .fillMaxHeight(0.92f)
                .videoPlayerSafeContentPadding(includeTop = true, includeBottom = true)
                .padding(end = KatariTheme.spacing.m, top = KatariTheme.spacing.m, bottom = KatariTheme.spacing.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedVisibility(
                visible = drawerExpanded,
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

@Composable
private fun SubtitleEditorSample(
    appearance: VideoSubtitleAppearance,
    layout: VideoSubtitleLayout,
    previewCues: List<Cue>,
    previewText: String,
    onDraftChange: (VideoSubtitleAppearance) -> Unit,
    modifier: Modifier = Modifier,
) {
    val latestAppearance by rememberUpdatedState(appearance)
    val latestLayout by rememberUpdatedState(layout)
    var sampleBounds by remember { mutableStateOf<Rect?>(null) }
    val fallbackCues = remember(previewText) {
        listOf(
            Cue.Builder()
                .setText(previewText)
                .build(),
        )
    }
    val renderedCues = if (previewCues.isNotEmpty()) previewCues else fallbackCues

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { sampleBounds = it.boundsInRoot() },
            factory = { context ->
                SubtitleView(context).apply {
                    applyAppearance(appearance)
                    applyLayout(layout, appearance, viewCoversPlayer = true)
                    setCues(renderedCues)
                }
            },
            update = { subtitleView ->
                subtitleView.applyAppearance(appearance)
                subtitleView.applyLayout(layout, appearance, viewCoversPlayer = true)
                subtitleView.setCues(renderedCues)
            },
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(Unit) {
                    var dragging = false
                    detectDragGestures(
                        onDragStart = { offset ->
                            dragging = sampleBounds?.contains(offset) == true
                        },
                        onDragCancel = {
                            dragging = false
                        },
                        onDragEnd = {
                            dragging = false
                        },
                    ) { change, dragAmount ->
                        if (!dragging) {
                            return@detectDragGestures
                        }
                        val base = latestLayout
                        if (base.offsetBaseWidth > 0f && base.offsetBaseHeight > 0f) {
                            val currentAppearance = latestAppearance
                            onDraftChange(
                                currentAppearance.copy(
                                    offsetX = (currentAppearance.offsetX + (dragAmount.x / base.offsetBaseWidth))
                                        .coerceIn(MIN_SUBTITLE_OFFSET_X, MAX_SUBTITLE_OFFSET_X),
                                    offsetY = (currentAppearance.offsetY + (dragAmount.y / base.offsetBaseHeight))
                                        .coerceIn(MIN_SUBTITLE_OFFSET_Y, MAX_SUBTITLE_OFFSET_Y),
                                ),
                            )
                            change.consume()
                        } else {
                            dragging = false
                        }
                    }
                },
        )
    }
}
