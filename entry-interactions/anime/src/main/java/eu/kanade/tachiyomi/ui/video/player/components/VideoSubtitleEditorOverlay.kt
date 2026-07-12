package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.text.Cue
import androidx.media3.ui.SubtitleView
import eu.kanade.tachiyomi.ui.video.player.MAX_SUBTITLE_OFFSET_X
import eu.kanade.tachiyomi.ui.video.player.MAX_SUBTITLE_OFFSET_Y
import eu.kanade.tachiyomi.ui.video.player.MAX_SUBTITLE_TEXT_SIZE
import eu.kanade.tachiyomi.ui.video.player.MIN_SUBTITLE_OFFSET_X
import eu.kanade.tachiyomi.ui.video.player.MIN_SUBTITLE_OFFSET_Y
import eu.kanade.tachiyomi.ui.video.player.MIN_SUBTITLE_TEXT_SIZE
import eu.kanade.tachiyomi.ui.video.player.VideoSubtitleAppearance
import eu.kanade.tachiyomi.ui.video.player.applyAppearance
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@OptIn(ExperimentalLayoutApi::class)
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
                .padding(end = 12.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedVisibility(
                visible = drawerExpanded,
                enter = fadeIn() + expandHorizontally(expandFrom = Alignment.End),
                exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.End),
            ) {
                SubtitleEditorDrawer(
                    appearance = normalizedAppearance,
                    onDraftChange = onDraftChange,
                    onReset = onReset,
                    onCancel = onDismissRequest,
                    onDone = onDone,
                    modifier = Modifier
                        .fillMaxHeight()
                        .widthIn(min = 252.dp, max = 292.dp),
                )
            }

            SubtitleEditorDrawerHandle(
                expanded = drawerExpanded,
                onToggle = { drawerExpanded = !drawerExpanded },
                modifier = Modifier.padding(start = 8.dp),
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
    var sampleBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubtitleEditorDrawer(
    appearance: VideoSubtitleAppearance,
    onDraftChange: (VideoSubtitleAppearance) -> Unit,
    onReset: () -> Unit,
    onCancel: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 18.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(MR.strings.anime_playback_subtitle_settings),
                    style = MaterialTheme.typography.titleMedium,
                )

                SliderSection(
                    title = stringResource(MR.strings.anime_playback_subtitle_size),
                    value = appearance.textSize,
                    valueRange = MIN_SUBTITLE_TEXT_SIZE..MAX_SUBTITLE_TEXT_SIZE,
                    onValueChange = { onDraftChange(appearance.copy(textSize = it)) },
                )

                ColorSwatchSection(
                    title = stringResource(MR.strings.anime_playback_subtitle_text_color),
                    selectedColor = appearance.textColor,
                    colors = TEXT_COLOR_SWATCHES,
                    onColorSelected = { onDraftChange(appearance.copy(textColor = it)) },
                )

                ColorSwatchSection(
                    title = stringResource(MR.strings.anime_playback_subtitle_background_color),
                    selectedColor = appearance.backgroundColor,
                    colors = BACKGROUND_COLOR_SWATCHES,
                    onColorSelected = { onDraftChange(appearance.copy(backgroundColor = it)) },
                )

                SliderSection(
                    title = stringResource(MR.strings.anime_playback_subtitle_background_opacity),
                    value = appearance.backgroundOpacity,
                    valueRange = 0f..1f,
                    onValueChange = { onDraftChange(appearance.copy(backgroundOpacity = it)) },
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(onClick = onReset) {
                        Text(text = stringResource(MR.strings.action_reset))
                    }
                    TextButton(onClick = onCancel) {
                        Text(text = stringResource(MR.strings.action_cancel))
                    }
                }
                Button(
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(MR.strings.action_ok))
                }
            }
        }
    }
}

@Composable
private fun SubtitleEditorDrawerHandle(
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .width(36.dp)
            .height(92.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (expanded) {
                    Icons.AutoMirrored.Filled.KeyboardArrowRight
                } else {
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft
                },
                contentDescription = stringResource(MR.strings.anime_playback_subtitle_settings),
            )
        }
    }
}

@Composable
private fun SliderSection(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorSwatchSection(
    title: String,
    selectedColor: Int,
    colors: List<Int>,
    onColorSelected: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            colors.forEach { color ->
                ColorSwatch(
                    color = color,
                    selected = selectedColor == color,
                    onClick = { onColorSelected(color) },
                )
            }
        }
    }
}

@Composable
private fun ColorSwatch(
    color: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val swatchShape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(swatchShape)
            .background(Color(color))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                },
                shape = swatchShape,
            )
            .clickable(onClick = onClick),
    )
}

private val TEXT_COLOR_SWATCHES = listOf(
    android.graphics.Color.WHITE,
    0xFFFFFF99.toInt(),
    0xFFFFC857.toInt(),
    0xFF8BE9FD.toInt(),
    0xFFFF79C6.toInt(),
)

private val BACKGROUND_COLOR_SWATCHES = listOf(
    android.graphics.Color.BLACK,
    0xFF1F2937.toInt(),
    0xFF374151.toInt(),
    0xFF7F1D1D.toInt(),
    0xFF0F766E.toInt(),
)
