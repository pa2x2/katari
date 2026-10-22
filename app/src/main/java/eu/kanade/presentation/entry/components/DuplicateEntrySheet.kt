package eu.kanade.presentation.entry.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonSize
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.content.Badge
import mihon.presentation.kit.content.BadgeTone
import mihon.presentation.kit.content.CoverFrame
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.content.TonedSpan
import mihon.presentation.kit.content.TonedText
import mihon.presentation.kit.dialog.DialogAction
import mihon.presentation.kit.list.CoverRow
import mihon.presentation.kit.list.RowGroupItem
import mihon.presentation.kit.list.RowGroupNote
import mihon.presentation.kit.list.RowGroupPosition
import mihon.presentation.kit.sheet.Sheet
import mihon.presentation.kit.sheet.SheetActions
import mihon.presentation.kit.sheet.SheetHeader
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

/**
 * Warns that entries already in the library look like the one being added: each as a cover row with its source,
 * status and author, a pill on a strong match, and Open and Migrate. Add anyway is the sheet's main action.
 */
@Composable
fun DuplicateEntrySheet(
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
    val duplicatePreferences = remember { context.appGraph.duplicatePreferences }
    val extendedEnabled by duplicatePreferences.extendedDuplicateDetectionEnabled.collectAsState()

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
            ) {
                itemsIndexed(
                    items = duplicates,
                    key = { _, duplicate -> duplicate.entry.id },
                ) { index, duplicate ->
                    RowGroupItem(position = RowGroupPosition.of(index, duplicates.size)) {
                        DuplicateEntryRow(
                            duplicate = duplicate,
                            sourceInfo = sourceManager.getDisplayInfo(duplicate.entry.source),
                            showScore = extendedEnabled,
                            onOpenEntry = {
                                onDismissRequest()
                                onOpenEntry(duplicate.entry)
                            },
                            onMigrate = if (migration.supportsDuplicateMigration(duplicate.entry)) {
                                {
                                    onDismissRequest()
                                    onMigrate(duplicate.entry)
                                }
                            } else {
                                null
                            },
                        )
                    }
                }
            }

            SheetActions(
                confirm = DialogAction(
                    label = stringResource(MR.strings.action_add_anyway),
                    onClick = {
                        onDismissRequest()
                        onConfirm()
                    },
                ),
                dismiss = DialogAction(stringResource(MR.strings.action_cancel), onDismissRequest),
            )
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun DuplicateEntryRow(
    duplicate: DuplicateEntryCandidate,
    sourceInfo: SourceDisplayInfo,
    showScore: Boolean,
    onOpenEntry: () -> Unit,
    onMigrate: (() -> Unit)?,
) {
    val entry = duplicate.entry
    CoverRow(
        cover = {
            EntryCover.Book(
                data = ImageRequest.Builder(LocalContext.current)
                    .data(entry)
                    .crossfade(true)
                    .build(),
                modifier = Modifier.fillMaxSize(),
                frame = CoverFrame.Thumbnail,
            )
        },
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = entry.displayTitle,
                modifier = Modifier.weight(1f, fill = false),
                role = TextRole.ItemTitle,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (duplicate.isStrongMatch) {
                Badge(text = stringResource(MR.strings.possible_duplicates_strong_match), tone = BadgeTone.Error)
            }
        }
        TonedText(
            spans = listOfNotNull(
                if (sourceInfo.isMissing) {
                    TonedSpan(
                        stringResource(MR.strings.source_not_installed, sourceInfo.sourceNotInstalledName()),
                        ContentTone.Error,
                    )
                } else {
                    TonedSpan(sourceInfo.name)
                },
                TonedSpan(entry.status.presentation().label),
                TonedSpan(duplicate.count.countLabel(entry.type)),
                entry.author?.takeIf { it.isNotBlank() }?.let(::TonedSpan),
            ),
            role = TextRole.Supporting,
            separator = " · ",
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (showScore) {
            FlowRow(
                modifier = Modifier.padding(top = KatariTheme.spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
                verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
            ) {
                Badge(
                    text = stringResource(MR.strings.possible_duplicates_score, duplicate.scorePercent),
                    tone = BadgeTone.Tertiary,
                )
                duplicate.reasons.forEach { reason ->
                    Badge(text = reason.label(entry.type), tone = BadgeTone.Muted)
                }
            }
        }
        Row(
            modifier = Modifier.padding(top = KatariTheme.spacing.s),
            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
        ) {
            Button(
                label = stringResource(MR.strings.action_open),
                onClick = onOpenEntry,
                style = ButtonStyle.Tonal,
                size = ButtonSize.Small,
            )
            if (onMigrate != null) {
                Button(
                    label = stringResource(MR.strings.action_migrate),
                    onClick = onMigrate,
                    size = ButtonSize.Small,
                )
            }
        }
    }
}

internal fun EntryMigrationFeature.supportsDuplicateMigration(entry: Entry): Boolean {
    return availability(entry) is EntryMigrationAvailability.Available
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
