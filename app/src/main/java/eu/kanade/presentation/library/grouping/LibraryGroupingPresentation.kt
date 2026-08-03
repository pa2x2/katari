package eu.kanade.presentation.library.grouping

import androidx.compose.runtime.Composable
import tachiyomi.domain.library.model.LibraryGrouping
import tachiyomi.domain.library.model.LibraryGroupingDimension
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun libraryGroupingSummary(grouping: LibraryGrouping): String {
    if (grouping.dimensions.isEmpty()) return stringResource(MR.strings.library_grouping_ungrouped)
    return grouping.dimensions
        .map { libraryGroupingDimensionLabel(it) }
        .joinToString(separator = " → ")
}

@Composable
fun libraryGroupingDimensionLabel(dimension: LibraryGroupingDimension): String {
    return when (dimension) {
        LibraryGroupingDimension.Category -> stringResource(MR.strings.action_group_category)
        LibraryGroupingDimension.EntryType -> stringResource(MR.strings.action_group_type)
        LibraryGroupingDimension.Source -> stringResource(MR.strings.action_group_extension)
    }
}

@Composable
fun showLibraryGroupingTabsLabel(grouping: LibraryGrouping): String {
    return when (grouping.dimensions.singleOrNull()) {
        LibraryGroupingDimension.Category -> stringResource(MR.strings.action_display_show_tabs)
        LibraryGroupingDimension.EntryType -> stringResource(MR.strings.action_display_show_type_tabs)
        LibraryGroupingDimension.Source -> stringResource(MR.strings.action_display_show_extension_tabs)
        null -> stringResource(MR.strings.action_display_show_group_tabs)
    }
}
