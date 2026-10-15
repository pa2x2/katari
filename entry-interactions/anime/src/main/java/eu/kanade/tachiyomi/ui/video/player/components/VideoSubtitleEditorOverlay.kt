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
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import mihon.presentation.kit.theme.KatariTheme

@Composable
internal fun VideoSubtitleEditorOverlay(
    draftAppearance: VideoSubtitleAppearance,
    previewCues: List<Cue>,
    previewText: String,
    onDraftChange: (VideoSubtitleAppearance) -> Unit,
    onDismissRequest: () -> Unit,
    onReset: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var overlaySize by remember { mutableStateOf(IntSize.Zero) }
    var drawerExpanded by remember { mutableStateOf(true) }
    val normalizedAppearance = draftAppearance.normalized()

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { overlaySize = it.size },
    ) {
        SubtitleEditorSample(
            appearance = normalizedAppearance,
            previewCues = previewCues,
            previewText = previewText,
            overlaySize = overlaySize,
            onDraftChange = onDraftChange,
            modifier = Modifier.fillMaxSize(),
        )

        Row(
            modifier = Modifier
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
                    onDraftChange = onDraftChange,
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
    previewCues: List<Cue>,
    previewText: String,
    overlaySize: IntSize,
    onDraftChange: (VideoSubtitleAppearance) -> Unit,
    modifier: Modifier = Modifier,
) {
    val latestAppearance by rememberUpdatedState(appearance)
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
                    setCues(renderedCues)
                }
            },
            update = { subtitleView ->
                subtitleView.applyAppearance(appearance)
                subtitleView.setCues(renderedCues)
            },
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(overlaySize) {
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
                        if (overlaySize.width > 0 && overlaySize.height > 0) {
                            val currentAppearance = latestAppearance
                            onDraftChange(
                                currentAppearance.copy(
                                    offsetX = (currentAppearance.offsetX + (dragAmount.x / overlaySize.width))
                                        .coerceIn(MIN_SUBTITLE_OFFSET_X, MAX_SUBTITLE_OFFSET_X),
                                    offsetY = (currentAppearance.offsetY + (dragAmount.y / overlaySize.height))
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
