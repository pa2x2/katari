package eu.kanade.tachiyomi.ui.browse.migration.entry.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import eu.kanade.presentation.entry.components.EntryCover
import eu.kanade.presentation.entry.components.presentation
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import mihon.presentation.kit.content.BrowseMetrics
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.control.TriStateCheckbox
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.domain.entry.model.Entry
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun MigrateEntrySelectionItem(
    entry: Entry,
    itemOrientation: EntryItemOrientation,
    consumedCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier,
    mergeRole: String? = null,
    isSelectable: Boolean = true,
    isSelected: Boolean,
    selectionState: ToggleableState? = null,
    onToggleSelection: () -> Unit,
    onLongClick: (() -> Unit)?,
    onInspect: () -> Unit,
    expanded: Boolean? = null,
    onToggleExpanded: (() -> Unit)? = null,
) {
    val handlesExpansion = expanded != null && onToggleExpanded != null
    val resolvedSelectionState = selectionState ?: if (isSelected) ToggleableState.On else ToggleableState.Off
    Row(
        modifier = modifier
            .then(
                if (isSelectable && resolvedSelectionState != ToggleableState.Off) {
                    Modifier.background(KatariTheme.colors.selected)
                } else {
                    Modifier
                },
            )
            .fillMaxWidth()
            .heightIn(min = BrowseMetrics.listCoverRowMinHeight)
            .then(
                if (isSelectable && !handlesExpansion) {
                    Modifier.semantics {
                        toggleableState = resolvedSelectionState
                    }
                } else {
                    Modifier
                },
            )
            .combinedClickable(
                role = if (handlesExpansion) Role.Button else Role.Checkbox,
                onClick = onToggleExpanded ?: onToggleSelection,
                onLongClick = onLongClick?.takeIf { isSelectable },
            )
            .padding(horizontal = KatariTheme.spacing.contentInset, vertical = KatariTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.l),
    ) {
        MigrateEntrySelectionCover(
            entry = entry,
            itemOrientation = itemOrientation,
            onInspect = onInspect,
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xxs),
        ) {
            Text(
                text = entry.displayTitle,
                role = TextRole.ItemTitle,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            val creator = listOfNotNull(entry.author, entry.artist)
                .map(String::trim)
                .filter(String::isNotEmpty)
                .distinct()
                .joinToString()
                .ifBlank { null }
            val sourceTitle = entry.title
                .takeIf { entry.displayName?.isNotBlank() == true && it != entry.displayTitle }
                ?.let { stringResource(MR.strings.migrationEntriesScreen_sourceTitle, it) }
            listOfNotNull(sourceTitle, creator)
                .joinToString(separator = " • ")
                .takeIf(String::isNotEmpty)
                ?.let { identity ->
                    Text(
                        text = identity,
                        role = TextRole.Supporting,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

            val typePresentation = entry.type.entryTypePresentation()
            val metadata = buildList {
                mergeRole?.let(::add)
                add(stringResource(typePresentation.displayNameLabel))
                add(entry.status.presentation().label)
                add(pluralStringResource(typePresentation.childCountPlural, totalCount, totalCount))
                if (totalCount > 0) {
                    add(stringResource(MR.strings.migrationEntriesScreen_progress, consumedCount, totalCount))
                }
            }.joinToString(separator = " • ")
            Text(
                text = metadata,
                role = TextRole.Supporting,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (isSelectable) {
            TriStateCheckbox(
                state = resolvedSelectionState,
                onClick = onToggleSelection,
                modifier = if (handlesExpansion) Modifier else Modifier.clearAndSetSemantics {},
            )
        }
    }
}

@Composable
internal fun MigrateEntrySelectionCover(
    entry: Entry,
    itemOrientation: EntryItemOrientation,
    onInspect: () -> Unit,
) {
    val coverType = when (itemOrientation) {
        EntryItemOrientation.VERTICAL -> EntryCover.Book
        EntryItemOrientation.HORIZONTAL -> EntryCover.Wide
    }
    val coverHeight = migrationCoverHeight(itemOrientation)
    Box(
        modifier = Modifier
            .width(BrowseMetrics.listCoverSlotWidth)
            .height(BrowseMetrics.listCoverHeight),
        contentAlignment = Alignment.Center,
    ) {
        coverType(
            data = entry,
            modifier = Modifier.height(coverHeight),
            contentDescription = stringResource(
                MR.strings.migrationEntriesScreen_inspectEntry,
                entry.displayTitle,
            ),
            onClick = onInspect,
        )
    }
}

/** Landscape covers keep the slot's width, so they are drawn at half the portrait height. */
internal fun migrationCoverHeight(itemOrientation: EntryItemOrientation): Dp = when (itemOrientation) {
    EntryItemOrientation.VERTICAL -> BrowseMetrics.listCoverHeight
    EntryItemOrientation.HORIZONTAL -> BrowseMetrics.listCoverHeight / 2
}
