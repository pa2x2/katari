package eu.kanade.presentation.entry.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.request.ImageRequest
import coil3.request.crossfade
import eu.kanade.presentation.components.AdaptiveSheet
import eu.kanade.presentation.components.TabbedDialogPaddings
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.presentation.more.settings.LocalPreferenceMinHeight
import eu.kanade.presentation.more.settings.widget.TextPreferenceWidget
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.sourceNotInstalledName
import tachiyomi.domain.entry.model.DuplicateEntryCandidate
import tachiyomi.domain.entry.model.DuplicateMatchReason
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryStatus
import tachiyomi.domain.library.service.DuplicatePreferences
import tachiyomi.domain.source.model.SourceDisplayInfo
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.Badge
import tachiyomi.presentation.core.components.BadgeGroup
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import tachiyomi.presentation.core.util.secondaryItemAlpha
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun DuplicateEntryDialog(
    duplicates: List<DuplicateEntryCandidate>,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    onOpenEntry: (entry: Entry) -> Unit,
    onMigrate: (entry: Entry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sourceManager = remember { Injekt.get<SourceManager>() }
    val minHeight = LocalPreferenceMinHeight.current
    val horizontalPadding = PaddingValues(horizontal = TabbedDialogPaddings.Horizontal)
    val horizontalPaddingModifier = Modifier.padding(horizontalPadding)

    AdaptiveSheet(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = TabbedDialogPaddings.Vertical)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium),
        ) {
            Text(
                text = stringResource(MR.strings.possible_duplicates_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .then(horizontalPaddingModifier)
                    .padding(top = MaterialTheme.padding.small),
            )

            Text(
                text = stringResource(MR.strings.possible_duplicates_summary),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.then(horizontalPaddingModifier),
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .heightIn(max = 520.dp),
                contentPadding = horizontalPadding,
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
            ) {
                items(
                    items = duplicates,
                    key = { it.entry.id },
                ) {
                    DuplicateEntryListItem(
                        duplicate = it,
                        getSourceInfo = { sourceManager.getDisplayInfo(it.entry.source) },
                        onMigrate = { onMigrate(it.entry) },
                        onDismissRequest = onDismissRequest,
                        onOpenEntry = { onOpenEntry(it.entry) },
                    )
                }
            }

            Column(modifier = horizontalPaddingModifier) {
                HorizontalDivider()

                TextPreferenceWidget(
                    title = stringResource(MR.strings.action_add_anyway),
                    icon = Icons.Outlined.Add,
                    onPreferenceClick = {
                        onDismissRequest()
                        onConfirm()
                    },
                    modifier = Modifier.padding(top = MaterialTheme.padding.small).clip(CircleShape),
                )
            }

            OutlinedButton(
                onClick = onDismissRequest,
                modifier = Modifier
                    .then(horizontalPaddingModifier)
                    .padding(bottom = MaterialTheme.padding.medium)
                    .heightIn(min = minHeight)
                    .fillMaxWidth(),
            ) {
                Text(
                    modifier = Modifier.padding(vertical = MaterialTheme.padding.extraSmall),
                    text = stringResource(MR.strings.action_cancel),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun DuplicateEntryListItem(
    duplicate: DuplicateEntryCandidate,
    getSourceInfo: () -> SourceDisplayInfo,
    onDismissRequest: () -> Unit,
    onOpenEntry: () -> Unit,
    onMigrate: () -> Unit,
) {
    val sourceInfo = getSourceInfo()
    val entry = duplicate.entry
    val duplicatePreferences = remember { Injekt.get<DuplicatePreferences>() }
    val extendedEnabled by duplicatePreferences.extendedDuplicateDetectionEnabled.collectAsState()

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(
            width = 1.dp,
            color = if (duplicate.isStrongMatch) {
                MaterialTheme.colorScheme.error.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.padding.small),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
            verticalAlignment = Alignment.Top,
        ) {
            Box(modifier = Modifier.width(DuplicateCoverWidth)) {
                EntryCover.Book(
                    data = ImageRequest.Builder(LocalContext.current)
                        .data(entry)
                        .crossfade(true)
                        .build(),
                    modifier = Modifier.fillMaxWidth(),
                )
                BadgeGroup(
                    modifier = Modifier
                        .padding(4.dp)
                        .align(Alignment.TopStart),
                ) {
                    Badge(
                        color = MaterialTheme.colorScheme.secondary,
                        textColor = MaterialTheme.colorScheme.onSecondary,
                        text = duplicate.count.countLabel(entry.type),
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
            ) {
                Text(
                    text = entry.displayTitle,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                if (!entry.displayName.isNullOrBlank() && entry.displayName != entry.title) {
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                DuplicateMetaRow(
                    text = if (sourceInfo.isMissing) {
                        stringResource(MR.strings.source_not_installed, sourceInfo.sourceNotInstalledName())
                    } else {
                        sourceInfo.name
                    },
                    iconImageVector = Icons.Outlined.Public,
                    iconTint = if (sourceInfo.isMissing) MaterialTheme.colorScheme.error else LocalContentColor.current,
                    maxLines = 2,
                )

                DuplicateMetaRow(
                    text = entry.statusLabel(),
                    iconImageVector = entry.statusIcon(),
                )

                val author = entry.author
                if (!author.isNullOrBlank()) {
                    DuplicateMetaRow(
                        text = author,
                        iconImageVector = Icons.Filled.PersonOutline,
                        maxLines = 2,
                    )
                }

                val artist = entry.artist
                if (!artist.isNullOrBlank() && author != artist) {
                    DuplicateMetaRow(
                        text = artist,
                        iconImageVector = Icons.Filled.Brush,
                        maxLines = 2,
                    )
                }

                entry.description
                    ?.normalizedPreview()
                    ?.takeIf { it.isNotBlank() }
                    ?.let {
                        DuplicateMetaRow(
                            text = it,
                            iconImageVector = Icons.Outlined.Description,
                            maxLines = 3,
                        )
                    }

                if (extendedEnabled) {
                    FlowRow(
                        modifier = Modifier.padding(top = MaterialTheme.padding.extraSmall),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
                    ) {
                        Badge(
                            text = stringResource(MR.strings.possible_duplicates_score, duplicate.scorePercent),
                            color = duplicate.scoreBadgeColor(),
                            textColor = duplicate.scoreBadgeTextColor(),
                        )
                        duplicate.reasons.forEach { reason ->
                            Badge(
                                text = reason.label(entry.type),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                textColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = MaterialTheme.padding.extraSmall),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                ) {
                    FilledTonalButton(
                        onClick = {
                            onDismissRequest()
                            onOpenEntry()
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = stringResource(MR.strings.action_open),
                            modifier = Modifier.padding(start = MaterialTheme.padding.extraSmall),
                        )
                    }

                    Button(
                        onClick = {
                            onDismissRequest()
                            onMigrate()
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = stringResource(MR.strings.action_migrate),
                            modifier = Modifier.padding(start = MaterialTheme.padding.extraSmall),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DuplicateMetaRow(
    text: String,
    iconImageVector: ImageVector,
    maxLines: Int = 1,
    iconTint: Color = Color.Unspecified,
) {
    Row(
        modifier = Modifier
            .secondaryItemAlpha()
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = iconImageVector,
            contentDescription = null,
            modifier = Modifier.size(EntryDetailsIconWidth),
            tint = if (iconTint == Color.Unspecified) LocalContentColor.current else iconTint,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            overflow = TextOverflow.Ellipsis,
            maxLines = maxLines,
        )
    }
}

@Composable
private fun Long.countLabel(entryType: EntryType): String {
    return pluralStringResource(entryType.entryTypePresentation().childCountPlural, toInt(), this)
}

@Composable
private fun DuplicateMatchReason.label(entryType: EntryType): String {
    return when (this) {
        DuplicateMatchReason.DESCRIPTION -> stringResource(MR.strings.possible_duplicates_reason_description)
        DuplicateMatchReason.TITLE -> stringResource(MR.strings.possible_duplicates_reason_title)
        DuplicateMatchReason.TRACKER -> stringResource(MR.strings.possible_duplicates_reason_tracker)
        DuplicateMatchReason.AUTHOR -> stringResource(MR.strings.possible_duplicates_reason_author)
        DuplicateMatchReason.ARTIST -> stringResource(MR.strings.possible_duplicates_reason_artist)
        DuplicateMatchReason.COVER -> stringResource(MR.strings.possible_duplicates_reason_cover)
        DuplicateMatchReason.STATUS -> stringResource(MR.strings.possible_duplicates_reason_status)
        DuplicateMatchReason.GENRE -> stringResource(MR.strings.possible_duplicates_reason_genre)
        DuplicateMatchReason.CHAPTER_COUNT -> stringResource(entryType.entryTypePresentation().childCountReasonLabel)
    }
}

@Composable
private fun Entry.statusLabel(): String {
    return when (status) {
        EntryStatus.ONGOING -> stringResource(MR.strings.ongoing)
        EntryStatus.COMPLETED -> stringResource(MR.strings.completed)
        EntryStatus.LICENSED -> stringResource(MR.strings.licensed)
        EntryStatus.PUBLISHING_FINISHED -> stringResource(MR.strings.publishing_finished)
        EntryStatus.CANCELLED -> stringResource(MR.strings.cancelled)
        EntryStatus.ON_HIATUS -> stringResource(MR.strings.on_hiatus)
        else -> stringResource(MR.strings.unknown)
    }
}

private fun Entry.statusIcon(): ImageVector {
    return when (status) {
        EntryStatus.ONGOING -> Icons.Outlined.Schedule
        EntryStatus.COMPLETED -> Icons.Outlined.DoneAll
        EntryStatus.LICENSED -> Icons.Outlined.AttachMoney
        EntryStatus.PUBLISHING_FINISHED -> Icons.Outlined.Done
        EntryStatus.CANCELLED -> Icons.Outlined.Close
        EntryStatus.ON_HIATUS -> Icons.Outlined.Pause
        else -> Icons.Outlined.Block
    }
}

@Composable
private fun DuplicateEntryCandidate.scoreBadgeColor(): Color {
    return if (isStrongMatch) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.tertiary
    }
}

@Composable
private fun DuplicateEntryCandidate.scoreBadgeTextColor(): Color {
    return if (isStrongMatch) {
        MaterialTheme.colorScheme.onError
    } else {
        MaterialTheme.colorScheme.onTertiary
    }
}

private fun String.normalizedPreview(): String {
    return replace(Regex("<[^>]+>"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

private val DuplicateCoverWidth = 96.dp
private val EntryDetailsIconWidth = 16.dp
