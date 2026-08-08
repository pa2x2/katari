package eu.kanade.presentation.entry.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SmallExtendedFloatingActionButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults.rememberTooltipPositionProvider
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.shouldExpandFAB

@Composable
fun EntryContinueActions(
    listState: LazyListState,
    continueTargetListIndex: Int?,
    topOcclusionPx: Int,
    isReading: Boolean,
    visible: Boolean,
    onTargetReached: () -> Unit,
    onContinueReading: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val targetPosition by remember(listState, continueTargetListIndex, topOcclusionPx) {
        derivedStateOf {
            val targetIndex = continueTargetListIndex ?: return@derivedStateOf null
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) {
                null
            } else {
                val targetItem = visibleItems.firstOrNull { it.index == targetIndex }
                val visibleTop = maxOf(layoutInfo.viewportStartOffset, topOcclusionPx)
                val isActuallyVisible = targetItem != null &&
                    targetItem.offset + targetItem.size > visibleTop &&
                    targetItem.offset < layoutInfo.viewportEndOffset
                if (isActuallyVisible) {
                    null
                } else if (
                    targetItem?.let { it.offset + it.size <= visibleTop } == true ||
                    targetIndex < visibleItems.first().index
                ) {
                    EntryContinueTargetPosition.Above
                } else {
                    EntryContinueTargetPosition.Below
                }
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        continueTargetListIndex?.let { targetIndex ->
            val label = stringResource(MR.strings.action_go_to_current_chapter)
            TooltipBox(
                positionProvider = rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                tooltip = { PlainTooltip { Text(label) } },
                state = rememberTooltipState(),
                focusable = false,
            ) {
                SmallFloatingActionButton(
                    onClick = {
                        scope.launch {
                            val layoutInfo = listState.layoutInfo
                            val visibleTop = maxOf(layoutInfo.viewportStartOffset, topOcclusionPx)
                            val usableHeight = (layoutInfo.viewportEndOffset - visibleTop).coerceAtLeast(0)
                            val comfortableTop = visibleTop + usableHeight / 4
                            listState.animateScrollToItem(targetIndex, scrollOffset = -comfortableTop)
                            onTargetReached()
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.animateFloatingActionButton(
                        visible = visible && targetPosition != null,
                        alignment = Alignment.BottomEnd,
                    ),
                ) {
                    Icon(
                        imageVector = when (targetPosition) {
                            EntryContinueTargetPosition.Above -> Icons.Outlined.KeyboardArrowUp
                            EntryContinueTargetPosition.Below, null -> Icons.Outlined.KeyboardArrowDown
                        },
                        contentDescription = label,
                    )
                }
            }
        }

        SmallExtendedFloatingActionButton(
            text = {
                Text(stringResource(if (isReading) MR.strings.action_resume else MR.strings.action_start))
            },
            icon = { Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null) },
            onClick = onContinueReading,
            expanded = listState.shouldExpandFAB(),
            modifier = Modifier.animateFloatingActionButton(
                visible = visible,
                alignment = Alignment.BottomEnd,
            ),
        )
    }
}

private enum class EntryContinueTargetPosition {
    Above,
    Below,
}
