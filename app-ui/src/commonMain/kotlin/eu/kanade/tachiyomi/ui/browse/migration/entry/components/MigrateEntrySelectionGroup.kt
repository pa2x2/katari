package eu.kanade.tachiyomi.ui.browse.migration.entry.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.ui.browse.migration.entry.models.MigrationEntrySelectionAvailability
import eu.kanade.tachiyomi.ui.browse.migration.entry.models.MigrationEntrySelectionGroup
import mihon.presentation.kit.content.BrowseMetrics
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.list.treeBranch
import mihon.presentation.kit.list.treeStem
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.domain.entry.model.Entry
import tachiyomi.i18n.*
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
    onInspect: (Long, Boolean) -> Unit,
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
            onInspect = { onInspect(member.entry.id, false) },
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
    val rootCoverHalfHeight = migrationCoverHeight(itemOrientation) / 2

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
            onInspect = { onInspect(root.entry.id, false) },
            expanded = expanded,
            onToggleExpanded = { expanded = !expanded },
            modifier = if (expanded) {
                Modifier.treeStem(x = TreeConnectorX, stemTop = rootCoverHalfHeight)
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
                                    onInspect = { onInspect(member.entry.id, true) },
                                    modifier = childModifier,
                                )
                            }
                            MigrationEntrySelectionAvailability.OTHER_SOURCE,
                            MigrationEntrySelectionAvailability.UNAVAILABLE,
                            -> {
                                MigrationEntryMergeContextItem(
                                    entry = member.entry,
                                    itemOrientation = itemOrientation,
                                    sourceName = member.sourceName,
                                    mergeRole = stringResource(MR.strings.label_member),
                                    unavailable = member.availability ==
                                        MigrationEntrySelectionAvailability.UNAVAILABLE,
                                    onInspect = { onInspect(member.entry.id, true) },
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .treeBranch(x = TreeConnectorX, last = isLast),
    ) {
        content(Modifier.padding(start = TreeConnectorX))
    }
}

@Composable
private fun MigrationEntryMergeContextItem(
    entry: Entry,
    itemOrientation: EntryItemOrientation,
    sourceName: String,
    mergeRole: String,
    unavailable: Boolean,
    onInspect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = BrowseMetrics.listCoverRowMinHeight)
            .clickable(onClick = onInspect)
            .padding(horizontal = KatariTheme.spacing.contentInset, vertical = KatariTheme.spacing.s),
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.l),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MigrateEntrySelectionCover(
            entry = entry,
            itemOrientation = itemOrientation,
            onInspect = onInspect,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
        ) {
            Text(
                text = entry.displayTitle,
                role = TextRole.Body,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
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
                role = TextRole.Supporting,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// The vertical line linking a merged group's members runs under the middle of the root's cover slot.
private val TreeConnectorX = KatariTheme.spacing.contentInset + BrowseMetrics.listCoverSlotWidth / 2
