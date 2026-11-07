package eu.kanade.presentation.library.update.report.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.library.update.labelRes
import mihon.feature.library.update.report.LibraryUpdateReport
import mihon.presentation.kit.list.CollapsibleRowGroupHeader
import mihon.presentation.kit.list.CollapsibleRowGroupSubheader
import mihon.presentation.kit.list.RowGroup
import mihon.presentation.kit.list.RowGroupItem
import mihon.presentation.kit.list.RowGroupPosition
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.library.update.model.EntryUpdateDecisionReason
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/** A section whose entries need no action, so they stay folded under its header until asked for. */
@Composable
internal fun CollapsibleReportSection(
    icon: ImageVector,
    title: StringResource,
    items: List<LibraryUpdateReport.Item>,
    onClickEntry: (Entry) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column {
        CollapsibleRowGroupHeader(
            title = stringResource(title),
            summary = pluralStringResource(MR.plurals.library_updates_entries, items.size, items.size),
            expanded = expanded,
            onClick = { expanded = !expanded },
            icon = icon,
        )
        if (expanded) {
            RowGroup(rows = items.map { item -> { FoldedItem(item = item, onClickEntry = onClickEntry) } })
        }
    }
}

/** A section of entries the run left out, one card with a folded group per reason. */
internal fun LazyListScope.reasonSection(
    key: String,
    icon: ImageVector,
    title: StringResource,
    groups: Map<EntryUpdateDecisionReason, List<LibraryUpdateReport.Item>>,
    onClickEntry: (Entry) -> Unit,
) {
    if (groups.isEmpty()) return
    item(key = "header-$key") {
        ReportSectionHeader(icon = icon, title = title, count = groups.values.sumOf { it.size })
    }
    reasonGroups(key = key, groups = groups, onClickEntry = onClickEntry)
}

/**
 * Folded groups per reason as the [offset]th rows onwards of a card of [cardSize] rows, which may start with other
 * rows of the same section.
 *
 * @param itemTrailing shown at the end of each entry's row once a group is unfolded.
 */
internal fun LazyListScope.reasonGroups(
    key: String,
    groups: Map<EntryUpdateDecisionReason, List<LibraryUpdateReport.Item>>,
    onClickEntry: (Entry) -> Unit,
    offset: Int = 0,
    cardSize: Int = groups.size,
    itemTrailing: (@Composable (LibraryUpdateReport.Item) -> Unit)? = null,
) {
    groups.entries.forEachIndexed { index, (reason, items) ->
        item(key = "$key-${reason.name}") {
            RowGroupItem(position = RowGroupPosition.of(offset + index, cardSize)) {
                CollapsibleGroup(
                    title = stringResource(reason.labelRes),
                    items = items,
                    onClickEntry = onClickEntry,
                    itemTrailing = itemTrailing,
                )
            }
        }
    }
}

@Composable
private fun CollapsibleGroup(
    title: String,
    items: List<LibraryUpdateReport.Item>,
    onClickEntry: (Entry) -> Unit,
    itemTrailing: (@Composable (LibraryUpdateReport.Item) -> Unit)?,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column {
        CollapsibleRowGroupSubheader(
            title = title,
            summary = pluralStringResource(MR.plurals.library_updates_entries, items.size, items.size),
            expanded = expanded,
            onClick = { expanded = !expanded },
        )
        if (expanded) {
            items.forEach { item ->
                FoldedItem(item = item, onClickEntry = onClickEntry, itemTrailing = itemTrailing)
            }
        }
    }
}

@Composable
private fun FoldedItem(
    item: LibraryUpdateReport.Item,
    onClickEntry: (Entry) -> Unit,
    itemTrailing: (@Composable (LibraryUpdateReport.Item) -> Unit)? = null,
) {
    ReportRow(
        entry = item.entry,
        title = item.entry.title,
        subtitle = if (item.isRechecked) stringResource(MR.strings.library_update_report_rechecked) else null,
        onClick = { onClickEntry(item.entry) },
        trailing = itemTrailing?.let { { it(item) } },
    )
}
