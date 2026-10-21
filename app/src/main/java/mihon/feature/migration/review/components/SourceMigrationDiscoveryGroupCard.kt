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
internal fun SourceMigrationDiscoveryGroupCard(
    group: SourceMigrationReviewGroup,
    onSourceDetailsClick: (entryId: Long) -> Unit,
    onTargetDetailsClick: (entryId: Long) -> Unit,
    onTargetClick: (sourceEntryId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth(), style = CardStyle.Elevated) {
        SourceMigrationDiscoveryGroupHeader(group)

        if (group.memberCount > 1) {
            SectionHeader(text = stringResource(MR.strings.sourceMigrationReview_included, group.mappings.size))
        }

        group.mappings.forEachIndexed { index, mapping ->
            if (index > 0) HorizontalDivider()
            SourceMigrationDiscoveryMapping(
                mapping = mapping,
                sourceRole = when {
                    group.memberCount == 1 -> stringResource(MR.strings.sourceMigrationReview_current)
                    mapping.item.sourceEntryId == group.visibleEntryId -> {
                        stringResource(MR.strings.sourceMigrationReview_primary)
                    }
                    else -> stringResource(MR.strings.label_member)
                },
                targetRole = if (
                    group.memberCount > 1 && mapping.item.sourceEntryId == group.visibleEntryId
                ) {
                    stringResource(MR.strings.sourceMigrationReview_newPrimary)
                } else {
                    null
                },
                onSourceDetailsClick = onSourceDetailsClick,
                onTargetDetailsClick = onTargetDetailsClick,
                onTargetClick = onTargetClick,
            )
        }

        if (group.notSelectedMembers.isNotEmpty()) {
            SectionHeader(
                text = stringResource(MR.strings.sourceMigrationReview_notSelected, group.notSelectedMembers.size),
            )
            group.notSelectedMembers.forEach { member ->
                SourceMigrationDiscoveryNotSelectedMember(member, onSourceDetailsClick)
            }
        }
    }
}

@Composable
private fun SourceMigrationDiscoveryGroupHeader(group: SourceMigrationReviewGroup) {
    val searching = group.mappings.any { mapping -> mapping.item.state in SEARCHING_STATES }
    val noMatchCount = group.mappings.count { mapping ->
        mapping.item.targetTitle == null && mapping.item.state !in SEARCHING_STATES
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = KatariTheme.spacing.rowInset,
                end = KatariTheme.spacing.l,
                top = KatariTheme.spacing.l,
                bottom = KatariTheme.spacing.s,
            ),
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
        ) {
            Text(
                text = if (group.memberCount == 1) {
                    stringResource(MR.strings.sourceMigrationReview_standaloneEntry)
                } else {
                    stringResource(MR.strings.sourceMigrationReview_mergedEntry, group.memberCount)
                },
                role = TextRole.Caption,
                tone = ContentTone.Secondary,
            )
            if (group.memberCount > 1) {
                Text(
                    text = group.title,
                    role = TextRole.SectionTitle,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (group.notSelectedMembers.isEmpty()) {
                        stringResource(MR.strings.sourceMigrationReview_groupReplacements, group.mappings.size)
                    } else {
                        stringResource(
                            MR.strings.sourceMigrationReview_groupReplacementsNotSelected,
                            group.mappings.size,
                            group.notSelectedMembers.size,
                        )
                    },
                    role = TextRole.Supporting,
                )
            }
        }
        SourceMigrationGroupStatusPill(searching = searching, noMatchCount = noMatchCount)
    }
}

@Composable
private fun SourceMigrationDiscoveryMapping(
    mapping: SourceMigrationReviewMapping,
    sourceRole: String,
    targetRole: String?,
    onSourceDetailsClick: (entryId: Long) -> Unit,
    onTargetDetailsClick: (entryId: Long) -> Unit,
    onTargetClick: (sourceEntryId: Long) -> Unit,
) {
    val item = mapping.item
    val targetModifier = Modifier.padding(horizontal = KatariTheme.spacing.l)
    Column(
        modifier = Modifier.padding(vertical = KatariTheme.spacing.xs),
        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
    ) {
        SourceMigrationEntryListItem(
            title = item.sourceTitle,
            thumbnailUrl = item.sourceThumbnailUrl,
            supportingText = mapping.sourceName,
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SourceMigrationRoleBadge(sourceRole)
                    SourceMigrationEntryDetailsButton {
                        onSourceDetailsClick(item.sourceEntryId)
                    }
                }
            },
        )

        SourceMigrationReplacementConnector()

        when {
            item.targetTitle != null -> {
                Card(modifier = targetModifier.fillMaxWidth(), container = SurfaceContainer.High) {
                    SourceMigrationEntryListItem(
                        title = item.targetTitle,
                        thumbnailUrl = item.targetThumbnailUrl,
                        inset = KatariTheme.spacing.s,
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
            }
            item.state in SEARCHING_STATES -> SourceMigrationTargetPlaceholder(item.state, targetModifier)
            else -> SourceMigrationMissingTarget(
                onClick = { onTargetClick(item.sourceEntryId) },
                modifier = targetModifier,
            )
        }
    }
}

@Composable
private fun SourceMigrationReplacementConnector() {
    Row(
        modifier = Modifier.padding(horizontal = KatariTheme.spacing.rowInset),
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
            .padding(
                start = KatariTheme.spacing.rowInset,
                end = KatariTheme.spacing.l,
                top = KatariTheme.spacing.s,
                bottom = KatariTheme.spacing.s,
            ),
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
