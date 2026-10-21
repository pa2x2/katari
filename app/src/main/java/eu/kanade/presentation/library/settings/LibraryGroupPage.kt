package eu.kanade.presentation.library.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import eu.kanade.presentation.library.grouping.LibraryGroupingEditor
import eu.kanade.tachiyomi.ui.library.LibrarySettingsViewModel
import mihon.presentation.kit.list.RowGroupHeader
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

@Composable
internal fun LibraryGroupPage(
    viewModel: LibrarySettingsViewModel,
) {
    val grouping by viewModel.libraryPreferences.grouping.collectAsState()
    RowGroupHeader(stringResource(MR.strings.library_grouping_hierarchy))
    LibraryGroupingEditor(
        grouping = grouping,
        onGroupingChange = viewModel::setGrouping,
    )
}
