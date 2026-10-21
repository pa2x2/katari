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
import androidx.compose.ui.text.style.TextOverflow
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
import mihon.presentation.kit.list.SectionHeader
import mihon.presentation.kit.surface.Card
import mihon.presentation.kit.surface.CardStyle
import mihon.presentation.kit.surface.HorizontalDivider
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
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

    Card(modifier = modifier.fillMaxWidth(), style = CardStyle.Elevated) {
        if (group.memberCount > 1) {
            Column(
                modifier = Modifier.padding(
                    start = KatariTheme.spacing.rowInset,
                    end = KatariTheme.spacing.rowInset,
                    top = KatariTheme.spacing.m,
                ),
                verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
            ) {
                Text(
                    text = group.title,
                    role = TextRole.SectionTitle,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(
                        MR.strings.migrationEntriesScreen_selectedCount,
                        group.mappings.size,
                        group.memberCount,
                    ),
                    role = TextRole.Supporting,
                )
            }
        }

        if (replacements.isNotEmpty()) {
            SectionHeader(
                text = stringResource(MR.strings.sourceMigrationReview_replacements, replacements.size),
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
            SectionHeader(text = stringResource(MR.strings.sourceMigrationReview_noMatch, unresolved.size))
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
            SectionHeader(
                text = stringResource(MR.strings.sourceMigrationReview_notSelected, group.notSelectedMembers.size),
            )
            group.notSelectedMembers.forEachIndexed { index, member ->
                if (index > 0) HorizontalDivider()
                SourceMigrationNotSelectedMember(member, onSourceDetailsClick)
            }
        }
    }
}

/** One source entry over its replacement, or over the prompt to choose one when the search found none. */
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
        modifier = Modifier.padding(vertical = KatariTheme.spacing.s),
        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
    ) {
        SourceMigrationEntryListItem(
            title = item.sourceTitle,
            thumbnailUrl = item.sourceThumbnailUrl,
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
                .padding(start = KatariTheme.spacing.rowInset)
                .size(KatariTheme.sizes.iconSmall),
            tone = ContentTone.Secondary,
        )

        Card(
            modifier = Modifier
                .padding(horizontal = KatariTheme.spacing.l)
                .fillMaxWidth(),
            container = SurfaceContainer.High,
        ) {
            if (item.targetTitle != null) {
                SourceMigrationEntryListItem(
                    title = item.targetTitle,
                    thumbnailUrl = item.targetThumbnailUrl,
                    inset = KatariTheme.spacing.s,
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
                        .padding(horizontal = KatariTheme.spacing.s, vertical = KatariTheme.spacing.l),
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
}

@Composable
private fun SourceMigrationNotSelectedMember(
    member: SourceMigrationReviewMember,
    onDetailsClick: (entryId: Long) -> Unit,
) {
    SourceMigrationEntryListItem(
        title = member.member.title,
        thumbnailUrl = member.member.thumbnailUrl,
        dimmed = true,
        supportingText = member.sourceName,
        trailingContent = {
            SourceMigrationEntryDetailsButton {
                onDetailsClick(member.member.entryId)
            }
        },
    )
}
