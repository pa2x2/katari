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
import eu.kanade.tachiyomi.ui.browse.catalog.CatalogScreenModel
import eu.kanade.tachiyomi.ui.category.CategoryScreen
import eu.kanade.tachiyomi.ui.entry.EntryScreen
import mihon.feature.migration.dialog.MigrateEntryDialog
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The dialogs a feed entry's library actions open, driven by the feed's catalog action model. */
@Composable
internal fun Screen.FeedCatalogActionDialogs(
    dialog: CatalogScreenModel.Dialog?,
    screenModel: CatalogScreenModel,
    navigator: Navigator,
) {
    val onDismissRequest = screenModel::dismissDialog
    when (dialog) {
        is CatalogScreenModel.Dialog.EntryPreview -> {
            BrowseEntryPreviewSheet(
                entryId = dialog.entryId,
                onLibraryAction = screenModel::confirmBrowseLibraryAction,
                onMergeAction = screenModel::showMergeTargetPicker,
                onOpenEntry = {
                    onDismissRequest()
                    navigator.push(EntryScreen(it, fromSource = true))
                },
                onDismissRequest = onDismissRequest,
            )
        }
        is CatalogScreenModel.Dialog.LibraryActionChooser -> {
            BrowseLibraryActionDialog(
                mangaTitle = dialog.entry.displayTitle,
                favorite = dialog.entry.favorite,
                onDismissRequest = onDismissRequest,
                onLibraryAction = {
                    onDismissRequest()
                    screenModel.confirmBrowseLibraryAction(dialog.entry)
                },
                onMergeIntoLibrary = { screenModel.showMergeTargetPicker(dialog.entry) },
            )
        }
        CatalogScreenModel.Dialog.CheckingDuplicates -> DuplicateDetectionLoadingDialog()
        is CatalogScreenModel.Dialog.RemoveEntry -> {
            RemoveMangaDialog(
                onDismissRequest = onDismissRequest,
                onConfirm = { screenModel.changeFavorite(dialog.entry) },
                mangaToRemove = dialog.entry,
            )
        }
        is CatalogScreenModel.Dialog.DuplicateEntry -> {
            DuplicateEntryDialog(
                duplicates = dialog.duplicates,
                onDismissRequest = onDismissRequest,
                onConfirm = { screenModel.addFavorite(dialog.entry) },
                onOpenEntry = { navigator.push(EntryScreen(it.id, fromSource = true)) },
                onMigrate = {
                    screenModel.showMigrateEntryDialog(current = it, target = dialog.entry)
                },
            )
        }
        is CatalogScreenModel.Dialog.SelectEntryMergeTarget -> {
            MergeTargetPickerDialog(
                title = stringResource(MR.strings.action_merge_into_library),
                query = dialog.query,
                visibleTargets = dialog.visibleTargets,
                onDismissRequest = onDismissRequest,
                onQueryChange = screenModel::updateMergeTargetQuery,
                onSelectTarget = screenModel::openMergeEditor,
            )
        }
        is CatalogScreenModel.Dialog.EditEntryMerge -> {
            BrowseMergeEditorDialog(
                entries = dialog.entries,
                targetId = dialog.targetId,
                targetLocked = dialog.targetLocked,
                removedIds = dialog.removedIds,
                libraryRemovalIds = dialog.libraryRemovalIds,
                confirmEnabled = dialog.enabled,
                onDismissRequest = onDismissRequest,
                onMove = screenModel::moveMergeEntry,
                onSelectTarget = screenModel::setMergeTarget,
                onToggleRemove = screenModel::toggleMergeEntryRemoval,
                onToggleLibraryRemove = screenModel::toggleMergeEntryLibraryRemoval,
                onConfirm = screenModel::confirmBrowseMerge,
            )
        }
        is CatalogScreenModel.Dialog.ChangeEntryCategory -> {
            ChangeCategoryDialog(
                initialSelection = dialog.initialSelection,
                onDismissRequest = onDismissRequest,
                onEditCategories = { navigator.push(CategoryScreen()) },
                onConfirm = { include, _ ->
                    screenModel.addFavorite(dialog.entry, include)
                },
            )
        }
        is CatalogScreenModel.Dialog.MigrateEntry -> {
            MigrateEntryDialog(
                current = dialog.current,
                target = dialog.target,
                onClickTitle = { navigator.push(EntryScreen(dialog.current.id, fromSource = true)) },
                onDismissRequest = onDismissRequest,
                onComplete = {
                    screenModel.dismissDialog()
                    navigator.push(EntryScreen(dialog.target.id, fromSource = true))
                },
            )
        }
        CatalogScreenModel.Dialog.Filter,
        is CatalogScreenModel.Dialog.SavePreset,
        null,
        -> Unit
    }
}
