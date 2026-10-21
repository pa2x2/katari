package mihon.feature.migration.review.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mihon.feature.migration.review.SourceMigrationReviewGroup
import mihon.feature.migration.review.SourceMigrationReviewMapping
import mihon.feature.migration.review.SourceMigrationReviewMember
import mihon.feature.migration.session.model.SourceMigrationItemState
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.AddCircle
import mihon.icons.materialsymbols.rounded.ArrowDownward
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.control.Checkbox
import mihon.presentation.kit.list.RowGroup
import mihon.presentation.kit.list.RowGroupHeader
import mihon.presentation.kit.list.RowGroupNote
import mihon.presentation.kit.surface.Pill
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * A merged entry once its replacements are found: its title with a way to include every ready member, a note of how
 * many members are migrated, then one card with the replacements, the members without a match and the members left
 * alone.
 */
@Composable
internal fun SourceMigrationReviewMergedGroup(
    group: SourceMigrationReviewGroup,
    onIncludedChange: (sourceEntryId: Long, included: Boolean) -> Unit,
    onToggleGroup: (groupId: Long) -> Unit,
    onTargetClick: ((sourceEntryId: Long) -> Unit)?,
    onSourceDetailsClick: (entryId: Long) -> Unit,
    onTargetDetailsClick: (entryId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val (replacements, unresolved) = group.mappings.partition { mapping -> mapping.item.targetTitle != null }

    Column(modifier) {
        RowGroupHeader(
            text = group.title,
            trailing = { Pill(text = stringResource(MR.strings.merge_members_count, group.memberCount)) },
            actions = if (group.readyMappings.isNotEmpty()) {
                {
                    Button(
                        label = if (group.allReadyMappingsIncluded) {
                            stringResource(MR.strings.migrationConfigScreen_selectNoneLabel)
                        } else {
                            stringResource(MR.strings.action_select_all)
                        },
                        onClick = { onToggleGroup(group.id) },
                        style = ButtonStyle.Text,
                    )
                }
            } else {
                null
            },
        )
        RowGroupNote(
            stringResource(MR.strings.migrationEntriesScreen_selectedCount, group.mappings.size, group.memberCount),
        )
        RowGroup(
            rows = (replacements + unresolved).map { mapping ->
                val row: @Composable () -> Unit = {
                    SourceMigrationReviewMapping(
                        mapping = mapping,
                        onIncludedChange = onIncludedChange,
                        onTargetClick = onTargetClick,
                        onSourceDetailsClick = onSourceDetailsClick,
                        onTargetDetailsClick = onTargetDetailsClick,
                    )
                }
                row
            } + group.notSelectedMembers.map { member ->
                val row: @Composable () -> Unit = { SourceMigrationNotSelectedMember(member, onSourceDetailsClick) }
                row
            },
        )
    }
}

/** One source entry over its replacement, or over the prompt to choose one when the search found none. */
@Composable
internal fun SourceMigrationReviewMapping(
    mapping: SourceMigrationReviewMapping,
    onIncludedChange: (sourceEntryId: Long, included: Boolean) -> Unit,
    onTargetClick: ((sourceEntryId: Long) -> Unit)?,
    onSourceDetailsClick: (entryId: Long) -> Unit,
    onTargetDetailsClick: (entryId: Long) -> Unit,
) {
    val item = mapping.item
    Column(
        modifier = Modifier.padding(vertical = KatariTheme.spacing.s),
        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
    ) {
        SourceMigrationEntryListItem(
            title = item.sourceTitle,
            thumbnailUrl = item.sourceThumbnailUrl,
            inset = KatariTheme.spacing.l,
            supportingText = mapping.sourceName,
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SourceMigrationItemStatusPill(item.state)
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
            imageVector = MaterialSymbols.Rounded.ArrowDownward,
            contentDescription = null,
            modifier = Modifier
                .padding(start = KatariTheme.spacing.l)
                .size(KatariTheme.sizes.iconSmall),
            tone = ContentTone.Secondary,
        )

        if (item.targetTitle != null) {
            SourceMigrationEntryListItem(
                title = item.targetTitle,
                thumbnailUrl = item.targetThumbnailUrl,
                inset = KatariTheme.spacing.l,
                onClick = onTargetClick?.let { onClick ->
                    { onClick(item.sourceEntryId) }
                },
                supportingText = mapping.targetSourceName.orEmpty(),
                trailingContent = item.selectedTargetEntryId?.let { targetEntryId ->
                    {
                        SourceMigrationEntryDetailsButton {
                            onTargetDetailsClick(targetEntryId)
                        }
                    }
                },
            )
        } else {
            Row(
                modifier = Modifier
                    .then(
                        onTargetClick?.let { onClick ->
                            Modifier.clickable { onClick(item.sourceEntryId) }
                        } ?: Modifier,
                    )
                    .fillMaxWidth()
                    .padding(horizontal = KatariTheme.spacing.l, vertical = KatariTheme.spacing.l),
                horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.l),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = MaterialSymbols.Rounded.AddCircle,
                    contentDescription = null,
                    tone = ContentTone.Primary,
                )
                Text(
                    text = stringResource(MR.strings.sourceMigrationReview_chooseReplacement),
                    role = TextRole.ItemTitle,
                    tone = ContentTone.Primary,
                )
            }
        }
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
        inset = KatariTheme.spacing.l,
        dimmed = true,
        supportingText = member.sourceName,
        trailingContent = {
            SourceMigrationEntryDetailsButton {
                onDetailsClick(member.member.entryId)
            }
        },
    )
}
