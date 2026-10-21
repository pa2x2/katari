package mihon.feature.migration.review.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import eu.kanade.presentation.entry.components.EntryCover
import mihon.feature.migration.review.SourceMigrationReviewGroup
import mihon.feature.migration.review.SourceMigrationReviewMapping
import mihon.feature.migration.review.SourceMigrationReviewMember
import mihon.feature.migration.session.model.SourceMigrationItemState
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.ArrowDownward
import mihon.presentation.kit.content.Badge
import mihon.presentation.kit.content.BadgeGroup
import mihon.presentation.kit.content.BadgeTone
import mihon.presentation.kit.content.CoverFrame
import mihon.presentation.kit.content.IconBadge
import mihon.presentation.kit.content.Text
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
 * A merged entry during the search for replacements: its title over a note of what is replaced, then one card with
 * each member over its replacement and the members the migration leaves alone.
 */
@Composable
internal fun SourceMigrationDiscoveryMergedGroup(
    group: SourceMigrationReviewGroup,
    onSourceDetailsClick: (entryId: Long) -> Unit,
    onTargetDetailsClick: (entryId: Long) -> Unit,
    onTargetClick: (sourceEntryId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val searching = group.mappings.any { mapping -> mapping.item.state in SEARCHING_STATES }
    val noMatchCount = group.mappings.count { mapping ->
        mapping.item.targetTitle == null && mapping.item.state !in SEARCHING_STATES
    }
    Column(modifier) {
        RowGroupHeader(
            text = group.title,
            trailing = { Pill(text = stringResource(MR.strings.merge_members_count, group.memberCount)) },
            actions = { SourceMigrationGroupStatusPill(searching = searching, noMatchCount = noMatchCount) },
        )
        RowGroupNote(
            if (group.notSelectedMembers.isEmpty()) {
                stringResource(MR.strings.sourceMigrationReview_groupReplacements, group.mappings.size)
            } else {
                stringResource(
                    MR.strings.sourceMigrationReview_groupReplacementsNotSelected,
                    group.mappings.size,
                    group.notSelectedMembers.size,
                )
            },
        )
        RowGroup(
            rows = group.mappings.map { mapping ->
                val primary = mapping.item.sourceEntryId == group.visibleEntryId
                val sourceRole = if (primary) {
                    stringResource(MR.strings.sourceMigrationReview_primary)
                } else {
                    stringResource(MR.strings.label_member)
                }
                val targetRole = stringResource(MR.strings.sourceMigrationReview_newPrimary).takeIf { primary }
                val row: @Composable () -> Unit = {
                    SourceMigrationDiscoveryMapping(
                        mapping = mapping,
                        sourceRole = sourceRole,
                        targetRole = targetRole,
                        onSourceDetailsClick = onSourceDetailsClick,
                        onTargetDetailsClick = onTargetDetailsClick,
                        onTargetClick = onTargetClick,
                    )
                }
                row
            } + group.notSelectedMembers.map { member ->
                val row: @Composable () -> Unit = {
                    SourceMigrationDiscoveryNotSelectedMember(member, onSourceDetailsClick)
                }
                row
            },
        )
    }
}

/** A standalone entry, or one member of a merged entry, over its replacement or what stands in for one. */
@Composable
internal fun SourceMigrationDiscoveryMapping(
    mapping: SourceMigrationReviewMapping,
    sourceRole: String?,
    targetRole: String?,
    onSourceDetailsClick: (entryId: Long) -> Unit,
    onTargetDetailsClick: (entryId: Long) -> Unit,
    onTargetClick: (sourceEntryId: Long) -> Unit,
) {
    val item = mapping.item
    Column(
        modifier = Modifier.padding(vertical = KatariTheme.spacing.xs),
        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
    ) {
        SourceMigrationEntryListItem(
            title = item.sourceTitle,
            thumbnailUrl = item.sourceThumbnailUrl,
            inset = KatariTheme.spacing.l,
            supportingText = mapping.sourceName,
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    sourceRole?.let { role -> SourceMigrationRoleBadge(role) }
                    SourceMigrationEntryDetailsButton {
                        onSourceDetailsClick(item.sourceEntryId)
                    }
                }
            },
        )

        SourceMigrationReplacementConnector()

        when {
            item.targetTitle != null -> {
                SourceMigrationEntryListItem(
                    title = item.targetTitle,
                    thumbnailUrl = item.targetThumbnailUrl,
                    inset = KatariTheme.spacing.l,
                    onClick = { onTargetClick(item.sourceEntryId) },
                    supportingText = listOfNotNull(
                        mapping.targetSourceName,
                        item.matchKind?.let { matchKind -> sourceMigrationMatchKindLabel(matchKind) },
                    ).joinToString(separator = " · "),
                    trailingContent = item.selectedTargetEntryId?.let { targetEntryId ->
                        {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                targetRole?.let { role -> SourceMigrationRoleBadge(role) }
                                SourceMigrationEntryDetailsButton {
                                    onTargetDetailsClick(targetEntryId)
                                }
                            }
                        }
                    },
                )
            }
            item.state in SEARCHING_STATES -> SourceMigrationTargetPlaceholder(item.state)
            else -> SourceMigrationMissingTarget(onClick = { onTargetClick(item.sourceEntryId) })
        }
    }
}

@Composable
private fun SourceMigrationReplacementConnector() {
    Row(
        modifier = Modifier.padding(horizontal = KatariTheme.spacing.l),
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(MaterialSymbols.Rounded.ArrowDownward)
        Text(
            text = stringResource(MR.strings.sourceMigrationReview_willBeReplacedBy),
            role = TextRole.Caption,
            tone = ContentTone.Secondary,
        )
    }
}

@Composable
private fun SourceMigrationDiscoveryNotSelectedMember(
    member: SourceMigrationReviewMember,
    onDetailsClick: (entryId: Long) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = KatariTheme.spacing.l, vertical = KatariTheme.spacing.s),
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EntryCover.Book(
            data = member.member.thumbnailUrl,
            modifier = Modifier.width(KatariTheme.sizes.icon),
            frame = CoverFrame.Thumbnail,
        )
        Text(
            text = member.member.title,
            modifier = Modifier.weight(1f),
            role = TextRole.Body,
            tone = ContentTone.Secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        SourceMigrationRoleBadge(stringResource(MR.strings.label_member))
        SourceMigrationEntryDetailsButton {
            onDetailsClick(member.member.entryId)
        }
    }
}

/** What an entry is within its merged group, such as its primary or a member. */
@Composable
private fun SourceMigrationRoleBadge(label: String) {
    BadgeGroup {
        Badge(text = label, tone = BadgeTone.Neutral)
    }
}

private val SEARCHING_STATES = setOf(
    SourceMigrationItemState.DISCOVERY_QUEUED,
    SourceMigrationItemState.DISCOVERING,
)
