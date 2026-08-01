package eu.kanade.tachiyomi.ui.browse.migration.entry.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.ui.browse.migration.entry.models.MigrationEntrySelectionAvailability
import eu.kanade.tachiyomi.ui.browse.migration.entry.models.MigrationEntrySelectionGroup
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun MigrateEntrySelectionGroup(
    group: MigrationEntrySelectionGroup,
    itemOrientation: EntryItemOrientation,
    selection: Set<Long>,
    onToggleSelection: (Long) -> Unit,
    onToggleGroup: (Set<Long>) -> Unit,
    onLongClickItem: (Long) -> Unit,
    onInspect: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!group.isMerged) {
        val member = group.eligibleMembers.single()
        MigrateEntrySelectionItem(
            entry = member.entry,
            itemOrientation = itemOrientation,
            consumedCount = member.progress.consumedCount,
            totalCount = member.progress.totalCount,
            isSelected = member.entry.id in selection,
            onToggleSelection = { onToggleSelection(member.entry.id) },
            onLongClick = { onLongClickItem(member.entry.id) },
            onInspect = { onInspect(member.entry.id) },
            modifier = modifier,
        )
        return
    }

    val root = group.members.first { it.entry.id == group.rootEntryId }
    val children = group.members.filterNot { it.entry.id == group.rootEntryId }
    val selectedMemberCount = group.eligibleEntryIds.count(selection::contains)
    val groupSelectionState = when (selectedMemberCount) {
        0 -> ToggleableState.Off
        group.eligibleEntryIds.size -> ToggleableState.On
        else -> ToggleableState.Indeterminate
    }
    var expanded by rememberSaveable(group.key) { mutableStateOf(true) }
    val childCount = group.totalMemberCount - 1
    val rootSummary = pluralStringResource(
        MR.plurals.migrationEntriesScreen_mergedGroupSummary,
        childCount,
        childCount,
    )
    val rootRole = when (root.availability) {
        MigrationEntrySelectionAvailability.ELIGIBLE -> rootSummary
        MigrationEntrySelectionAvailability.OTHER_SOURCE -> stringResource(
            MR.strings.migrationEntriesScreen_retainedMergedMember,
            root.sourceName,
            rootSummary,
        )
        MigrationEntrySelectionAvailability.UNAVAILABLE -> stringResource(
            MR.strings.migrationEntriesScreen_unavailableMergedMember,
            root.sourceName,
            rootSummary,
        )
    }
    val connectorColor = MaterialTheme.colorScheme.outlineVariant

    Column(modifier = modifier) {
        MigrateEntrySelectionItem(
            entry = root.entry,
            itemOrientation = itemOrientation,
            consumedCount = root.progress.consumedCount,
            totalCount = root.progress.totalCount,
            mergeRole = rootRole,
            isSelected = groupSelectionState != ToggleableState.Off,
            selectionState = groupSelectionState,
            onToggleSelection = { onToggleGroup(group.eligibleEntryIds) },
            onLongClick = null,
            onInspect = { onInspect(root.entry.id) },
            expanded = expanded,
            onToggleExpanded = { expanded = !expanded },
            modifier = if (expanded) {
                Modifier.drawWithContent {
                    drawContent()
                    val connectorX = 26.dp.toPx()
                    drawLine(
                        color = connectorColor,
                        start = Offset(connectorX, size.height / 2 + 10.dp.toPx()),
                        end = Offset(connectorX, size.height),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
            } else {
                Modifier
            },
        )

        AnimatedVisibility(visible = expanded) {
            Column {
                children.forEachIndexed { index, member ->
                    MigrationEntryTreeChild(isLast = index == children.lastIndex) { childModifier ->
                        when (member.availability) {
                            MigrationEntrySelectionAvailability.ELIGIBLE -> {
                                MigrateEntrySelectionItem(
                                    entry = member.entry,
                                    itemOrientation = itemOrientation,
                                    consumedCount = member.progress.consumedCount,
                                    totalCount = member.progress.totalCount,
                                    mergeRole = stringResource(MR.strings.label_member),
                                    isSelected = member.entry.id in selection,
                                    onToggleSelection = { onToggleSelection(member.entry.id) },
                                    onLongClick = { onLongClickItem(member.entry.id) },
                                    onInspect = { onInspect(member.entry.id) },
                                    modifier = childModifier,
                                )
                            }
                            MigrationEntrySelectionAvailability.OTHER_SOURCE,
                            MigrationEntrySelectionAvailability.UNAVAILABLE,
                            -> {
                                MigrationEntryMergeContextItem(
                                    title = member.entry.displayTitle,
                                    sourceName = member.sourceName,
                                    mergeRole = stringResource(MR.strings.label_member),
                                    unavailable = member.availability ==
                                        MigrationEntrySelectionAvailability.UNAVAILABLE,
                                    onInspect = { onInspect(member.entry.id) },
                                    modifier = childModifier,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MigrationEntryTreeChild(
    isLast: Boolean,
    content: @Composable (Modifier) -> Unit,
) {
    val connectorColor = MaterialTheme.colorScheme.outlineVariant
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val branchX = 26.dp.toPx()
                val branchEndX = 52.dp.toPx()
                val branchY = size.height / 2
                val strokeWidth = 1.dp.toPx()
                drawLine(
                    color = connectorColor,
                    start = Offset(branchX, 0f),
                    end = Offset(branchX, if (isLast) branchY else size.height),
                    strokeWidth = strokeWidth,
                )
                drawLine(
                    color = connectorColor,
                    start = Offset(branchX, branchY),
                    end = Offset(branchEndX, branchY),
                    strokeWidth = strokeWidth,
                )
            },
    ) {
        content(Modifier.padding(start = 52.dp))
    }
}

@Composable
private fun MigrationEntryMergeContextItem(
    title: String,
    sourceName: String,
    mergeRole: String,
    unavailable: Boolean,
    onInspect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onInspect)
            .padding(MaterialTheme.padding.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
        ) {
            Text(
                text = title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(
                    if (unavailable) {
                        MR.strings.migrationEntriesScreen_unavailableMergedMember
                    } else {
                        MR.strings.migrationEntriesScreen_retainedMergedMember
                    },
                    sourceName,
                    mergeRole,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
