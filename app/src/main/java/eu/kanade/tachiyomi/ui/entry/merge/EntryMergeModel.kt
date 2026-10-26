package eu.kanade.tachiyomi.ui.entry.merge

import android.content.Context
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import eu.kanade.presentation.entry.components.MergeEditorEntry
import eu.kanade.presentation.entry.components.MergeTarget
import eu.kanade.presentation.entry.components.MergeTargetSearchController
import eu.kanade.presentation.entry.components.buildMergeTargetQuery
import eu.kanade.presentation.entry.components.buildMergeTargets
import eu.kanade.presentation.entry.components.rankMergeTargets
import eu.kanade.presentation.entry.components.toMergeEditorLock
import eu.kanade.tachiyomi.ui.entry.EntryStateAccess
import eu.kanade.tachiyomi.ui.entry.members.EntryMembers
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import mihon.entry.interactions.merge.EntryMergeCommitIntent
import mihon.entry.interactions.merge.EntryMergeDissolveIntent
import mihon.entry.interactions.merge.EntryMergeEditorEntry
import mihon.entry.interactions.merge.EntryMergeEditorEntryOrigin
import mihon.entry.interactions.merge.EntryMergeEditorEntryReference
import mihon.entry.interactions.merge.EntryMergeExecutionResult
import mihon.entry.interactions.merge.EntryMergeFeature
import mihon.entry.interactions.merge.EntryMergeMemberPreparationIntent
import mihon.entry.interactions.merge.EntryMergePreparationResult
import mihon.entry.interactions.merge.EntryMergePrepareIntent
import mihon.entry.interactions.merge.EntryMergeRemoveEntriesIntent
import mihon.entry.interactions.merge.EntryMergeSubject
import mihon.presentation.kit.feedback.SnackbarHostState
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.category.repository.CategoryRepository
import tachiyomi.domain.entry.interactor.GetLibraryEntries
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.service.showsMergedMembersReversed
import tachiyomi.domain.library.service.DuplicatePreferences
import tachiyomi.domain.source.service.SourceInfoNames
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.*
import tachiyomi.i18n.context.stringResource

/** The sheets an entry screen merges through: adding the entry to another library entry, and managing its merge. */
@AssistedInject
class EntryMergeModel(
    @Assisted private val scope: CoroutineScope,
    @Assisted private val context: Context,
    @Assisted private val snackbarHostState: SnackbarHostState,
    @Assisted private val state: EntryStateAccess,
    @Assisted private val members: EntryMembers,
    private val entryMergeFeature: EntryMergeFeature,
    private val getLibraryEntries: GetLibraryEntries,
    private val categoryRepository: CategoryRepository,
    private val duplicatePreferences: DuplicatePreferences,
    private val sourceManager: SourceManager,
    private val sourceNames: SourceInfoNames,
) {

    @AssistedFactory
    fun interface Factory {
        fun create(
            scope: CoroutineScope,
            context: Context,
            snackbarHostState: SnackbarHostState,
            state: EntryStateAccess,
            members: EntryMembers,
        ): EntryMergeModel
    }

    private val sheetState = MutableStateFlow<EntryMergeSheet?>(null)
    val sheet: StateFlow<EntryMergeSheet?> = sheetState.asStateFlow()

    private val targetSearchController = MergeTargetSearchController<MergeTarget>(scope)

    fun dismiss() {
        targetSearchController.cancelPending()
        sheetState.value = null
    }

    fun showTargetPicker() {
        val loaded = state.loaded ?: return
        targetSearchController.cancelPending()
        scope.launchIO {
            val excludedIds = loaded.mergeGroupMemberIds.toSet()
            val targets = buildMergeTargets(
                libraryItems = getLibraryEntries.await(),
                sourceNames = sourceNames,
                entryType = loaded.entry.type,
                excludedEntryIds = excludedIds,
            )
            if (targets.isEmpty()) {
                snackbarHostState.showSnackbar(context.stringResource(MR.strings.merge_target_picker_none))
                return@launchIO
            }
            val query = buildMergeTargetQuery(loaded.entry.displayTitle, duplicatePreferences)
            val visibleTargets = rankMergeTargets(targets, query).toImmutableList()
            sheetState.value = EntryMergeSheet.SelectTarget(
                entry = loaded.entry,
                query = query,
                targets = targets,
                visibleTargets = visibleTargets,
            )
        }
    }

    fun updateTargetQuery(query: String) {
        val picker = sheetState.value as? EntryMergeSheet.SelectTarget ?: return
        if (picker.query == query) return
        sheetState.update { sheet ->
            val current = sheet as? EntryMergeSheet.SelectTarget ?: return@update sheet
            if (current.targets !== picker.targets) return@update sheet
            // The last results stay until the new ones are ranked; emptying them would flash the empty state
            current.copy(query = query)
        }
        targetSearchController.submit(picker.targets, query) { visibleTargets ->
            sheetState.update { sheet ->
                val current = sheet as? EntryMergeSheet.SelectTarget ?: return@update sheet
                if (current.targets !== picker.targets || current.query != query) return@update sheet
                current.copy(visibleTargets = visibleTargets)
            }
        }
    }

    fun openEditor(targetId: Long) {
        val picker = sheetState.value as? EntryMergeSheet.SelectTarget ?: return
        scope.launchIO {
            val target = picker.visibleTargets.firstOrNull { it.id == targetId } ?: return@launchIO
            val editor = createEditor(picker.entry, target) ?: return@launchIO
            sheetState.update { sheet ->
                val current = sheet as? EntryMergeSheet.SelectTarget ?: return@update sheet
                if (current.targets !== picker.targets) return@update sheet
                editor
            }
        }
    }

    fun moveEditorEntry(fromIndex: Int, toIndex: Int) {
        updateEditor { editor ->
            val entries = editor.entries.toMutableList()
            if (fromIndex !in entries.indices || toIndex !in entries.indices) return@updateEditor editor
            val entry = entries.removeAt(fromIndex)
            entries.add(toIndex, entry)
            editor.copy(entries = entries.toImmutableList())
        }
    }

    fun setEditorTarget(entryId: Long) {
        updateEditor { editor ->
            if (editor.entries.none { it.id == entryId }) return@updateEditor editor
            val reference = editor.referencesById[entryId] ?: return@updateEditor editor
            if (editor.targetLocked) return@updateEditor editor
            editor.copy(
                targetId = entryId,
                targetReference = reference,
                removedIds = editor.removedIds - entryId,
                libraryRemovalIds = editor.libraryRemovalIds - entryId,
            )
        }
    }

    fun toggleEditorRemoval(entryId: Long) {
        updateEditor { editor ->
            val entry = editor.entries.firstOrNull { it.id == entryId } ?: return@updateEditor editor
            if (!entry.isRemovable || entryId == editor.targetId) return@updateEditor editor
            editor.copy(removedIds = editor.removedIds.toggled(entryId))
        }
    }

    fun toggleEditorLibraryRemoval(entryId: Long) {
        updateEditor { editor ->
            val entry = editor.entries.firstOrNull { it.id == entryId } ?: return@updateEditor editor
            if (!entry.isRemovable || entryId == editor.targetId) return@updateEditor editor
            editor.copy(libraryRemovalIds = editor.libraryRemovalIds.toggled(entryId))
        }
    }

    fun confirmEditor() {
        val editor = sheetState.value as? EntryMergeSheet.Edit ?: return
        scope.launchIO {
            val result = entryMergeFeature.execute(
                EntryMergeCommitIntent(
                    editReference = editor.editReference,
                    target = editor.targetReference,
                    orderedEntries = editor.entries.mapNotNull { editor.referencesById[it.id] },
                    removedEntries = editor.referencesById.referencesFor(editor.removedIds),
                    libraryRemovalEntries = editor.referencesById.referencesFor(editor.libraryRemovalIds),
                ),
            )
            if (result is EntryMergeExecutionResult.Applied) {
                dismiss()
            }
        }
    }

    fun showManageSheet() {
        val loaded = state.loaded ?: return
        if (!loaded.isPartOfMerge) return
        scope.launchIO {
            val editor = members.existingMergeEditor(loaded.entry) ?: return@launchIO
            val target = editor.entries.firstOrNull { it.reference == editor.target } ?: return@launchIO
            val mergeMembers = editor.entries.map { editorEntry ->
                MergeMember(
                    id = editorEntry.entry.id,
                    reference = editorEntry.reference,
                    entry = editorEntry.entry,
                    subtitle = subtitle(editorEntry.entry),
                    removable = editorEntry.removable,
                    locked = editorEntry.locked,
                )
            }.inMergeSheetOrder(loaded.entry, target.entry.id).toImmutableList()
            sheetState.value = EntryMergeSheet.Manage(
                editReference = editor.editReference,
                targetId = target.entry.id,
                savedTargetId = target.entry.id,
                targetLocked = editor.targetLocked,
                lock = editor.toMergeEditorLock(::sourceName),
                members = mergeMembers,
            )
        }
    }

    fun removeMembers(entryIds: List<Long>) {
        val loaded = state.loaded ?: return
        val manage = sheetState.value as? EntryMergeSheet.Manage
        scope.launchIO {
            if (entryIds.isEmpty()) return@launchIO
            if (manage != null) {
                save(manage, entryIds)
            } else {
                entryMergeFeature.execute(
                    EntryMergeRemoveEntriesIntent(
                        subject = EntryMergeSubject(loaded.entry.profileId, loaded.entry.id),
                        entryIds = entryIds.toSet(),
                        removeFromLibrary = false,
                        removeDownloads = false,
                    ),
                )
            }
            dismiss()
        }
    }

    fun reorderMembers(fromIndex: Int, toIndex: Int) {
        updateManage { manage ->
            if (fromIndex !in manage.members.indices || toIndex !in manage.members.indices) {
                return@updateManage manage
            }
            val reordered = manage.members.toMutableList().apply {
                val item = removeAt(fromIndex)
                add(toIndex, item)
            }
            val reorderedRemovalIds = reordered.mapNotNull { member ->
                member.id.takeIf { it in manage.removableIds }
            }.toImmutableList()
            val reorderedLibraryRemovalIds = reordered.mapNotNull { member ->
                member.id.takeIf { it in manage.libraryRemovalIds }
            }.toImmutableList()
            manage.copy(
                members = reordered.toImmutableList(),
                removableIds = reorderedRemovalIds,
                libraryRemovalIds = reorderedLibraryRemovalIds,
            )
        }
    }

    fun setManageTarget(entryId: Long) {
        updateManage { manage ->
            if (manage.members.none { it.id == entryId }) return@updateManage manage
            manage.copy(
                targetId = entryId,
                removableIds = manage.removableIds.filterNot { it == entryId }.toImmutableList(),
                libraryRemovalIds = manage.libraryRemovalIds.filterNot { it == entryId }.toImmutableList(),
            )
        }
    }

    fun toggleMemberRemoval(entryId: Long) {
        updateManage { manage ->
            if (entryId == manage.targetId || manage.members.none { it.id == entryId }) return@updateManage manage
            manage.copy(removableIds = manage.removableIds.toggled(entryId).toImmutableList())
        }
    }

    fun toggleMemberLibraryRemoval(entryId: Long) {
        updateManage { manage ->
            if (entryId == manage.targetId || manage.members.none { it.id == entryId }) return@updateManage manage
            manage.copy(libraryRemovalIds = manage.libraryRemovalIds.toggled(entryId).toImmutableList())
        }
    }

    fun saveOrder() {
        val manage = sheetState.value as? EntryMergeSheet.Manage ?: return
        scope.launchIO {
            save(manage, manage.removableIds + manage.libraryRemovalIds)
            dismiss()
        }
    }

    fun unmergeAll() {
        val loaded = state.loaded ?: return
        scope.launchIO {
            val result = entryMergeFeature.execute(
                EntryMergeDissolveIntent(EntryMergeSubject(loaded.entry.profileId, loaded.entry.id)),
            )
            if (result is EntryMergeExecutionResult.Applied) dismiss()
        }
    }

    private inline fun updateEditor(crossinline transform: (EntryMergeSheet.Edit) -> EntryMergeSheet.Edit) {
        sheetState.update { sheet -> (sheet as? EntryMergeSheet.Edit)?.let(transform) ?: sheet }
    }

    private inline fun updateManage(crossinline transform: (EntryMergeSheet.Manage) -> EntryMergeSheet.Manage) {
        sheetState.update { sheet -> (sheet as? EntryMergeSheet.Manage)?.let(transform) ?: sheet }
    }

    private suspend fun save(manage: EntryMergeSheet.Manage, entryIdsToRemove: Collection<Long>) {
        val entry = state.loaded?.entry ?: return
        val target = manage.members.firstOrNull { it.id == manage.targetId } ?: return
        entryMergeFeature.execute(
            EntryMergeCommitIntent(
                editReference = manage.editReference,
                target = target.reference,
                // The sheet's order is its own inverse, so applying it again restores the stored order
                orderedEntries = manage.members.inMergeSheetOrder(entry, manage.targetId).map(MergeMember::reference),
                removedEntries = manage.members.referencesFor(entryIdsToRemove - manage.libraryRemovalIds.toSet()),
                libraryRemovalEntries = manage.members.referencesFor(manage.libraryRemovalIds),
            ),
        )
    }

    private suspend fun createEditor(entry: Entry, target: MergeTarget): EntryMergeSheet.Edit? {
        val targetEntry = target.memberEntries.firstOrNull { it.id == target.id }
            ?: target.memberEntries.firstOrNull()
            ?: return null
        val categoryIds = target.categoryIds.ifEmpty {
            categoryRepository.getCategoriesByEntryId(targetEntry.id).map { it.id }
        }
        val preparations = if (entry.favorite) {
            emptyList()
        } else {
            listOf(EntryMergeMemberPreparationIntent(entry, categoryIds))
        }
        val editor = when (
            val result = entryMergeFeature.prepare(
                EntryMergePrepareIntent(
                    // The entry being added goes first, so it tops the editor whether the target is merged yet or not.
                    selectedEntries = listOf(entry, targetEntry),
                    root = targetEntry,
                    preparations = preparations,
                ),
            )
        ) {
            is EntryMergePreparationResult.Ready -> result.editor
            is EntryMergePreparationResult.Rejected -> return null
        }
        val targetEditorEntry = editor.entries.firstOrNull { it.reference == editor.target } ?: return null
        return EntryMergeSheet.Edit(
            entry = entry,
            editReference = editor.editReference,
            targetId = targetEditorEntry.entry.id,
            targetReference = targetEditorEntry.reference,
            targetLocked = editor.targetLocked,
            lock = editor.toMergeEditorLock(::sourceName),
            entries = editor.entries.map(::toEditorEntry).toImmutableList(),
            referencesById = editor.entries.associate { it.entry.id to it.reference },
            removedIds = emptySet(),
            libraryRemovalIds = emptySet(),
        )
    }

    private fun toEditorEntry(editorEntry: EntryMergeEditorEntry): MergeEditorEntry {
        val isExisting = editorEntry.origin == EntryMergeEditorEntryOrigin.EXISTING_MEMBER
        return MergeEditorEntry(
            id = editorEntry.entry.id,
            entry = editorEntry.entry,
            subtitle = subtitle(editorEntry.entry) + if (isExisting) "" else " • New",
            isRemovable = editorEntry.removable,
            isMember = isExisting,
            locked = editorEntry.locked,
        )
    }

    private fun sourceName(sourceId: Long): String = sourceManager.getDisplayInfo(sourceId).name

    private fun subtitle(entry: Entry): String {
        val sourceName = sourceName(entry.source)
        val creator = entry.author?.takeIf { it.isNotBlank() }
            ?: entry.artist?.takeIf { it.isNotBlank() }
        return buildString {
            append(sourceName)
            if (creator != null && !creator.equals(sourceName, ignoreCase = true)) {
                append(" • ")
                append(creator)
            }
        }
    }
}

/**
 * [this] stored merge order as the Manage merge sheet lists it: like the episode list, with the merged entry itself, which
 * the source keeps first, staying on top.
 */
private fun List<MergeMember>.inMergeSheetOrder(
    entry: Entry,
    targetId: Long,
): List<MergeMember> {
    if (!entry.showsMergedMembersReversed) return this
    val (target, others) = partition { it.id == targetId }
    return target + others.asReversed()
}

private fun Map<Long, EntryMergeEditorEntryReference>.referencesFor(
    entryIds: Collection<Long>,
): Set<EntryMergeEditorEntryReference> {
    val ids = entryIds.toSet()
    return filterKeys { it in ids }.values.toSet()
}

private fun List<MergeMember>.referencesFor(
    entryIds: Collection<Long>,
): Set<EntryMergeEditorEntryReference> {
    val ids = entryIds.toSet()
    return filter { it.id in ids }.mapTo(linkedSetOf(), MergeMember::reference)
}

private fun Set<Long>.toggled(id: Long): Set<Long> = if (id in this) this - id else this + id

private fun List<Long>.toggled(id: Long): List<Long> = if (id in this) this - id else this + id
