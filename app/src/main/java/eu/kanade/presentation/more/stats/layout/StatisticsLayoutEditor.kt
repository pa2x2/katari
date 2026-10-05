package eu.kanade.presentation.more.stats.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.stats.components.rememberStatisticsDurationFormatter
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import tachiyomi.domain.statistics.model.StatisticsCard
import tachiyomi.domain.statistics.model.StatisticsCardLayout
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StatisticsLayoutEditor(
    initial: StatisticsCardLayout,
    initialGoalMinutes: Int,
    initialMonthlyRecap: Boolean,
    isOverview: Boolean,
    onDismiss: () -> Unit,
    onSave: (StatisticsCardLayout, goalMinutes: Int, monthlyRecap: Boolean) -> Unit,
) {
    var draft by remember { mutableStateOf(initial) }
    var goalMinutes by remember { mutableIntStateOf(initialGoalMinutes) }
    var monthlyRecap by remember { mutableStateOf(initialMonthlyRecap) }
    val cards = statisticsCards(isOverview)
    fun sectionCards(group: StatisticsCardGroup) = draft.order.filter { it in cards && it.group == group }

    // Cards only move within their section, so both drag and accessibility moves resolve against it.
    fun moveWithinSection(card: StatisticsCard, target: StatisticsCard) {
        if (card.group != target.group) return
        draft = draft.move(card, draft.order.indexOf(target) - draft.order.indexOf(card))
    }
    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val card = cards.firstOrNull { it.id == from.key } ?: return@rememberReorderableLazyListState
        val target = cards.firstOrNull { it.id == to.key } ?: return@rememberReorderableLazyListState
        moveWithinSection(card, target)
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
                TextButton(onClick = {
                    onSave(draft, goalMinutes, monthlyRecap)
                }) { Text(stringResource(MR.strings.action_save)) }
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                StatisticsCardGroup.entries.forEach { group ->
                    val section = sectionCards(group)
                    if (section.isEmpty()) return@forEach
                    item(key = "section-${group.name}") {
                        Text(
                            text = stringResource(group.label),
                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp).semantics { heading() },
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    items(section, key = { it.id }) { card ->
                        ReorderableItem(reorderState, card.id) {
                            val label = stringResource(card.label())
                            val index = section.indexOf(card)
                            Row(
                                modifier = Modifier.fillMaxWidth().semantics {
                                    customActions = buildList {
                                        section.getOrNull(index - 1)?.let { previous ->
                                            add(
                                                CustomAccessibilityAction(moveUp) {
                                                    moveWithinSection(card, previous)
                                                    true
                                                },
                                            )
                                        }
                                        section.getOrNull(index + 1)?.let { next ->
                                            add(
                                                CustomAccessibilityAction(moveDown) {
                                                    moveWithinSection(card, next)
                                                    true
                                                },
                                            )
                                        }
                                    }
                                }.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                IconButton(onClick = {}, modifier = Modifier.draggableHandle()) {
                                    Icon(Icons.Outlined.DragHandle, stringResource(MR.strings.statistics_reorder))
                                }
                                Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                                    Text(label)
                                    card.availabilityHint()?.let { hint ->
                                        Text(
                                            text = stringResource(hint),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
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
                item(key = "section-goal") {
                    Text(
                        text = stringResource(MR.strings.statistics_goal_section),
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp).semantics { heading() },
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                item(key = "daily-goal") {
                    DailyGoalSetting(goalMinutes = goalMinutes, onGoalMinutesChange = { goalMinutes = it })
                }
                item(key = "monthly-recap") {
                    MonthlyRecapSetting(enabled = monthlyRecap, onEnabledChange = { monthlyRecap = it })
                }
            }
        }
    }
}

@Composable
private fun DailyGoalSetting(goalMinutes: Int, onGoalMinutesChange: (Int) -> Unit) {
    val formatDuration = rememberStatisticsDurationFormatter()
    Column(Modifier.padding(vertical = 4.dp)) {
        Text(stringResource(MR.strings.statistics_daily_goal))
        Text(
            text = stringResource(MR.strings.statistics_daily_goal_summary),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DAILY_GOAL_MINUTES.forEach { minutes ->
                FilterChip(
                    selected = minutes == goalMinutes,
                    onClick = { onGoalMinutesChange(minutes) },
                    label = {
                        Text(
                            if (minutes == 0) {
                                stringResource(MR.strings.statistics_goal_off)
                            } else {
                                formatDuration(minutes * 60_000L)
                            },
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun MonthlyRecapSetting(enabled: Boolean, onEnabledChange: (Boolean) -> Unit) {
    val label = stringResource(MR.strings.statistics_monthly_recap)
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text(label)
            Text(
                text = stringResource(MR.strings.statistics_monthly_recap_summary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            modifier = Modifier.semantics { contentDescription = label },
            checked = enabled,
            onCheckedChange = onEnabledChange,
        )
    }
}

private val DAILY_GOAL_MINUTES = listOf(0, 15, 30, 45, 60, 90, 120)
