package eu.kanade.tachiyomi.ui.browse.migration.entry.components

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.entry.components.EntryCover
import eu.kanade.presentation.entry.components.presentation
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import tachiyomi.domain.entry.model.Entry
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.selectedBackground

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
            .selectedBackground(isSelectable && resolvedSelectionState != ToggleableState.Off)
            .fillMaxWidth()
            .heightIn(min = 96.dp)
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        MigrateEntrySelectionCover(
            entry = entry,
            itemOrientation = itemOrientation,
            onInspect = onInspect,
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = entry.displayTitle,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelMedium,
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
    val coverHeight = when (itemOrientation) {
        EntryItemOrientation.VERTICAL -> 80.dp
        EntryItemOrientation.HORIZONTAL -> 40.dp
    }
    Box(
        modifier = Modifier
            .width(72.dp)
            .height(80.dp),
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
