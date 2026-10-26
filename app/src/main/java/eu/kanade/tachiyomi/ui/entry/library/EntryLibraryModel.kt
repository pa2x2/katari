package eu.kanade.tachiyomi.ui.entry.library

import android.content.Context
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import eu.kanade.tachiyomi.ui.entry.EntryStateAccess
import eu.kanade.tachiyomi.ui.entry.EntryViewModel
import eu.kanade.tachiyomi.ui.entry.members.EntryMembers
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import logcat.LogPriority
import mihon.entry.interactions.download.EntryDownloadMaintenanceFeature
import mihon.entry.interactions.library.membership.EntryLibraryAddRequest
import mihon.entry.interactions.library.membership.EntryLibraryAddResult
import mihon.entry.interactions.library.membership.EntryLibraryCategorySelection
import mihon.entry.interactions.library.membership.EntryLibraryDuplicatePolicy
import mihon.entry.interactions.library.membership.EntryLibraryMembershipFeature
import mihon.entry.interactions.library.membership.EntryLibraryRemovalResult
import mihon.entry.interactions.merge.EntryMergeExecutionResult
import mihon.entry.interactions.merge.EntryMergeFeature
import mihon.entry.interactions.merge.EntryMergeRemoveEntriesIntent
import mihon.entry.interactions.merge.EntryMergeSubject
import mihon.presentation.kit.feedback.SnackbarHostState
import mihon.presentation.kit.feedback.SnackbarResult
import tachiyomi.core.common.preference.mapAsCheckboxState
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.category.repository.CategoryRepository
import tachiyomi.domain.entry.interactor.SetEntryCategories
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.i18n.*
import tachiyomi.i18n.context.stringResource
import tachiyomi.source.local.isLocal

/**
 * An entry screen's library membership: adding the entry, with the duplicate and category questions that can come
 * first, removing it or its merge, its categories and its update interval.
 */
@AssistedInject
class EntryLibraryModel(
    @Assisted private val scope: CoroutineScope,
    @Assisted private val context: Context,
    @Assisted private val snackbarHostState: SnackbarHostState,
    @Assisted private val state: EntryStateAccess,
    @Assisted private val members: EntryMembers,
    private val entryLibraryMembershipFeature: EntryLibraryMembershipFeature,
    private val entryMergeFeature: EntryMergeFeature,
    private val downloadMaintenance: EntryDownloadMaintenanceFeature,
    private val categoryRepository: CategoryRepository,
    private val setEntryCategories: SetEntryCategories,
    private val entryRepository: EntryRepository,
    libraryPreferences: LibraryPreferences,
) {

    @AssistedFactory
    fun interface Factory {
        fun create(
            scope: CoroutineScope,
            context: Context,
            snackbarHostState: SnackbarHostState,
            state: EntryStateAccess,
            members: EntryMembers,
        ): EntryLibraryModel
    }

    private val dialogState = MutableStateFlow<EntryLibraryDialog?>(null)
    val dialog: StateFlow<EntryLibraryDialog?> = dialogState.asStateFlow()

    val isUpdateIntervalEnabled =
        LibraryPreferences.SKIP_OUTSIDE_RELEASE_PERIOD in libraryPreferences.updateSkipRules.get()

    private val isFavorited: Boolean
        get() = state.loaded?.entry?.favorite ?: false

    fun dismiss() {
        dialogState.value = null
    }

    fun toggleFavorite() {
        val loaded = state.loaded ?: return
        if (isFavorited) {
            if (loaded.isMerged) {
                showRemoveMergedEntryDialog(loaded)
            } else {
                dialogState.value = EntryLibraryDialog.RemoveFromLibrary(loaded.entry)
            }
            return
        }
        toggleFavorite(onRemoved = ::offerDownloadDeletion)
    }

    fun removeFromLibrary() {
        if (!isFavorited) return
        toggleFavorite(onRemoved = ::offerDownloadDeletion)
    }

    /** Adds the entry to the library, as the duplicates sheet asks when the user adds it anyway. */
    fun addDespiteDuplicates() {
        toggleFavorite(onRemoved = {}, checkDuplicate = false)
    }

    fun showDuplicateDialog() {
        val loaded = state.loaded ?: return
        if (loaded.duplicateCandidates.isEmpty()) return
        dialogState.value = EntryLibraryDialog.DuplicateEntry(loaded.entry, loaded.duplicateCandidates)
    }

    fun showMigrateDialog(duplicate: Entry) {
        val entry = state.loaded?.entry ?: return
        dialogState.value = EntryLibraryDialog.Migrate(target = entry, current = duplicate)
    }

    fun showChangeCategoryDialog() {
        val entry = state.loaded?.entry ?: return
        scope.launch {
            val categories = categoryRepository.getAll().filterNot { it.isSystemCategory }
            val selection = members.displayedIds()
                .flatMap { memberId -> categoryRepository.getCategoriesByEntryId(memberId).map { it.id } }
                .distinct()
            dialogState.value = EntryLibraryDialog.ChangeCategory(
                entry = entry,
                initialSelection = categories.mapAsCheckboxState { it.id in selection },
            )
        }
    }

    fun moveEntryToCategoriesAndAddToLibrary(entry: Entry, categories: List<Long>) {
        scope.launchIO {
            if (entry.favorite) {
                members.displayedIds().forEach { memberId ->
                    setEntryCategories.await(memberId, categories)
                }
                return@launchIO
            }
            addToLibrary(
                EntryLibraryAddRequest(
                    entry = entry,
                    duplicatePolicy = EntryLibraryDuplicatePolicy.ALLOW,
                    categorySelection = EntryLibraryCategorySelection.Selected(categories),
                ),
            )
        }
    }

    fun showSetFetchIntervalDialog() {
        val entry = state.loaded?.entry ?: return
        dialogState.value = EntryLibraryDialog.SetFetchInterval(entry)
    }

    fun setFetchInterval(entry: Entry, interval: Int) {
        scope.launchIO {
            val updatedEntry = entry.copy(
                // Custom intervals are negative
                fetchInterval = -interval,
            )
            if (entryRepository.update(updatedEntry)) {
                val refreshedEntry = entryRepository.getEntryById(entry.id)
                if (refreshedEntry != null) {
                    state.update { it.copy(entry = refreshedEntry).rebuildChapterPresentation() }
                }
            }
        }
    }

    fun removeMergedEntry(entries: List<Entry>, deleteFromLibrary: Boolean, deleteChapters: Boolean) {
        val loaded = state.loaded ?: return
        scope.launchIO {
            val result = entryMergeFeature.execute(
                EntryMergeRemoveEntriesIntent(
                    subject = EntryMergeSubject(loaded.entry.profileId, loaded.entry.id),
                    entryIds = entries.mapTo(mutableSetOf(), Entry::id),
                    removeFromLibrary = deleteFromLibrary,
                    removeDownloads = deleteChapters,
                ),
            )
            if (result is EntryMergeExecutionResult.Applied) dismiss()
        }
    }

    private fun toggleFavorite(
        onRemoved: (List<Entry>) -> Unit,
        checkDuplicate: Boolean = true,
    ) {
        val loaded = state.loaded ?: return
        scope.launchIO {
            val entry = loaded.entry

            if (isFavorited) {
                when (val result = entryLibraryMembershipFeature.remove(listOf(entry))) {
                    is EntryLibraryRemovalResult.Removed -> withUIContext {
                        onRemoved(result.downloadDecisionEntries)
                    }
                    is EntryLibraryRemovalResult.Failed -> logcat(LogPriority.ERROR, result.cause)
                    is EntryLibraryRemovalResult.LockedInMerge -> withUIContext {
                        val locked = result.members.first()
                        snackbarHostState.showSnackbar(
                            context.stringResource(
                                MR.strings.merge_locked_member_removal,
                                locked.member.displayTitle,
                                locked.mergedEntry.displayTitle,
                            ),
                        )
                    }
                    EntryLibraryRemovalResult.NoChange -> Unit
                }
            } else {
                addToLibrary(
                    EntryLibraryAddRequest(
                        entry = loaded.libraryAdditionEntry(),
                        duplicatePolicy = if (checkDuplicate) {
                            EntryLibraryDuplicatePolicy.CHECK
                        } else {
                            EntryLibraryDuplicatePolicy.ALLOW
                        },
                    ),
                )
            }
        }
    }

    /**
     * A member shown inside its merge, such as a box-set title opened from Browse, adds the merged entry the screen
     * shows. Adding the member alone would take it out of a merge outside the Library.
     */
    private suspend fun EntryViewModel.State.Success.libraryAdditionEntry(): Entry {
        if (!isMerged || mergeTargetId == entry.id) return entry
        return entryOrNull(mergeTargetId)?.takeUnless(Entry::favorite) ?: entry
    }

    private suspend fun addToLibrary(request: EntryLibraryAddRequest) {
        when (val result = entryLibraryMembershipFeature.add(request)) {
            is EntryLibraryAddResult.DuplicateCandidates -> {
                dialogState.value = EntryLibraryDialog.DuplicateEntry(result.entry, result.candidates)
            }
            is EntryLibraryAddResult.CategorySelectionRequired -> {
                dialogState.value = EntryLibraryDialog.ChangeCategory(
                    entry = result.entry,
                    initialSelection = result.categories.mapAsCheckboxState {
                        it.id in result.selectedCategoryIds
                    },
                )
            }
            is EntryLibraryAddResult.Failed -> logcat(LogPriority.ERROR, result.cause)
            is EntryLibraryAddResult.Added,
            is EntryLibraryAddResult.AlreadyInLibrary,
            -> Unit
        }
    }

    private fun showRemoveMergedEntryDialog(loaded: EntryViewModel.State.Success) {
        if (!loaded.isMerged) return
        scope.launchIO {
            val mergeMembers = loaded.memberIds.mapNotNull { memberId -> entryOrNull(memberId) }.toImmutableList()
            dialogState.value = EntryLibraryDialog.RemoveMergedEntry(
                members = mergeMembers,
                containsLocalEntry = mergeMembers.any { it.isLocal() },
            )
        }
    }

    private fun offerDownloadDeletion(downloadEntries: List<Entry>) {
        scope.launch {
            if (downloadEntries.isEmpty()) return@launch
            val result = snackbarHostState.showSnackbar(
                message = context.stringResource(MR.strings.delete_downloads_for_manga),
                actionLabel = context.stringResource(MR.strings.action_delete),
                withDismissAction = true,
            )
            if (result == SnackbarResult.ActionPerformed) {
                scope.launchNonCancellable {
                    downloadEntries.forEach { entry -> downloadMaintenance.removeEntryDownloads(entry) }
                }
            }
        }
    }

    private suspend fun entryOrNull(id: Long): Entry? {
        return runCatching { entryRepository.getEntryById(id) }.getOrNull()
    }
}
