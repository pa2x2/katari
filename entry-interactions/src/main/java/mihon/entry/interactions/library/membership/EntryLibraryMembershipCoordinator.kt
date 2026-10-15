package mihon.entry.interactions.library.membership

import kotlinx.coroutines.CancellationException
import mihon.entry.interactions.download.EntryDownloadMaintenanceFeature
import mihon.entry.interactions.download.EntryDownloadMaintenanceInspection
import mihon.entry.interactions.library.membership.host.EntryLibraryCustomCoverHost
import mihon.entry.interactions.library.membership.host.EntryLibraryMembershipCommit
import mihon.entry.interactions.library.membership.host.EntryLibraryMembershipHost
import mihon.entry.interactions.library.membership.host.EntryLibraryMembershipPreparation
import mihon.entry.interactions.lifecycle.EntryLifecycleConsequenceFailure
import mihon.entry.interactions.lifecycle.attempt
import mihon.entry.interactions.merge.EntryMergeCandidateFeature
import mihon.entry.interactions.merge.EntryMergeLibraryLifecycleFeature
import mihon.entry.interactions.tracking.EntryTrackingFeature
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.entry.model.Entry

/**
 * Adds entries to and removes them from the Library. Merge groups give up removed members inside the removal
 * transaction; tracking, download and custom-cover consequences run after the change commits, and their failures are
 * reported without undoing it.
 */
internal class EntryLibraryMembershipCoordinator(
    private val host: EntryLibraryMembershipHost,
    private val mergeCandidates: EntryMergeCandidateFeature,
    private val mergeLibraryLifecycle: () -> EntryMergeLibraryLifecycleFeature,
    private val tracking: () -> EntryTrackingFeature,
    private val downloads: () -> EntryDownloadMaintenanceFeature,
    private val customCoverHost: EntryLibraryCustomCoverHost,
) : EntryLibraryMembershipFeature {

    override suspend fun add(request: EntryLibraryAddRequest): EntryLibraryAddResult {
        val entry = request.entry
        if (entry.favorite) return EntryLibraryAddResult.AlreadyInLibrary(entry)
        return try {
            if (request.duplicatePolicy == EntryLibraryDuplicatePolicy.CHECK) {
                val candidates = mergeCandidates.candidates(entry)
                if (candidates.isNotEmpty()) {
                    return EntryLibraryAddResult.DuplicateCandidates(entry, candidates)
                }
            }

            val preparation = host.prepareAddition(entry)
            val categoryIds = when (val selection = request.categorySelection) {
                EntryLibraryCategorySelection.ResolveDefault -> resolveDefaultCategories(preparation)
                    ?: return categorySelectionRequired(entry, preparation)
                is EntryLibraryCategorySelection.Selected -> selection.categoryIds.distinct()
            }
            when (
                val commit = host.add(
                    entry = entry,
                    categoryIds = categoryIds,
                    defaultChildFlags = preparation.defaultChildFlags,
                )
            ) {
                is EntryLibraryMembershipCommit.Applied -> {
                    val added = commit.entries.single()
                    val failures = mutableListOf<EntryLifecycleConsequenceFailure>()
                    failures.attempt("entry.tracking.library-addition") { tracking().bindAutomatically(added) }
                    EntryLibraryAddResult.Added(added, failures)
                }
                EntryLibraryMembershipCommit.NoChange -> EntryLibraryAddResult.AlreadyInLibrary(entry)
                EntryLibraryMembershipCommit.Conflict -> EntryLibraryAddResult.Failed(
                    entry,
                    EntryLibraryMembershipConflict("Entry changed while adding it to the Library"),
                )
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            EntryLibraryAddResult.Failed(entry, error)
        }
    }

    override suspend fun remove(entries: List<Entry>): EntryLibraryRemovalResult {
        val requested = entries.distinctBy { it.profileId to it.id }
        if (requested.isEmpty()) return EntryLibraryRemovalResult.NoChange
        return try {
            when (val commit = host.remove(entries = requested, beforeCommit = ::leaveMergeGroups)) {
                is EntryLibraryMembershipCommit.Applied -> removed(commit.entries)
                EntryLibraryMembershipCommit.NoChange -> EntryLibraryRemovalResult.NoChange
                EntryLibraryMembershipCommit.Conflict -> EntryLibraryRemovalResult.Failed(
                    requested,
                    EntryLibraryMembershipConflict("Entries changed while removing them from the Library"),
                )
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            EntryLibraryRemovalResult.Failed(requested, error)
        }
    }

    private suspend fun leaveMergeGroups(entries: List<Entry>) {
        entries.groupBy(Entry::type).values.forEach { typedEntries ->
            val result = mergeLibraryLifecycle().entriesRemovedFromLibrary(typedEntries)
            check(result.unresolvedGroupCount == 0) { "Merge membership changed while removing Library entries" }
        }
    }

    private suspend fun removed(entries: List<Entry>): EntryLibraryRemovalResult.Removed {
        val downloadDecisions = mutableListOf<Entry>()
        val failures = mutableListOf<EntryLifecycleConsequenceFailure>()
        entries.groupBy(Entry::type).values.forEach { typedEntries ->
            failures.attempt("entry.download.maintenance.library-removal") {
                typedEntries.forEach { entry ->
                    if (downloads().inspectEntry(entry) == EntryDownloadMaintenanceInspection.HasDownloads) {
                        downloadDecisions += entry
                    }
                }
            }
            failures.attempt("entry.library-custom-cover.removal") {
                typedEntries.forEach { entry -> customCoverHost.cleanupAfterLibraryRemoval(entry) }
            }
        }
        return EntryLibraryRemovalResult.Removed(
            entries,
            downloadDecisions.distinctBy { it.profileId to it.id },
            failures,
        )
    }

    private fun resolveDefaultCategories(preparation: EntryLibraryMembershipPreparation): List<Long>? {
        val categories = preparation.categories.filterNot(Category::isSystemCategory)
        val defaultCategoryId = preparation.defaultCategoryId
        return when {
            categories.any { it.id == defaultCategoryId } -> listOf(defaultCategoryId)
            defaultCategoryId == 0L || categories.isEmpty() -> emptyList()
            else -> null
        }
    }

    private fun categorySelectionRequired(
        entry: Entry,
        preparation: EntryLibraryMembershipPreparation,
    ): EntryLibraryAddResult.CategorySelectionRequired {
        return EntryLibraryAddResult.CategorySelectionRequired(
            entry,
            preparation.categories.filterNot(Category::isSystemCategory),
            preparation.selectedCategoryIds,
        )
    }
}

private class EntryLibraryMembershipConflict(message: String) : IllegalStateException(message)
