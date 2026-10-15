package eu.kanade.tachiyomi.ui.browse.source.browse.filter.control

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import eu.kanade.tachiyomi.source.entry.EntryFilter
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.Sort
import mihon.icons.materialsymbols.rounded.ArrowDownward
import mihon.icons.materialsymbols.rounded.ArrowDropDown
import mihon.icons.materialsymbols.rounded.ArrowUpward
import mihon.icons.materialsymbols.rounded.Tune
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.control.SegmentedButtons
import mihon.presentation.kit.menu.DropdownMenu
import mihon.presentation.kit.menu.RadioMenuItem
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * A sort filter: the current option as a row value that opens the option menu, and an always visible direction
 * control, so neither the choice nor its direction is hidden or changed silently.
 */
@Composable
internal fun FilterSortRow(filter: EntryFilter.Sort, onUpdate: () -> Unit) {
    val selection = filter.state
    FilterMenuRow(
        icon = MaterialSymbols.AutoMirroredRounded.Sort,
        label = filter.name,
        value = selection?.let { filter.values.getOrNull(it.index) } ?: stringResource(MR.strings.filter_sort_unset),
        options = filter.values.toList(),
        selectedIndex = selection?.index,
        onSelect = { index ->
            filter.state = EntryFilter.Sort.Selection(index, selection?.ascending ?: true)
            onUpdate()
        },
    ) {
        FilterSortDirection(ascending = selection?.ascending, enabled = selection != null) { ascending ->
            filter.state = selection?.copy(ascending = ascending)
            onUpdate()
        }
    }
}

/** A single-choice ordering filter drawn in the same row style as [FilterSortRow]. */
@Composable
internal fun FilterOrderingSelectRow(filter: EntryFilter.Select<*>, onUpdate: () -> Unit) {
    FilterMenuRow(
        icon = MaterialSymbols.Rounded.Tune,
        label = filter.name,
        value = filter.values.getOrNull(filter.state)?.toString().orEmpty(),
        options = filter.values.map { it.toString() },
        selectedIndex = filter.state,
        onSelect = {
            filter.state = it
            onUpdate()
        },
    )
}

@Composable
private fun FilterMenuRow(
    icon: ImageVector,
    label: String,
    value: String,
    options: List<String>,
    selectedIndex: Int?,
    onSelect: (Int) -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    val spacing = KatariTheme.spacing
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = spacing.rowInset),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true }
                    .padding(start = spacing.rowInset, top = spacing.s, bottom = spacing.s, end = spacing.s),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.l),
            ) {
                Icon(icon, contentDescription = null, tone = ContentTone.Secondary)
                Column {
                    Text(text = label, role = TextRole.Supporting)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = value,
                            modifier = Modifier.weight(1f, fill = false),
                            role = TextRole.ItemTitle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Icon(MaterialSymbols.Rounded.ArrowDropDown, contentDescription = null)
                    }
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEachIndexed { index, option ->
                    RadioMenuItem(
                        label = option,
                        selected = index == selectedIndex,
                        onClick = {
                            expanded = false
                            onSelect(index)
                        },
                    )
                }
            }
        }
        trailing?.invoke()
    }
}

@Composable
private fun FilterSortDirection(ascending: Boolean?, enabled: Boolean, onChange: (Boolean) -> Unit) {
    // Null while the filter has no sort, so neither direction shows as selected.
    SegmentedButtons<Boolean?>(
        options = listOf(false, true),
        selected = ascending,
        onSelect = { it?.let(onChange) },
        label = { stringResource(if (it == true) MR.strings.filter_sort_asc else MR.strings.filter_sort_desc) },
        enabled = enabled,
        icon = { if (it == true) MaterialSymbols.Rounded.ArrowUpward else MaterialSymbols.Rounded.ArrowDownward },
        description = {
            stringResource(if (it == true) MR.strings.filter_sort_ascending else MR.strings.filter_sort_descending)
        },
    )
}
