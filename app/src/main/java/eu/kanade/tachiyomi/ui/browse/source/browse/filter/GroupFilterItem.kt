package eu.kanade.tachiyomi.ui.browse.source.browse.filter

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Badge
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.filter.EntryFilterGroupSummary
import eu.kanade.tachiyomi.source.entry.filter.validationIssues
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun GroupFilterItem(
    filter: EntryFilter.Group<*>,
    activeOnly: Boolean,
    onUpdate: () -> Unit,
    onReset: (EntryFilter<*>) -> Unit,
    item: @Composable (EntryFilter<*>, Boolean) -> Unit,
) {
    var expanded by rememberSaveable(filter) { mutableStateOf(false) }
    var selectedOnly by rememberSaveable(filter) { mutableStateOf(false) }
    val issues = filter.validationIssues()
    LaunchedEffect(issues.isNotEmpty()) { if (issues.isNotEmpty()) expanded = true }
    LaunchedEffect(activeOnly) { if (activeOnly) expanded = true }
    val count = filter.activeCount()
    val summary = (filter as? EntryFilterGroupSummary)?.selectionSummary(
        filter.state.filterIsInstance<EntryFilter<*>>(),
    )
    Column {
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        Row(
            Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    filter.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (issues.isEmpty()) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
                summary?.takeIf {
                    it.isNotBlank()
                }?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (count != null && count > 0) {
                Badge(
                    containerColor = if (issues.isEmpty()) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    },
                    contentColor = if (issues.isEmpty()) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    },
                ) { Text(count.toString()) }
            }
            Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = null)
        }
        if (expanded) {
            FlowRow(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(enabled = !activeOnly, selected = selectedOnly || activeOnly, onClick = {
                    selectedOnly = !selectedOnly
                }, label = { Text(stringResource(MR.strings.filter_selected)) })
                TextButton(onClick = {
                    onReset(filter)
                    onUpdate()
                }) { Text(stringResource(MR.strings.filter_reset_defaults)) }
                if (filter.canClear()) {
                    TextButton(onClick = {
                        filter.clearSelection()
                        onUpdate()
                    }) { Text(stringResource(MR.strings.filter_clear)) }
                }
            }
            SearchableFilterGroupContent(group = filter, selectedOnly = selectedOnly || activeOnly) {
                item(
                    it,
                    selectedOnly || activeOnly,
                )
            }
            issues.forEach {
                Text(
                    it.displayMessage(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }
    }
}
