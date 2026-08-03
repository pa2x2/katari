package eu.kanade.presentation.library.grouping

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import tachiyomi.domain.library.model.LibraryGrouping
import tachiyomi.domain.library.model.LibraryGroupingDimension
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.secondaryItemAlpha

@Composable
fun LibraryGroupingEditor(
    grouping: LibraryGrouping,
    onGroupingChange: (LibraryGrouping) -> Unit,
    modifier: Modifier = Modifier,
) {
    val orderedDimensions = remember {
        (grouping.dimensions + LibraryGroupingDimension.entries)
            .distinct()
            .toMutableStateList()
    }
    val enabledDimensions = remember {
        grouping.dimensions.toMutableStateList()
    }

    LaunchedEffect(grouping) {
        val localGrouping = orderedDimensions.filter(enabledDimensions::contains)
        if (grouping.dimensions != localGrouping) {
            enabledDimensions.clear()
            enabledDimensions.addAll(grouping.dimensions)
            val reconciledOrder = grouping.dimensions + orderedDimensions.filterNot(grouping.dimensions::contains)
            orderedDimensions.clear()
            orderedDimensions.addAll(reconciledOrder)
        }
    }

    fun commitGrouping() {
        onGroupingChange(
            LibraryGrouping(orderedDimensions.filter(enabledDimensions::contains)),
        )
    }

    val listState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(listState, PaddingValues()) { from, to ->
        val fromIndex = orderedDimensions.indexOfFirst { it.name == from.key }
        val toIndex = orderedDimensions.indexOfFirst { it.name == to.key }
        if (fromIndex == -1 || toIndex == -1) return@rememberReorderableLazyListState
        orderedDimensions.add(toIndex, orderedDimensions.removeAt(fromIndex))
        commitGrouping()
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
    ) {
        Text(
            text = stringResource(MR.strings.library_grouping_hierarchy_summary),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.secondaryItemAlpha(),
        )
        Box {
            ScrollbarLazyColumn(
                modifier = Modifier.heightIn(max = 280.dp),
                state = listState,
            ) {
                items(
                    items = orderedDimensions,
                    key = LibraryGroupingDimension::name,
                ) { dimension ->
                    ReorderableItem(reorderableState, dimension.name) {
                        LibraryGroupingDimensionItem(
                            dimension = dimension,
                            checked = dimension in enabledDimensions,
                            roleLabel = groupingRoleLabel(
                                dimension = dimension,
                                orderedDimensions = orderedDimensions,
                                enabledDimensions = enabledDimensions,
                            ),
                            onCheckedChange = { checked ->
                                if (checked) {
                                    if (dimension !in enabledDimensions) enabledDimensions += dimension
                                } else {
                                    enabledDimensions -= dimension
                                }
                                commitGrouping()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun groupingRoleLabel(
    dimension: LibraryGroupingDimension,
    orderedDimensions: List<LibraryGroupingDimension>,
    enabledDimensions: List<LibraryGroupingDimension>,
): String? {
    val enabledInOrder = orderedDimensions.filter(enabledDimensions::contains)
    return when (enabledInOrder.indexOf(dimension)) {
        0 -> stringResource(MR.strings.library_grouping_primary_tabs)
        1 -> stringResource(MR.strings.library_grouping_secondary_tabs)
        2 -> stringResource(MR.strings.library_grouping_tertiary_tabs)
        else -> null
    }
}

@Composable
private fun ReorderableCollectionItemScope.LibraryGroupingDimensionItem(
    dimension: LibraryGroupingDimension,
    checked: Boolean,
    roleLabel: String?,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.padding.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable(role = Role.Checkbox) { onCheckedChange(!checked) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = null,
            )
            Column {
                Text(
                    text = libraryGroupingDimensionLabel(dimension),
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (roleLabel != null) {
                    Text(
                        text = roleLabel,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.secondaryItemAlpha(),
                    )
                }
            }
        }
        Icon(
            imageVector = Icons.Outlined.DragHandle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .draggableHandle()
                .padding(MaterialTheme.padding.small),
        )
    }
}
