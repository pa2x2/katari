package eu.kanade.tachiyomi.ui.browse.migration.entry.components

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import app.katari.presentation.kit.content.CoverFrame
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.control.TriStateCheckbox
import app.katari.presentation.kit.list.CoverRow
import app.katari.presentation.kit.list.CoverRowSlot
import app.katari.presentation.kit.theme.TextRole
import eu.kanade.presentation.entry.components.presentation
import eu.kanade.presentation.entry.components.toGridCoverType
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
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
    CoverRow(
        cover = { MigrateEntrySelectionCover(entry = entry, itemOrientation = itemOrientation, onInspect = onInspect) },
        modifier = modifier.semantics {
            role = if (handlesExpansion) Role.Button else Role.Checkbox
            if (isSelectable && !handlesExpansion) toggleableState = resolvedSelectionState
        },
        selected = isSelectable && resolvedSelectionState != ToggleableState.Off,
        onClick = onToggleExpanded ?: onToggleSelection,
        onLongClick = onLongClick?.takeIf { isSelectable },
        coverSlot = CoverRowSlot.Wide,
        trailing = if (isSelectable) {
            {
                TriStateCheckbox(
                    state = resolvedSelectionState,
                    onClick = onToggleSelection,
                    modifier = if (handlesExpansion) Modifier else Modifier.clearAndSetSemantics {},
                )
            }
        } else {
            null
        },
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
}

/** The cover of a picker row, in the wide slot of a [CoverRow]; tapping it opens the entry. */
@Composable
internal fun MigrateEntrySelectionCover(
    entry: Entry,
    itemOrientation: EntryItemOrientation,
    onInspect: () -> Unit,
) {
    itemOrientation.toGridCoverType()(
        data = entry,
        modifier = when (itemOrientation) {
            EntryItemOrientation.VERTICAL -> Modifier.fillMaxHeight()
            EntryItemOrientation.HORIZONTAL -> Modifier.fillMaxWidth()
        },
        contentDescription = stringResource(MR.strings.migrationEntriesScreen_inspectEntry, entry.displayTitle),
        frame = CoverFrame.Thumbnail,
        onClick = onInspect,
    )
}
