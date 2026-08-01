package eu.kanade.tachiyomi.ui.browse.source.browse.filter

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsEndWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import eu.kanade.tachiyomi.source.entry.EntryFilterNavigationTarget
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.SettingsItemsPaddings
import tachiyomi.presentation.core.i18n.stringResource

internal fun List<EntryFilterNavigationTarget>.usesCompactNavigationRail(): Boolean {
    return size in 3..MAXIMUM_COMPACT_NAVIGATION_TARGETS && all { it.label.length <= 2 }
}

@Composable
internal fun PagedFilterNavigationMenu(
    targets: List<EntryFilterNavigationTarget>,
    onJump: (EntryFilterNavigationTarget) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.padding(horizontal = SettingsItemsPaddings.Horizontal)) {
        TextButton(onClick = { expanded = true }) {
            Icon(Icons.Outlined.UnfoldMore, contentDescription = null)
            Text(stringResource(MR.strings.browse_filter_jump_to))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                items(targets, key = EntryFilterNavigationTarget::id) { target ->
                    Row(
                        modifier = Modifier
                            .clickable {
                                expanded = false
                                onJump(target)
                            }
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Text(target.label)
                    }
                }
            }
        }
    }
}

@Composable
internal fun RowScope.PagedFilterNavigationRail(
    targets: List<EntryFilterNavigationTarget>,
    currentTargetId: String?,
    onJump: (EntryFilterNavigationTarget) -> Unit,
) {
    var railSize by remember { mutableStateOf(IntSize.Zero) }
    var previewIndex by remember { mutableIntStateOf(-1) }
    val bubbleOffset = with(LocalDensity.current) { NAVIGATION_BUBBLE_OFFSET.roundToPx() }

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(NAVIGATION_RAIL_WIDTH)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f))
            .onSizeChanged { railSize = it }
            .pointerInput(targets, railSize) {
                if (targets.isEmpty() || railSize.height == 0) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown()
                    previewIndex = navigationIndex(down.position, railSize, targets.size)
                    var releasedIndex: Int? = null
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (change.isConsumed) break
                        previewIndex = navigationIndex(change.position, railSize, targets.size)
                        if (!change.pressed) {
                            releasedIndex = previewIndex
                            break
                        }
                        change.consume()
                    }
                    releasedIndex?.let { targets.getOrNull(it)?.let(onJump) }
                    previewIndex = -1
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(modifier = Modifier.fillMaxHeight()) {
            targets.forEachIndexed { index, target ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = target.label,
                        color = when {
                            previewIndex == index -> MaterialTheme.colorScheme.primary
                            currentTargetId == target.id -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        targets.getOrNull(previewIndex)?.let { target ->
            Popup(
                alignment = Alignment.CenterEnd,
                offset = IntOffset(-bubbleOffset, 0),
            ) {
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.large)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = target.label,
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleMedium,
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

private fun navigationIndex(position: Offset, size: IntSize, targetCount: Int): Int {
    return ((position.y / size.height) * targetCount)
        .toInt()
        .coerceIn(0, targetCount - 1)
}

private val NAVIGATION_RAIL_WIDTH = 32.dp
private val NAVIGATION_BUBBLE_OFFSET = 48.dp
private const val MAXIMUM_COMPACT_NAVIGATION_TARGETS = 32
