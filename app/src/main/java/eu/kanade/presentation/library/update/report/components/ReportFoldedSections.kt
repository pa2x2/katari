package eu.kanade.presentation.library.update.report.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.library.update.labelRes
import mihon.feature.library.update.report.LibraryUpdateReport
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.KeyboardArrowDown
import mihon.icons.materialsymbols.rounded.KeyboardArrowUp
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
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
        ReportSectionHeader(
            icon = icon,
            title = title,
            count = items.size,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            trailing = { ExpandIcon(expanded) },
        )
        if (expanded) {
            FoldedItems(items = items, onClickEntry = onClickEntry)
        }
    }
}

/** A section of entries the run left out, folded into one group per reason. */
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

/** @param itemTrailing shown at the end of each entry's row once a group is unfolded. */
internal fun LazyListScope.reasonGroups(
    key: String,
    groups: Map<EntryUpdateDecisionReason, List<LibraryUpdateReport.Item>>,
    onClickEntry: (Entry) -> Unit,
    itemTrailing: (@Composable (LibraryUpdateReport.Item) -> Unit)? = null,
) {
    groups.forEach { (reason, items) ->
        item(key = "$key-${reason.name}") {
            CollapsibleGroup(
                title = stringResource(reason.labelRes),
                items = items,
                onClickEntry = onClickEntry,
                itemTrailing = itemTrailing,
            )
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = KatariTheme.spacing.contentInset, vertical = KatariTheme.spacing.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, role = TextRole.ItemTitle)
                Text(
                    text = pluralStringResource(MR.plurals.library_updates_entries, items.size, items.size),
                    role = TextRole.Supporting,
                )
            }
            ExpandIcon(expanded)
        }
        if (expanded) {
            FoldedItems(items = items, onClickEntry = onClickEntry, itemTrailing = itemTrailing)
        }
    }
}

@Composable
private fun FoldedItems(
    items: List<LibraryUpdateReport.Item>,
    onClickEntry: (Entry) -> Unit,
    itemTrailing: (@Composable (LibraryUpdateReport.Item) -> Unit)? = null,
) {
    items.forEach { item ->
        ReportRow(
            entry = item.entry,
            title = item.entry.title,
            subtitle = if (item.isRechecked) stringResource(MR.strings.library_update_report_rechecked) else null,
            onClick = { onClickEntry(item.entry) },
            trailing = itemTrailing?.let { { it(item) } },
        )
    }
}

@Composable
private fun ExpandIcon(expanded: Boolean) {
    Icon(
        imageVector = if (expanded) {
            MaterialSymbols.Rounded.KeyboardArrowUp
        } else {
            MaterialSymbols.Rounded.KeyboardArrowDown
        },
        contentDescription = null,
    )
}
