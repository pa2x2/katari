package eu.kanade.presentation.library.update.report.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import app.katari.library.update.report.LibraryUpdateReport
import app.katari.presentation.kit.list.CollapsibleRowGroupSubheader
import app.katari.presentation.kit.list.RowGroupItem
import app.katari.presentation.kit.list.RowGroupPosition
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.library.update.labelRes
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.library.update.model.EntryUpdateDecisionReason
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/**
 * A section whose entries need no action, such as the ones with nothing new: its header like any other section's, and
 * one card whose entries stay folded until asked for.
 */
internal fun LazyListScope.foldedSection(
    key: String,
    icon: ImageVector,
    title: StringResource,
    items: List<LibraryUpdateReport.Item>,
    sourceNameOf: (Entry) -> String?,
    onClickEntry: (Entry) -> Unit,
) {
    if (items.isEmpty()) return
    item(key = "header-$key") {
        ReportSectionHeader(icon = icon, title = title, count = items.size)
    }
    item(key = key) {
        RowGroupItem(position = RowGroupPosition.Alone) {
            var expanded by rememberSaveable { mutableStateOf(false) }
            CollapsibleGroup(
                title = stringResource(
                    if (expanded) MR.strings.library_updates_show_fewer else MR.strings.library_updates_show_all,
                ),
                items = items,
                expanded = expanded,
                onToggle = { expanded = !expanded },
                sourceNameOf = sourceNameOf,
                onClickEntry = onClickEntry,
                itemTrailing = null,
            )
        }
    }
}

/** A section of entries the run left out, one card with a folded group per reason. */
internal fun LazyListScope.reasonSection(
    key: String,
    icon: ImageVector,
    title: StringResource,
    groups: Map<EntryUpdateDecisionReason, List<LibraryUpdateReport.Item>>,
    sourceNameOf: (Entry) -> String?,
    onClickEntry: (Entry) -> Unit,
) {
    if (groups.isEmpty()) return
    item(key = "header-$key") {
        ReportSectionHeader(icon = icon, title = title, count = groups.values.sumOf { it.size })
    }
    reasonGroups(key = key, groups = groups, sourceNameOf = sourceNameOf, onClickEntry = onClickEntry)
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
    sourceNameOf: (Entry) -> String?,
    onClickEntry: (Entry) -> Unit,
    offset: Int = 0,
    cardSize: Int = groups.size,
    itemTrailing: (@Composable (LibraryUpdateReport.Item) -> Unit)? = null,
) {
    groups.entries.forEachIndexed { index, (reason, items) ->
        item(key = "$key-${reason.name}") {
            RowGroupItem(position = RowGroupPosition.of(offset + index, cardSize)) {
                var expanded by rememberSaveable { mutableStateOf(false) }
                CollapsibleGroup(
                    title = stringResource(reason.labelRes),
                    items = items,
                    expanded = expanded,
                    onToggle = { expanded = !expanded },
                    sourceNameOf = sourceNameOf,
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
    expanded: Boolean,
    onToggle: () -> Unit,
    sourceNameOf: (Entry) -> String?,
    onClickEntry: (Entry) -> Unit,
    itemTrailing: (@Composable (LibraryUpdateReport.Item) -> Unit)?,
) {
    Column {
        CollapsibleRowGroupSubheader(
            title = title,
            summary = pluralStringResource(MR.plurals.library_updates_entries, items.size, items.size),
            expanded = expanded,
            onClick = onToggle,
        )
        if (expanded) {
            items.forEach { item ->
                ReportRow(
                    entry = item.entry,
                    title = item.entry.title,
                    subtitle = if (item.isRechecked) {
                        stringResource(
                            MR.strings.library_update_report_rechecked,
                        )
                    } else {
                        null
                    },
                    sourceName = sourceNameOf(item.entry),
                    onClick = { onClickEntry(item.entry) },
                    trailing = itemTrailing?.let { { it(item) } },
                )
            }
        }
    }
}
