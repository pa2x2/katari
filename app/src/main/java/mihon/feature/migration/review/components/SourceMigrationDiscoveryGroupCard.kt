package mihon.feature.migration.review.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.entry.components.EntryCover
import mihon.feature.migration.review.SourceMigrationReviewGroup
import mihon.feature.migration.review.SourceMigrationReviewMapping
import mihon.feature.migration.review.SourceMigrationReviewMember
import mihon.feature.migration.session.model.SourceMigrationItemState
import mihon.feature.migration.session.model.SourceMigrationMatchKind
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.Badge
import tachiyomi.presentation.core.components.BadgeGroup
import tachiyomi.presentation.core.components.ListGroupHeader
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun SourceMigrationDiscoveryGroupCard(
    group: SourceMigrationReviewGroup,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column {
            SourceMigrationDiscoveryGroupHeader(group)

            if (group.memberCount > 1) {
                ListGroupHeader(
                    text = stringResource(MR.strings.sourceMigrationReview_included, group.mappings.size),
                )
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
                )
            }

            if (group.notSelectedMembers.isNotEmpty()) {
                ListGroupHeader(
                    text = stringResource(
                        MR.strings.sourceMigrationReview_notSelected,
                        group.notSelectedMembers.size,
                    ),
                )
                group.notSelectedMembers.forEach { member ->
                    SourceMigrationDiscoveryNotSelectedMember(member)
                }
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
            .padding(MaterialTheme.padding.medium),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
        ) {
            Text(
                text = if (group.memberCount == 1) {
                    stringResource(MR.strings.sourceMigrationReview_standaloneEntry)
                } else {
                    stringResource(MR.strings.sourceMigrationReview_mergedEntry, group.memberCount)
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
            )
            if (group.memberCount > 1) {
                Text(
                    text = group.title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = if (group.notSelectedMembers.isEmpty()) {
                        stringResource(
                            MR.strings.sourceMigrationReview_groupReplacements,
                            group.mappings.size,
                        )
                    } else {
                        stringResource(
                            MR.strings.sourceMigrationReview_groupReplacementsNotSelected,
                            group.mappings.size,
                            group.notSelectedMembers.size,
                        )
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        SourceMigrationDiscoveryStatusBadge(
            searching = searching,
            noMatchCount = noMatchCount,
        )
    }
}

@Composable
private fun SourceMigrationDiscoveryMapping(
    mapping: SourceMigrationReviewMapping,
    sourceRole: String,
    targetRole: String?,
) {
    val item = mapping.item
    Column(
        modifier = Modifier.padding(vertical = MaterialTheme.padding.extraSmall),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
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
            trailingContent = { SourceMigrationRoleBadge(sourceRole) },
        )

        SourceMigrationReplacementConnector()

        when {
            item.targetTitle != null -> {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .padding(horizontal = MaterialTheme.padding.small)
                        .fillMaxWidth(),
                ) {
                    SourceMigrationEntryListItem(
                        title = item.targetTitle,
                        thumbnailUrl = item.targetThumbnailUrl,
                        supportingContent = {
                            Text(
                                text = listOfNotNull(
                                    mapping.targetSourceName,
                                    item.matchKind?.let { matchKind -> matchKindLabel(matchKind) },
                                ).joinToString(separator = " · "),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        trailingContent = targetRole?.let { role ->
                            { SourceMigrationRoleBadge(role) }
                        },
                    )
                }
            }
            item.state in SEARCHING_STATES -> SourceMigrationTargetPlaceholder()
            else -> SourceMigrationMissingTarget()
        }
    }
}

@Composable
private fun SourceMigrationReplacementConnector() {
    Row(
        modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = CircleShape,
            modifier = Modifier.size(24.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.ArrowDownward,
                contentDescription = null,
                modifier = Modifier.padding(4.dp),
            )
        }
        Text(
            text = stringResource(MR.strings.sourceMigrationReview_willBeReplacedBy),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun SourceMigrationTargetPlaceholder() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .padding(horizontal = MaterialTheme.padding.small)
            .fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(MaterialTheme.padding.small),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SourceMigrationSkeleton(
                modifier = Modifier
                    .width(44.dp)
                    .aspectRatio(EntryCover.Book.ratio),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
            ) {
                SourceMigrationSkeleton(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(14.dp),
                )
                SourceMigrationSkeleton(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(12.dp),
                )
            }
        }
    }
}

@Composable
private fun SourceMigrationMissingTarget() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .padding(horizontal = MaterialTheme.padding.small)
            .fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(MaterialTheme.padding.medium),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .aspectRatio(EntryCover.Book.ratio)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            )
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall)) {
                Text(
                    text = stringResource(MR.strings.sourceMigrationReview_chooseReplacement),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(MR.strings.sourceMigrationReview_noAutomaticMatch),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun SourceMigrationDiscoveryNotSelectedMember(member: SourceMigrationReviewMember) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(0.7f)
            .padding(
                horizontal = MaterialTheme.padding.medium,
                vertical = MaterialTheme.padding.small,
            ),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EntryCover.Book(
            data = member.member.thumbnailUrl,
            modifier = Modifier.width(24.dp),
        )
        Text(
            text = member.member.title,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = stringResource(MR.strings.label_member),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun SourceMigrationDiscoveryStatusBadge(searching: Boolean, noMatchCount: Int) {
    val containerColor = when {
        searching -> MaterialTheme.colorScheme.secondary
        noMatchCount > 0 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    val label = when {
        searching -> stringResource(MR.strings.sourceMigrationReview_searching)
        noMatchCount > 0 -> stringResource(MR.strings.sourceMigrationReview_noMatch, noMatchCount)
        else -> stringResource(MR.strings.sourceMigrationReview_ready)
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
private fun SourceMigrationRoleBadge(label: String) {
    BadgeGroup {
        Badge(
            text = label,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            textColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SourceMigrationSkeleton(modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    )
}

@Composable
private fun matchKindLabel(matchKind: SourceMigrationMatchKind): String {
    return stringResource(
        when (matchKind) {
            SourceMigrationMatchKind.EXACT -> MR.strings.sourceMigrationReview_exactMatch
            SourceMigrationMatchKind.SIMILAR -> MR.strings.sourceMigrationReview_similarMatch
            SourceMigrationMatchKind.MANUAL -> MR.strings.sourceMigrationReview_manualMatch
        },
    )
}

private val SEARCHING_STATES = setOf(
    SourceMigrationItemState.DISCOVERY_QUEUED,
    SourceMigrationItemState.DISCOVERING,
)
