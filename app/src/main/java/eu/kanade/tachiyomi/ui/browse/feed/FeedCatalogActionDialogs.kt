package eu.kanade.tachiyomi.ui.browse.feed

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.presentation.browse.components.BrowseEntryPreviewSheet
import eu.kanade.presentation.browse.components.BrowseLibraryActionDialog
import eu.kanade.presentation.browse.components.BrowseMergeEditorDialog
import eu.kanade.presentation.browse.components.DuplicateDetectionLoadingDialog
import eu.kanade.presentation.browse.components.MergeTargetPickerDialog
import eu.kanade.presentation.browse.components.RemoveMangaDialog
import eu.kanade.presentation.category.components.ChangeCategoryDialog
import eu.kanade.presentation.entry.components.DuplicateEntryDialog
import eu.kanade.tachiyomi.ui.browse.catalog.CatalogViewModel
import eu.kanade.tachiyomi.ui.category.CategoryScreen
import eu.kanade.tachiyomi.ui.entry.EntryScreen
import mihon.feature.migration.dialog.MigrateEntryDialog
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The dialogs a feed entry's library actions open, driven by the feed's catalog action model. */
@Composable
internal fun Screen.FeedCatalogActionDialogs(
    dialog: CatalogViewModel.Dialog?,
    viewModel: CatalogViewModel,
    navigator: Navigator,
) {
    val onDismissRequest = viewModel::dismissDialog
    when (dialog) {
        is CatalogViewModel.Dialog.EntryPreview -> {
            BrowseEntryPreviewSheet(
                entryId = dialog.entryId,
                onLibraryAction = viewModel::confirmBrowseLibraryAction,
                onMergeAction = viewModel::showMergeTargetPicker,
                onOpenEntry = {
                    onDismissRequest()
                    navigator.push(EntryScreen(it, fromSource = true))
                },
                onDismissRequest = onDismissRequest,
            )
        }
        is CatalogViewModel.Dialog.LibraryActionChooser -> {
            BrowseLibraryActionDialog(
                mangaTitle = dialog.entry.displayTitle,
                favorite = dialog.entry.favorite,
                onDismissRequest = onDismissRequest,
                onLibraryAction = {
                    onDismissRequest()
                    viewModel.confirmBrowseLibraryAction(dialog.entry)
                },
                onMergeIntoLibrary = { viewModel.showMergeTargetPicker(dialog.entry) },
            )
        }
        CatalogViewModel.Dialog.CheckingDuplicates -> DuplicateDetectionLoadingDialog()
        is CatalogViewModel.Dialog.RemoveEntry -> {
            RemoveMangaDialog(
                onDismissRequest = onDismissRequest,
                onConfirm = { viewModel.changeFavorite(dialog.entry) },
                mangaToRemove = dialog.entry,
            )
        }
        is CatalogViewModel.Dialog.DuplicateEntry -> {
            DuplicateEntryDialog(
                duplicates = dialog.duplicates,
                onDismissRequest = onDismissRequest,
                onConfirm = { viewModel.addFavorite(dialog.entry) },
                onOpenEntry = { navigator.push(EntryScreen(it.id, fromSource = true)) },
                onMigrate = {
                    viewModel.showMigrateEntryDialog(current = it, target = dialog.entry)
                },
            )
        }
        is CatalogViewModel.Dialog.SelectEntryMergeTarget -> {
            MergeTargetPickerDialog(
                title = stringResource(MR.strings.action_merge_into_library),
                query = dialog.query,
                visibleTargets = dialog.visibleTargets,
                onDismissRequest = onDismissRequest,
                onQueryChange = viewModel::updateMergeTargetQuery,
                onSelectTarget = viewModel::openMergeEditor,
            )
        }
        is CatalogViewModel.Dialog.EditEntryMerge -> {
            BrowseMergeEditorDialog(
                entries = dialog.entries,
                targetId = dialog.targetId,
                targetLocked = dialog.targetLocked,
                removedIds = dialog.removedIds,
                libraryRemovalIds = dialog.libraryRemovalIds,
                confirmEnabled = dialog.enabled,
                onDismissRequest = onDismissRequest,
                onMove = viewModel::moveMergeEntry,
                onSelectTarget = viewModel::setMergeTarget,
                onToggleRemove = viewModel::toggleMergeEntryRemoval,
                onToggleLibraryRemove = viewModel::toggleMergeEntryLibraryRemoval,
                onConfirm = viewModel::confirmBrowseMerge,
            )
        }
        is CatalogViewModel.Dialog.ChangeEntryCategory -> {
            ChangeCategoryDialog(
                initialSelection = dialog.initialSelection,
                onDismissRequest = onDismissRequest,
                onEditCategories = { navigator.push(CategoryScreen()) },
                onConfirm = { include, _ ->
                    viewModel.addFavorite(dialog.entry, include)
                },
            )
        }
        is CatalogViewModel.Dialog.MigrateEntry -> {
            MigrateEntryDialog(
                current = dialog.current,
                target = dialog.target,
                onClickTitle = { navigator.push(EntryScreen(dialog.current.id, fromSource = true)) },
                onDismissRequest = onDismissRequest,
                onComplete = {
                    viewModel.dismissDialog()
                    navigator.push(EntryScreen(dialog.target.id, fromSource = true))
                },
            )
        }
        CatalogViewModel.Dialog.Filter,
        is CatalogViewModel.Dialog.SavePreset,
        null,
        -> Unit
    }
}
