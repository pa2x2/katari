package eu.kanade.tachiyomi.ui.entry.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.kanade.presentation.browse.components.RemoveMangaDialog
import eu.kanade.presentation.category.components.ChangeCategoryDialog
import eu.kanade.presentation.entry.components.DuplicateEntrySheet
import eu.kanade.presentation.entry.components.SetIntervalDialog
import eu.kanade.presentation.library.DeleteLibraryEntriesDialog
import eu.kanade.presentation.util.Screen
import mihon.feature.migration.dialog.MigrateEntryDialog

/** Shows [model]'s dialog; [onOpenEntry] opens another entry and [onEditCategories] the category settings. */
@Composable
fun Screen.EntryLibraryDialogs(
    model: EntryLibraryModel,
    onOpenEntry: (Long) -> Unit,
    onEditCategories: () -> Unit,
) {
    val current by model.dialog.collectAsStateWithLifecycle()
    val onDismissRequest = { model.dismiss() }
    when (val dialog = current) {
        null -> {}
        is EntryLibraryDialog.ChangeCategory -> {
            ChangeCategoryDialog(
                initialSelection = dialog.initialSelection,
                onDismissRequest = onDismissRequest,
                onEditCategories = onEditCategories,
                onConfirm = { include, _ ->
                    model.moveEntryToCategoriesAndAddToLibrary(dialog.entry, include)
                },
            )
        }
        is EntryLibraryDialog.DuplicateEntry -> {
            DuplicateEntrySheet(
                duplicates = dialog.duplicates,
                onDismissRequest = onDismissRequest,
                onConfirm = model::addDespiteDuplicates,
                onOpenEntry = { onOpenEntry(it.id) },
                onMigrate = model::showMigrateDialog,
            )
        }
        is EntryLibraryDialog.RemoveFromLibrary -> {
            RemoveMangaDialog(
                onDismissRequest = onDismissRequest,
                onConfirm = model::removeFromLibrary,
                mangaToRemove = dialog.entry,
            )
        }
        is EntryLibraryDialog.RemoveMergedEntry -> {
            DeleteLibraryEntriesDialog(
                containsLocalEntries = dialog.containsLocalEntry,
                containsMergedEntries = true,
                onDismissRequest = onDismissRequest,
                onConfirm = { deleteEntry, deleteChapter ->
                    model.removeMergedEntry(dialog.members, deleteEntry, deleteChapter)
                },
            )
        }
        is EntryLibraryDialog.Migrate -> {
            MigrateEntryDialog(
                current = dialog.current,
                target = dialog.target,
                // Initiated from the context of [dialog.target] so we show [dialog.current].
                onClickTitle = { onOpenEntry(dialog.current.id) },
                onDismissRequest = onDismissRequest,
            )
        }
        is EntryLibraryDialog.SetFetchInterval -> {
            SetIntervalDialog(
                interval = dialog.entry.fetchInterval,
                nextUpdate = dialog.entry.expectedNextUpdate,
                entryType = dialog.entry.type,
                onDismissRequest = onDismissRequest,
                onValueChanged = { interval: Int -> model.setFetchInterval(dialog.entry, interval) }
                    .takeIf { model.isUpdateIntervalEnabled },
            )
        }
    }
}
