package mihon.feature.migration.list.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.feature.migration.list.models.MigratingEntry
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Close
import mihon.icons.materialsymbols.rounded.ContentCopy
import mihon.icons.materialsymbols.rounded.Search
import mihon.icons.materialsymbols.rounded.SkipNext
import mihon.icons.materialsymbols.rounded.SwapHoriz
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.menu.DropdownMenuItem
import mihon.presentation.kit.menu.OverflowMenu
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The end of a migration row: skip while searching, then a menu to search by hand, skip, or migrate this one now. */
@Composable
fun MigrationListItemAction(
    modifier: Modifier,
    result: MigratingEntry.SearchResult,
    onSearchManually: () -> Unit,
    onSkip: () -> Unit,
    onMigrate: () -> Unit,
    onCopy: () -> Unit,
) {
    Box(modifier) {
        when (result) {
            MigratingEntry.SearchResult.Searching -> {
                IconButton(
                    icon = MaterialSymbols.Rounded.Close,
                    contentDescription = stringResource(MR.strings.migrationListScreen_skipActionLabel),
                    onClick = onSkip,
                )
            }
            MigratingEntry.SearchResult.NotFound, is MigratingEntry.SearchResult.Success -> {
                OverflowMenu { close ->
                    DropdownMenuItem(
                        label = stringResource(MR.strings.migrationListScreen_searchManuallyActionLabel),
                        onClick = {
                            close()
                            onSearchManually()
                        },
                        icon = MaterialSymbols.Rounded.Search,
                    )
                    DropdownMenuItem(
                        label = stringResource(MR.strings.migrationListScreen_skipActionLabel),
                        onClick = {
                            close()
                            onSkip()
                        },
                        icon = MaterialSymbols.Rounded.SkipNext,
                    )
                    if (result is MigratingEntry.SearchResult.Success) {
                        DropdownMenuItem(
                            label = stringResource(MR.strings.migrationListScreen_migrateNowActionLabel),
                            onClick = {
                                close()
                                onMigrate()
                            },
                            icon = MaterialSymbols.Rounded.SwapHoriz,
                        )
                        DropdownMenuItem(
                            label = stringResource(MR.strings.migrationListScreen_copyNowActionLabel),
                            onClick = {
                                close()
                                onCopy()
                            },
                            icon = MaterialSymbols.Rounded.ContentCopy,
                        )
                    }
                }
            }
        }
    }
}
