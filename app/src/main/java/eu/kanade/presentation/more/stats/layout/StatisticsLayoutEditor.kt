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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import eu.kanade.presentation.components.GuideButton
import eu.kanade.presentation.more.stats.components.rememberStatisticsDurationFormatter
import eu.kanade.presentation.more.stats.data.StatsRecapNotifications
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.DragHandle
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.chip.FilterChip
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.control.Switch
import mihon.presentation.kit.list.RowGroupHeader
import mihon.presentation.kit.list.RowGroupItem
import mihon.presentation.kit.list.RowGroupPosition
import mihon.presentation.kit.list.SettingsRow
import mihon.presentation.kit.sheet.Sheet
import mihon.presentation.kit.sheet.SheetHeader
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import tachiyomi.domain.statistics.model.StatisticsCard
import tachiyomi.domain.statistics.model.StatisticsCardLayout
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun StatisticsLayoutEditor(
    initial: StatisticsCardLayout,
    initialGoalMinutes: Int,
    initialRecapNotifications: StatsRecapNotifications,
    isOverview: Boolean,
    onDismiss: () -> Unit,
    onSave: (StatisticsCardLayout, goalMinutes: Int, StatsRecapNotifications) -> Unit,
) {
    var draft by remember { mutableStateOf(initial) }
    var goalMinutes by remember { mutableIntStateOf(initialGoalMinutes) }
    var recapNotifications by remember { mutableStateOf(initialRecapNotifications) }
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
    Sheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(SHEET_HEIGHT_FRACTION)) {
            SheetHeader(
                title = stringResource(MR.strings.statistics_customize),
                actions = { GuideButton(page = "statistics/") },
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = KatariTheme.spacing.s),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Button(
                    label = stringResource(MR.strings.statistics_reset_layout),
                    onClick = { draft = StatisticsCardLayout() },
                    style = ButtonStyle.Text,
                )
                Button(
                    label = stringResource(MR.strings.action_save),
                    onClick = { onSave(draft, goalMinutes, recapNotifications) },
                    style = ButtonStyle.Text,
                )
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = KatariTheme.spacing.xl),
            ) {
                StatisticsCardGroup.entries.forEach { group ->
                    val section = sectionCards(group)
                    if (section.isEmpty()) return@forEach
                    item(key = "section-${group.name}") {
                        RowGroupHeader(stringResource(group.label), Modifier.semantics { heading() })
                    }
                    itemsIndexed(section, key = { _, card -> card.id }) { index, card ->
                        ReorderableItem(reorderState, card.id) { isDragging ->
                            val label = stringResource(card.label())
                            val shown = card !in draft.hidden
                            RowGroupItem(
                                position = RowGroupPosition.of(index, section.size),
                                lifted = isDragging,
                            ) {
                                SettingsRow(
                                    title = label,
                                    modifier = Modifier.semantics {
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
                                    },
                                    subtitle = card.availabilityHint()?.let { stringResource(it) },
                                    leading = {
                                        IconButton(
                                            icon = MaterialSymbols.Rounded.DragHandle,
                                            contentDescription = stringResource(MR.strings.statistics_reorder),
                                            onClick = {},
                                            modifier = Modifier.draggableHandle(),
                                        )
                                    },
                                    trailing = { Switch(checked = shown, onCheckedChange = null) },
                                    role = Role.Switch,
                                    onClick = {
                                        val hidden = if (shown) draft.hidden + card else draft.hidden - card
                                        draft = draft.copy(hidden = hidden)
                                    },
                                )
                            }
                        }
                    }
                }
                item(key = "section-goal") {
                    RowGroupHeader(
                        stringResource(MR.strings.statistics_goal_section),
                        Modifier.semantics { heading() },
                    )
                }
                item(key = "daily-goal") {
                    RowGroupItem(position = RowGroupPosition.First) {
                        DailyGoalSetting(goalMinutes = goalMinutes, onGoalMinutesChange = { goalMinutes = it })
                    }
                }
                item(key = "monthly-recap") {
                    RowGroupItem(position = RowGroupPosition.Middle) {
                        RecapNotificationSetting(
                            label = stringResource(MR.strings.statistics_monthly_recap),
                            summary = stringResource(MR.strings.statistics_monthly_recap_summary),
                            enabled = recapNotifications.monthly,
                            onEnabledChange = { recapNotifications = recapNotifications.copy(monthly = it) },
                        )
                    }
                }
                item(key = "yearly-recap") {
                    RowGroupItem(position = RowGroupPosition.Last) {
                        RecapNotificationSetting(
                            label = stringResource(MR.strings.statistics_yearly_recap),
                            summary = stringResource(MR.strings.statistics_yearly_recap_summary),
                            enabled = recapNotifications.yearly,
                            onEnabledChange = { recapNotifications = recapNotifications.copy(yearly = it) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyGoalSetting(goalMinutes: Int, onGoalMinutesChange: (Int) -> Unit) {
    val formatDuration = rememberStatisticsDurationFormatter()
    SettingsRow(
        title = stringResource(MR.strings.statistics_daily_goal),
        subtitle = stringResource(MR.strings.statistics_daily_goal_summary),
        below = {
            FlowRow(
                modifier = Modifier.padding(top = KatariTheme.spacing.s),
                horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
            ) {
                DAILY_GOAL_MINUTES.forEach { minutes ->
                    FilterChip(
                        label = if (minutes == 0) {
                            stringResource(MR.strings.statistics_goal_off)
                        } else {
                            formatDuration(minutes * 60_000L)
                        },
                        selected = minutes == goalMinutes,
                        onClick = { onGoalMinutesChange(minutes) },
                    )
                }
            }
        },
    )
}

@Composable
private fun RecapNotificationSetting(
    label: String,
    summary: String,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
) {
    SettingsRow(
        title = label,
        subtitle = summary,
        trailing = { Switch(checked = enabled, onCheckedChange = null) },
        role = Role.Switch,
        onClick = { onEnabledChange(!enabled) },
    )
}

private val DAILY_GOAL_MINUTES = listOf(0, 15, 30, 45, 60, 120)

/** How much of the window the sheet takes, so the card list scrolls inside it rather than growing with it. */
private const val SHEET_HEIGHT_FRACTION = 0.9f
