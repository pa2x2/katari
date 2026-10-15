package eu.kanade.presentation.library.settings

import androidx.compose.runtime.Composable
import eu.kanade.tachiyomi.ui.library.LibrarySettingsViewModel
import mihon.entry.interactions.library.EntryLibraryFilterAvailability
import mihon.presentation.kit.sheet.TabbedSheet
import tachiyomi.domain.category.model.Category
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun LibrarySettingsDialog(
    onDismissRequest: () -> Unit,
    viewModel: LibrarySettingsViewModel,
    category: Category?,
    filterAvailability: EntryLibraryFilterAvailability,
) {
    TabbedSheet(
        onDismissRequest = onDismissRequest,
        tabTitles = listOf(
            stringResource(MR.strings.action_filter),
            stringResource(MR.strings.action_sort),
            stringResource(MR.strings.action_display),
            stringResource(MR.strings.action_group),
        ),
    ) { page ->
        when (page) {
            0 -> LibraryFilterPage(
                viewModel = viewModel,
                filterAvailability = filterAvailability,
            )
            1 -> LibrarySortPage(
                category = category,
                viewModel = viewModel,
                progressSummaryAvailable = filterAvailability.progressSummary.isAvailable,
            )
            2 -> LibraryDisplayPage(
                viewModel = viewModel,
            )
            3 -> LibraryGroupPage(
                viewModel = viewModel,
            )
        }
    }
}
