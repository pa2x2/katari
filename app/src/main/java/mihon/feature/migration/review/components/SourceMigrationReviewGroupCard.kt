package mihon.feature.migration.review.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mihon.feature.migration.review.SourceMigrationReviewGroup
import mihon.feature.migration.review.SourceMigrationReviewMapping
import mihon.feature.migration.review.SourceMigrationReviewMember
import mihon.feature.migration.session.model.SourceMigrationItemState
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.Badge
import tachiyomi.presentation.core.components.BadgeGroup
import tachiyomi.presentation.core.components.ListGroupHeader
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun SourceMigrationReviewGroupCard(
    group: SourceMigrationReviewGroup,
    onIncludedChange: (sourceEntryId: Long, included: Boolean) -> Unit,
    onToggleGroup: (groupId: Long) -> Unit,
    modifier: Modifier = Modifier,
    onTargetClick: ((sourceEntryId: Long) -> Unit)? = null,
    onSourceDetailsClick: (entryId: Long) -> Unit,
    onTargetDetailsClick: (entryId: Long) -> Unit,
) {
    val replacements = group.mappings.filter { mapping -> mapping.item.targetTitle != null }
    val unresolved = group.mappings.filter { mapping -> mapping.item.targetTitle == null }

    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column {
            if (group.memberCount > 1) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = MaterialTheme.padding.medium,
                        vertical = MaterialTheme.padding.small,
                    ),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
                ) {
                    Text(
                        text = group.title,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(
                            MR.strings.migrationEntriesScreen_selectedCount,
                            group.mappings.size,
                            group.memberCount,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            if (replacements.isNotEmpty()) {
                SourceMigrationSectionHeader(
                    title = stringResource(MR.strings.sourceMigrationReview_replacements, replacements.size),
                    action = if (group.readyMappings.isNotEmpty()) {
                        if (group.allReadyMappingsIncluded) {
                            stringResource(MR.strings.migrationConfigScreen_selectNoneLabel)
                        } else {
                            stringResource(MR.strings.action_select_all)
                        }
                    } else {
                        null
                    },
                    onActionClick = { onToggleGroup(group.id) },
                )
                replacements.forEachIndexed { index, mapping ->
                    if (index > 0) HorizontalDivider()
                    SourceMigrationMapping(
                        mapping = mapping,
                        onIncludedChange = onIncludedChange,
                        onTargetClick = onTargetClick,
                        onSourceDetailsClick = onSourceDetailsClick,
                        onTargetDetailsClick = onTargetDetailsClick,
                    )
                }
            }

            if (unresolved.isNotEmpty()) {
                SourceMigrationSectionHeader(
                    title = stringResource(MR.strings.sourceMigrationReview_noMatch, unresolved.size),
                )
                unresolved.forEachIndexed { index, mapping ->
                    if (index > 0) HorizontalDivider()
                    SourceMigrationMapping(
                        mapping = mapping,
                        onIncludedChange = onIncludedChange,
                        onTargetClick = onTargetClick,
                        onSourceDetailsClick = onSourceDetailsClick,
                        onTargetDetailsClick = onTargetDetailsClick,
                    )
                }
            }

            if (group.notSelectedMembers.isNotEmpty()) {
                SourceMigrationSectionHeader(
                    title = stringResource(
                        MR.strings.sourceMigrationReview_notSelected,
                        group.notSelectedMembers.size,
                    ),
                )
                group.notSelectedMembers.forEachIndexed { index, member ->
                    if (index > 0) HorizontalDivider()
                    SourceMigrationNotSelectedMember(member, onSourceDetailsClick)
                }
            }
        }
    }
}

@Composable
private fun SourceMigrationSectionHeader(
    title: String,
    action: String? = null,
    onActionClick: () -> Unit = {},
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ListGroupHeader(
            text = title,
            modifier = Modifier.weight(1f),
        )
        action?.let {
            TextButton(
                onClick = onActionClick,
                modifier = Modifier.padding(end = MaterialTheme.padding.small),
            ) {
                Text(text = it)
            }
        }
    }
}

@Composable
private fun SourceMigrationMapping(
    mapping: SourceMigrationReviewMapping,
    onIncludedChange: (sourceEntryId: Long, included: Boolean) -> Unit,
    onTargetClick: ((sourceEntryId: Long) -> Unit)?,
    onSourceDetailsClick: (entryId: Long) -> Unit,
    onTargetDetailsClick: (entryId: Long) -> Unit,
) {
    val item = mapping.item
    Column(
        modifier = Modifier.padding(vertical = MaterialTheme.padding.small),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
    ) {
        SourceMigrationEntryListItem(
            title = item.sourceTitle,
            thumbnailUrl = item.sourceThumbnailUrl,
            supportingContent = {
                Text(
                    text = mapping.sourceName,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SourceMigrationStateBadge(item.state)
                    if (item.state == SourceMigrationItemState.READY) {
                        Checkbox(
                            checked = item.included,
                            onCheckedChange = { included ->
                                onIncludedChange(item.sourceEntryId, included)
                            },
                        )
                    }
                    SourceMigrationEntryDetailsButton {
                        onSourceDetailsClick(item.sourceEntryId)
                    }
                }
            },
        )

        Icon(
            imageVector = Icons.Outlined.ArrowDownward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(start = MaterialTheme.padding.medium)
                .size(20.dp),
        )

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .padding(horizontal = MaterialTheme.padding.medium)
                .fillMaxWidth(),
        ) {
            if (item.targetTitle != null) {
                SourceMigrationEntryListItem(
                    title = item.targetTitle,
                    thumbnailUrl = item.targetThumbnailUrl,
                    onClick = onTargetClick?.let { onClick ->
                        { onClick(item.sourceEntryId) }
                    },
                    supportingContent = {
                        Text(
                            text = mapping.targetSourceName.orEmpty(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    trailingContent = item.selectedTargetEntryId?.let { targetEntryId ->
                        {
                            SourceMigrationEntryDetailsButton {
                                onTargetDetailsClick(targetEntryId)
                            }
                        }
                    },
                )
            } else {
                ListItem(
                    modifier = onTargetClick?.let { onClick ->
                        Modifier.clickable { onClick(item.sourceEntryId) }
                    } ?: Modifier,
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Outlined.AddCircleOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                ) {
                    Text(
                        text = stringResource(MR.strings.sourceMigrationReview_chooseReplacement),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun SourceMigrationStateBadge(state: SourceMigrationItemState) {
    val label = when (state) {
        SourceMigrationItemState.DISCOVERY_QUEUED,
        SourceMigrationItemState.DISCOVERING,
        -> stringResource(MR.strings.sourceMigrationReview_searching)
        SourceMigrationItemState.READY -> stringResource(MR.strings.sourceMigrationReview_ready)
        SourceMigrationItemState.NEEDS_REVIEW,
        SourceMigrationItemState.CONFLICT,
        -> stringResource(MR.strings.sourceMigrationReview_needsReview)
        SourceMigrationItemState.NO_MATCH -> stringResource(MR.strings.sourceMigrationReview_noMatchStatus)
        SourceMigrationItemState.DISCOVERY_FAILED,
        SourceMigrationItemState.EXECUTION_FAILED,
        SourceMigrationItemState.CANCELLED,
        -> stringResource(MR.strings.sourceMigrationReview_failed)
        SourceMigrationItemState.EXECUTION_QUEUED,
        SourceMigrationItemState.EXECUTING,
        -> stringResource(MR.strings.sourceMigrationReview_migrating)
        SourceMigrationItemState.APPLIED -> stringResource(MR.strings.sourceMigrationReview_migrated)
        SourceMigrationItemState.APPLIED_INCOMPLETE -> stringResource(MR.strings.sourceMigrationReview_incomplete)
    }
    val containerColor = when (state) {
        SourceMigrationItemState.READY,
        SourceMigrationItemState.APPLIED,
        -> MaterialTheme.colorScheme.primary
        SourceMigrationItemState.NO_MATCH,
        SourceMigrationItemState.DISCOVERY_FAILED,
        SourceMigrationItemState.CONFLICT,
        SourceMigrationItemState.EXECUTION_FAILED,
        SourceMigrationItemState.APPLIED_INCOMPLETE,
        -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.secondary
    }
    BadgeGroup {
        Badge(
            text = label,
            color = containerColor,
            textColor = contentColorFor(containerColor),
        )
    }
}

@Composable
private fun SourceMigrationNotSelectedMember(
    member: SourceMigrationReviewMember,
    onDetailsClick: (entryId: Long) -> Unit,
) {
    SourceMigrationEntryListItem(
        title = member.member.title,
        thumbnailUrl = member.member.thumbnailUrl,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(0.6f),
        supportingContent = {
            Text(
                text = member.sourceName,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailingContent = {
            SourceMigrationEntryDetailsButton {
                onDetailsClick(member.member.entryId)
            }
        },
    )
}
