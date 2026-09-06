package eu.kanade.presentation.more.stats.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import tachiyomi.domain.statistics.model.StatisticsCardLayout
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StatisticsLayoutEditor(
    initial: StatisticsCardLayout,
    isOverview: Boolean,
    onDismiss: () -> Unit,
    onSave: (StatisticsCardLayout) -> Unit,
) {
    var draft by remember { mutableStateOf(initial) }
    val cards = statisticsCards(isOverview)
    fun moveVisible(card: tachiyomi.domain.statistics.model.StatisticsCard, offset: Int) {
        val visible = draft.order.filter(cards::contains)
        val target = visible[(visible.indexOf(card) + offset).coerceIn(visible.indices)]
        draft = draft.move(card, draft.order.indexOf(target) - draft.order.indexOf(card))
    }
    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val fromIndex = draft.order.indexOfFirst { it.id == from.key }
        val toIndex = draft.order.indexOfFirst { it.id == to.key }
        if (fromIndex >= 0 && toIndex >= 0) draft = draft.move(draft.order[fromIndex], toIndex - fromIndex)
    }
    val moveUp = stringResource(MR.strings.statistics_move_up)
    val moveDown = stringResource(MR.strings.statistics_move_down)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        ),
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.9f).padding(horizontal = 16.dp)) {
            Text(stringResource(MR.strings.statistics_customize), style = MaterialTheme.typography.titleLarge)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { draft = StatisticsCardLayout() }) {
                    Text(stringResource(MR.strings.statistics_reset_layout))
                }
                TextButton(onClick = { onSave(draft) }) { Text(stringResource(MR.strings.action_save)) }
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                items(draft.order.filter(cards::contains), key = { it.id }) { card ->
                    ReorderableItem(reorderState, card.id) {
                        val label = stringResource(card.label())
                        val scopeLabel = stringResource(
                            if (card.isCurrentLibrary) {
                                MR.strings.statistics_library
                            } else {
                                MR.strings.statistics_activity
                            },
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().semantics {
                                customActions = buildList {
                                    val visible = draft.order.filter(cards::contains)
                                    val index = visible.indexOf(card)
                                    if (index > 0) {
                                        add(
                                            CustomAccessibilityAction(moveUp) {
                                                moveVisible(card, -1)
                                                true
                                            },
                                        )
                                    }
                                    if (index < visible.lastIndex) {
                                        add(
                                            CustomAccessibilityAction(moveDown) {
                                                moveVisible(card, 1)
                                                true
                                            },
                                        )
                                    }
                                }
                            }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = {}, modifier = Modifier.draggableHandle()) {
                                Icon(Icons.Outlined.DragHandle, stringResource(MR.strings.statistics_reorder))
                            }
                            Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                                Text(label)
                                Text(
                                    scopeLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Switch(
                                modifier = Modifier.semantics { contentDescription = label },
                                checked = card !in draft.hidden,
                                onCheckedChange = { checked ->
                                    val hidden = if (checked) draft.hidden - card else draft.hidden + card
                                    draft = draft.copy(hidden = hidden)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
