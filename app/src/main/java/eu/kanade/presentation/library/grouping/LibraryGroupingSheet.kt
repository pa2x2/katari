package eu.kanade.presentation.library.grouping

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.presentation.kit.sheet.Sheet
import mihon.presentation.kit.sheet.SheetHeader
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.domain.library.model.LibraryGrouping
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The library's grouping hierarchy outside the library, applied as it changes, like the library sheet's Group tab. */
@Composable
fun LibraryGroupingSheet(
    grouping: LibraryGrouping,
    onGroupingChange: (LibraryGrouping) -> Unit,
    onDismissRequest: () -> Unit,
) {
    Sheet(onDismissRequest = onDismissRequest) {
        Column {
            SheetHeader(title = stringResource(MR.strings.library_grouping_hierarchy))
            LibraryGroupingEditor(
                grouping = grouping,
                onGroupingChange = onGroupingChange,
                modifier = Modifier.padding(bottom = KatariTheme.spacing.l),
            )
        }
    }
}
