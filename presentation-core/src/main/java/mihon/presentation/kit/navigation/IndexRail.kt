package mihon.presentation.kit.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsEndWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole

/**
 * A narrow strip of short [labels], such as letters, along a list's end edge. Pressing or sliding along it shows the
 * label under the finger enlarged in a bubble beside the strip, and letting go jumps there through [onJump].
 * [currentIndex] highlights the part of the list in view. The strip keeps clear of the system's edge gestures.
 */
@Composable
fun IndexRail(
    labels: List<String>,
    currentIndex: Int?,
    onJump: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var railSize by remember { mutableStateOf(IntSize.Zero) }
    var previewIndex by remember { mutableIntStateOf(-1) }
    val currentOnJump by rememberUpdatedState(onJump)
    val bubbleOffset = with(LocalDensity.current) { IndexBubbleOffset.roundToPx() }
    val colors = KatariTheme.colors

    Row(modifier = modifier.fillMaxHeight()) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(IndexRailWidth)
                .background(colors.surfaceContainerHigh.copy(alpha = INDEX_RAIL_ALPHA))
                .onSizeChanged { railSize = it }
                .pointerInput(labels.size, railSize) {
                    if (labels.isEmpty() || railSize.height == 0) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        previewIndex = indexAt(down.position, railSize, labels.size)
                        var releasedIndex: Int? = null
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (change.isConsumed) break
                            previewIndex = indexAt(change.position, railSize, labels.size)
                            if (!change.pressed) {
                                releasedIndex = previewIndex
                                break
                            }
                            change.consume()
                        }
                        releasedIndex?.let(currentOnJump)
                        previewIndex = -1
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(modifier = Modifier.fillMaxHeight()) {
                labels.forEachIndexed { index, label ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            role = TextRole.Badge,
                            tone = if (previewIndex == index || currentIndex == index) {
                                ContentTone.Primary
                            } else {
                                ContentTone.Secondary
                            },
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            labels.getOrNull(previewIndex)?.let { label ->
                Popup(alignment = Alignment.CenterEnd, offset = IntOffset(-bubbleOffset, 0)) {
                    Surface(container = SurfaceContainer.Primary, shape = KatariTheme.shapes.large) {
                        Text(
                            text = label,
                            modifier = Modifier.padding(
                                horizontal = KatariTheme.spacing.l,
                                vertical = KatariTheme.spacing.s,
                            ),
                            role = TextRole.SectionTitle,
                        )
                    }
                }
            }
        }
        Spacer(
            modifier = Modifier
                .fillMaxHeight()
                .windowInsetsEndWidth(WindowInsets.systemGestures),
        )
    }
}

private fun indexAt(position: Offset, size: IntSize, count: Int): Int {
    return ((position.y / size.height) * count)
        .toInt()
        .coerceIn(0, count - 1)
}

private val IndexRailWidth = 32.dp
private val IndexBubbleOffset = 48.dp

// See-through enough to hint at the list under the strip without hurting the labels' contrast.
private const val INDEX_RAIL_ALPHA = 0.94f
