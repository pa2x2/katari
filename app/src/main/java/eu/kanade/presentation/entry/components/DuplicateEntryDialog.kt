package eu.kanade.presentation.entry.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import coil3.request.ImageRequest
import coil3.request.crossfade
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.sourceNotInstalledName
import mihon.app.di.appGraph
import mihon.entry.interactions.migration.EntryMigrationAvailability
import mihon.entry.interactions.migration.EntryMigrationFeature
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Add
import mihon.icons.materialsymbols.rounded.Brush
import mihon.icons.materialsymbols.rounded.Description
import mihon.icons.materialsymbols.rounded.Person
import mihon.icons.materialsymbols.rounded.Public
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.content.Badge
import mihon.presentation.kit.content.BadgeGroup
import mihon.presentation.kit.content.BadgeTone
import mihon.presentation.kit.content.EntryMetrics
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.list.RowGroup
import mihon.presentation.kit.list.RowGroupGap
import mihon.presentation.kit.list.RowGroupNote
import mihon.presentation.kit.list.SettingsRow
import mihon.presentation.kit.sheet.Sheet
import mihon.presentation.kit.sheet.SheetHeader
import mihon.presentation.kit.surface.Card
import mihon.presentation.kit.surface.CardStyle
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.domain.entry.model.DuplicateEntryCandidate
import tachiyomi.domain.entry.model.DuplicateMatchReason
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.source.model.SourceDisplayInfo
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

@Composable
fun DuplicateEntryDialog(
    duplicates: List<DuplicateEntryCandidate>,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    onOpenEntry: (entry: Entry) -> Unit,
    onMigrate: (entry: Entry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val sourceManager = remember { context.appGraph.sourceManager }
    val migration = remember { context.appGraph.entryMigrationFeature }
    // The duplicates are cards of their own, at the edges of the row group cards around them.
    val horizontalPadding = PaddingValues(horizontal = KatariTheme.spacing.m)

    Sheet(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        capHeight = true,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SheetHeader(title = stringResource(MR.strings.possible_duplicates_title))

            RowGroupNote(stringResource(MR.strings.possible_duplicates_summary))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                contentPadding = horizontalPadding,
                verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
            ) {
                items(
                    items = duplicates,
                    key = { it.entry.id },
                ) {
                    DuplicateEntryListItem(
                        duplicate = it,
                        getSourceInfo = { sourceManager.getDisplayInfo(it.entry.source) },
                        onMigrate = if (migration.supportsDuplicateMigration(it.entry)) {
                            { onMigrate(it.entry) }
                        } else {
                            null
                        },
                        onDismissRequest = onDismissRequest,
                        onOpenEntry = { onOpenEntry(it.entry) },
                    )
                }
            }

            RowGroupGap()
            RowGroup {
                row {
                    SettingsRow(
                        title = stringResource(MR.strings.action_add_anyway),
                        icon = MaterialSymbols.Rounded.Add,
                        onClick = {
                            onDismissRequest()
                            onConfirm()
                        },
                    )
                }
            }

            Button(
                label = stringResource(MR.strings.action_cancel),
                onClick = onDismissRequest,
                modifier = Modifier
                    .padding(KatariTheme.spacing.m)
                    .fillMaxWidth(),
                style = ButtonStyle.Outlined,
            )
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
    onMigrate: (() -> Unit)?,
) {
    val context = LocalContext.current
    val sourceInfo = getSourceInfo()
    val entry = duplicate.entry
    val statusPresentation = entry.status.presentation()
    val duplicatePreferences = remember { context.appGraph.duplicatePreferences }
    val extendedEnabled by duplicatePreferences.extendedDuplicateDetectionEnabled.collectAsState()

    // A strong match is filled with the error container, so it stands out even without its score badge.
    Card(
        modifier = Modifier.fillMaxWidth(),
        style = CardStyle.Outlined,
        container = if (duplicate.isStrongMatch) SurfaceContainer.ErrorContainer else null,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(KatariTheme.spacing.s),
            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
            verticalAlignment = Alignment.Top,
        ) {
            Box(modifier = Modifier.width(EntryMetrics.duplicateCoverWidth)) {
                EntryCover.Book(
                    data = ImageRequest.Builder(LocalContext.current)
                        .data(entry)
                        .crossfade(true)
                        .build(),
                    modifier = Modifier.fillMaxWidth(),
                )
                BadgeGroup(
                    modifier = Modifier
                        .padding(KatariTheme.spacing.xs)
                        .align(Alignment.TopStart),
                ) {
                    Badge(
                        text = duplicate.count.countLabel(entry.type),
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
            ) {
                Text(
                    text = entry.displayTitle,
                    role = TextRole.SectionTitle,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                if (!entry.displayName.isNullOrBlank() && entry.displayName != entry.title) {
                    Text(
                        text = entry.title,
                        role = TextRole.Supporting,
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
                    iconImageVector = MaterialSymbols.Rounded.Public,
                    iconTone = if (sourceInfo.isMissing) ContentTone.Error else ContentTone.Secondary,
                    maxLines = 2,
                )

                DuplicateMetaRow(
                    text = statusPresentation.label,
                    iconImageVector = statusPresentation.icon,
                )

                val author = entry.author
                if (!author.isNullOrBlank()) {
                    DuplicateMetaRow(
                        text = author,
                        iconImageVector = MaterialSymbols.Rounded.Person,
                        maxLines = 2,
                    )
                }

                val artist = entry.artist
                if (!artist.isNullOrBlank() && author != artist) {
                    DuplicateMetaRow(
                        text = artist,
                        iconImageVector = MaterialSymbols.Rounded.Brush,
                        maxLines = 2,
                    )
                }

                entry.description
                    ?.normalizedPreview()
                    ?.takeIf { it.isNotBlank() }
                    ?.let {
                        DuplicateMetaRow(
                            text = it,
                            iconImageVector = MaterialSymbols.Rounded.Description,
                            maxLines = 3,
                        )
                    }

                if (extendedEnabled) {
                    FlowRow(
                        modifier = Modifier.padding(top = KatariTheme.spacing.xs),
                        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
                        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
                    ) {
                        Badge(
                            text = stringResource(MR.strings.possible_duplicates_score, duplicate.scorePercent),
                            tone = duplicate.scoreBadgeTone,
                        )
                        duplicate.reasons.forEach { reason ->
                            Badge(
                                text = reason.label(entry.type),
                                tone = BadgeTone.Muted,
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = KatariTheme.spacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
                ) {
                    Button(
                        label = stringResource(MR.strings.action_open),
                        onClick = {
                            onDismissRequest()
                            onOpenEntry()
                        },
                        modifier = Modifier.weight(1f),
                        style = ButtonStyle.Tonal,
                        icon = EntryActionIcons.openFullEntry,
                    )

                    if (onMigrate != null) {
                        Button(
                            label = stringResource(MR.strings.action_migrate),
                            onClick = {
                                onDismissRequest()
                                onMigrate()
                            },
                            modifier = Modifier.weight(1f),
                            icon = EntryActionIcons.migrate,
                        )
                    }
                }
            }
        }
    }
}

internal fun EntryMigrationFeature.supportsDuplicateMigration(entry: Entry): Boolean {
    return availability(entry) is EntryMigrationAvailability.Available
}

@Composable
private fun DuplicateMetaRow(
    text: String,
    iconImageVector: ImageVector,
    maxLines: Int = 1,
    iconTone: ContentTone = ContentTone.Secondary,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = iconImageVector,
            contentDescription = null,
            modifier = Modifier.size(KatariTheme.sizes.iconSmall),
            tone = iconTone,
        )
        Text(
            text = text,
            role = TextRole.Supporting,
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

private val DuplicateEntryCandidate.scoreBadgeTone: BadgeTone
    get() = if (isStrongMatch) BadgeTone.Error else BadgeTone.Tertiary

private fun String.normalizedPreview(): String {
    return replace(Regex("<[^>]+>"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}
