package mihon.entry.interactions

import kotlinx.coroutines.CancellationException
import mihon.feature.graph.FeatureAfterCommitVolatileExecutionScope
import mihon.feature.graph.FeatureCommitExecutionResult
import mihon.feature.graph.FeatureExecutionResult
import mihon.feature.graph.FeatureExecutionRuntime
import mihon.feature.graph.FeatureTransactionalExecutionScope
import mihon.feature.graph.coordinateFeatureCommit
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.entry.model.Entry

internal class EntryLibraryMembershipCoordinator(
    private val host: EntryLibraryMembershipHost,
    private val mergeCandidates: EntryMergeCandidateFeature,
    private val executions: FeatureExecutionRuntime,
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
                val execution = executions.coordinateFeatureCommit(
                    commit = {
                        host.add(
                            entry = entry,
                            categoryIds = categoryIds,
                            defaultChildFlags = preparation.defaultChildFlags,
                        )
                    },
                    committed = { it is EntryLibraryMembershipCommit.Applied },
                    volatileConsequences = { commit ->
                        val added = (commit as EntryLibraryMembershipCommit.Applied).entries.single()
                        execute(
                            point = ENTRY_LIBRARY_ADDED_EXECUTION_POINT,
                            contentType = added.type.toContentTypeId(),
                            event = EntryLibraryAddedEvent(added),
                        )
                    },
                )
            ) {
                is FeatureCommitExecutionResult.Committed -> {
                    val added = (execution.commit as EntryLibraryMembershipCommit.Applied).entries.single()
                    EntryLibraryAddResult.Added(added, execution.volatileConsequences.toConsequenceFailures())
                }
                is FeatureCommitExecutionResult.NotCommitted -> when (execution.commit) {
                    EntryLibraryMembershipCommit.NoChange -> EntryLibraryAddResult.AlreadyInLibrary(entry)
                    EntryLibraryMembershipCommit.Conflict -> EntryLibraryAddResult.Failed(
                        entry,
                        EntryLibraryMembershipConflict("Entry changed while adding it to the Library"),
                    )
                    is EntryLibraryMembershipCommit.Applied -> error("Applied Library addition was not committed")
                }
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
            when (
                val execution = executions.coordinateFeatureCommit(
                    commit = {
                        host.remove(
                            entries = requested,
                            beforeCommit = callback { persisted -> executeRemoving(persisted) },
                        )
                    },
                    committed = { it is EntryLibraryMembershipCommit.Applied },
                    volatileConsequences = { commit ->
                        executeRemoved((commit as EntryLibraryMembershipCommit.Applied).entries)
                    },
                )
            ) {
                is FeatureCommitExecutionResult.Committed -> execution.volatileConsequences
                is FeatureCommitExecutionResult.NotCommitted -> when (execution.commit) {
                    EntryLibraryMembershipCommit.NoChange -> EntryLibraryRemovalResult.NoChange
                    EntryLibraryMembershipCommit.Conflict -> EntryLibraryRemovalResult.Failed(
                        requested,
                        EntryLibraryMembershipConflict("Entries changed while removing them from the Library"),
                    )
                    is EntryLibraryMembershipCommit.Applied -> error("Applied Library removal was not committed")
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            EntryLibraryRemovalResult.Failed(requested, error)
        }
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

    private suspend fun FeatureTransactionalExecutionScope.executeRemoving(entries: List<Entry>) {
        entries.groupBy(Entry::type).forEach { (type, typedEntries) ->
            val result = execute(
                point = ENTRY_LIBRARY_REMOVING_EXECUTION_POINT,
                contentType = type.toContentTypeId(),
                event = EntryLibraryRemovingEvent(typedEntries),
            )
            if (!result.isSuccessful) throw EntryLibraryTransactionalConsequenceFailure(result)
        }
    }

    private suspend fun FeatureAfterCommitVolatileExecutionScope.executeRemoved(
        entries: List<Entry>,
    ): EntryLibraryRemovalResult.Removed {
        val downloads = linkedMapOf<Pair<Long, Long>, Entry>()
        val failures = mutableListOf<EntryLibraryConsequenceFailure>()
        val sink = EntryLibraryRemovalOutcomeSink { entry -> downloads[entry.profileId to entry.id] = entry }
        entries.groupBy(Entry::type).forEach { (type, typedEntries) ->
            val result = execute(
                point = ENTRY_LIBRARY_REMOVED_EXECUTION_POINT,
                contentType = type.toContentTypeId(),
                event = EntryLibraryRemovedEvent(typedEntries, sink),
            )
            failures += result.toConsequenceFailures()
        }
        return EntryLibraryRemovalResult.Removed(entries, downloads.values.toList(), failures)
    }
}

private fun FeatureExecutionResult.toConsequenceFailures(): List<EntryLibraryConsequenceFailure> {
    return failures.map { failure ->
        EntryLibraryConsequenceFailure(
            participantId = failure.participant.value,
            ownerId = failure.owner.value,
            cause = failure.error,
        )
    }
}

private class EntryLibraryMembershipConflict(message: String) : IllegalStateException(message)

private class EntryLibraryTransactionalConsequenceFailure(
    result: FeatureExecutionResult,
) : IllegalStateException(
    "Transactional Library removal participant failed: " +
        result.failures.joinToString { "${it.participant.value} (${it.error.message})" },
)
