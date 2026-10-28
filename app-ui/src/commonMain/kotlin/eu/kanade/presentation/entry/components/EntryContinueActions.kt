package eu.kanade.presentation.entry.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.presentation.util.formatChapterNumber
import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.launch
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.KeyboardArrowDown
import mihon.icons.materialsymbols.rounded.KeyboardArrowUp
import mihon.icons.materialsymbols.roundedfilled.PlayArrow
import mihon.presentation.kit.button.FloatingActionButton
import mihon.presentation.kit.button.SmallFloatingActionButton
import mihon.presentation.kit.feedback.Tooltip
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.shouldExpandFAB

@Composable
fun EntryContinueActions(
    listState: LazyListState,
    continueTargetListIndex: Int?,
    topOcclusionPx: Int,
    isReading: Boolean,
    targetChapter: EntryChapter?,
    entryType: EntryType,
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
        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
    ) {
        continueTargetListIndex?.let { targetIndex ->
            val label = stringResource(MR.strings.action_go_to_current_chapter)
            Tooltip(text = label) {
                SmallFloatingActionButton(
                    icon = when (targetPosition) {
                        EntryContinueTargetPosition.Above -> MaterialSymbols.Rounded.KeyboardArrowUp
                        EntryContinueTargetPosition.Below, null -> MaterialSymbols.Rounded.KeyboardArrowDown
                    },
                    contentDescription = label,
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
                    visible = visible && targetPosition != null,
                )
            }
        }

        FloatingActionButton(
            label = continueLabel(isReading, targetChapter, entryType),
            icon = MaterialSymbols.RoundedFilled.PlayArrow,
            onClick = onContinueReading,
            expanded = listState.shouldExpandFAB(),
            visible = visible,
        )
    }
}

/** Names the chapter it continues with, such as "Resume · Ch. 861", when that chapter has a number to show. */
@Composable
private fun continueLabel(isReading: Boolean, targetChapter: EntryChapter?, entryType: EntryType): String {
    val number = targetChapter
        ?.takeIf { it.isRecognizedNumber }
        ?.let {
            stringResource(
                entryType.entryTypePresentation().childNumberShortLabel,
                formatChapterNumber(it.chapterNumber),
            )
        }
    return when {
        number == null -> stringResource(if (isReading) MR.strings.action_resume else MR.strings.action_start)
        isReading -> stringResource(MR.strings.action_resume_child, number)
        else -> stringResource(MR.strings.action_start_child, number)
    }
}

private enum class EntryContinueTargetPosition {
    Above,
    Below,
}
