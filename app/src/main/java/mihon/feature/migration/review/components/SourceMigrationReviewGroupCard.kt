package mihon.feature.migration.review.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.entry.components.EntryCover
import mihon.feature.migration.review.SourceMigrationReviewGroup
import mihon.feature.migration.review.SourceMigrationReviewMapping
import mihon.feature.migration.review.SourceMigrationReviewMember
import mihon.feature.migration.session.model.SourceMigrationItemState
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun SourceMigrationReviewGroupCard(
    group: SourceMigrationReviewGroup,
    onIncludedChange: (sourceEntryId: Long, included: Boolean) -> Unit,
    onToggleGroup: (groupId: Long) -> Unit,
    onTargetClick: ((sourceEntryId: Long) -> Unit)? = null,
    modifier: Modifier = Modifier,
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
                    SourceMigrationNotSelectedMember(member)
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = MaterialTheme.padding.medium, end = MaterialTheme.padding.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
        )
        Spacer(modifier = Modifier.weight(1f))
        action?.let {
            TextButton(onClick = onActionClick) {
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
) {
    val item = mapping.item
    Column(
        modifier = Modifier.padding(MaterialTheme.padding.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SourceMigrationEntry(
                title = item.sourceTitle,
                sourceName = mapping.sourceName,
                thumbnailUrl = item.sourceThumbnailUrl,
                modifier = Modifier.weight(1f),
            )
            SourceMigrationStateLabel(item.state)
            if (item.state == SourceMigrationItemState.READY) {
                Checkbox(
                    checked = item.included,
                    onCheckedChange = { included ->
                        onIncludedChange(item.sourceEntryId, included)
                    },
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(start = 18.dp)
                .size(20.dp),
        )

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = onTargetClick != null) {
                    onTargetClick?.invoke(item.sourceEntryId)
                },
        ) {
            if (item.targetTitle != null) {
                SourceMigrationEntry(
                    title = item.targetTitle,
                    sourceName = mapping.targetSourceName.orEmpty(),
                    thumbnailUrl = item.targetThumbnailUrl,
                    modifier = Modifier.padding(MaterialTheme.padding.small),
                )
            } else {
                Row(
                    modifier = Modifier.padding(MaterialTheme.padding.medium),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AddCircleOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(MR.strings.sourceMigrationReview_chooseReplacement),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun SourceMigrationEntry(
    title: String,
    sourceName: String,
    thumbnailUrl: String?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EntryCover.Book(
            data = thumbnailUrl,
            modifier = Modifier.width(44.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = sourceName,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun SourceMigrationStateLabel(state: SourceMigrationItemState) {
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
    Text(
        text = label,
        color = when (state) {
            SourceMigrationItemState.READY,
            SourceMigrationItemState.APPLIED,
            -> MaterialTheme.colorScheme.primary
            SourceMigrationItemState.NO_MATCH,
            SourceMigrationItemState.DISCOVERY_FAILED,
            SourceMigrationItemState.CONFLICT,
            SourceMigrationItemState.EXECUTION_FAILED,
            SourceMigrationItemState.APPLIED_INCOMPLETE,
            -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        style = MaterialTheme.typography.labelMedium,
    )
}

@Composable
private fun SourceMigrationNotSelectedMember(member: SourceMigrationReviewMember) {
    SourceMigrationEntry(
        title = member.member.title,
        sourceName = member.sourceName,
        thumbnailUrl = member.member.thumbnailUrl,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(0.6f)
            .padding(MaterialTheme.padding.medium),
    )
}
