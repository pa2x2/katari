package mihon.entry.interactions.lifecycle.removal

import kotlinx.coroutines.CancellationException
import mihon.entry.interactions.download.EntryDownloadRemovalPlan
import mihon.entry.interactions.execute
import mihon.entry.interactions.lifecycle.metadata.toLifecycleFailures
import mihon.entry.interactions.lifecycle.removal.host.EntryDestructiveRemovalCommit
import mihon.entry.interactions.lifecycle.removal.host.EntryDestructiveRemovalHost
import mihon.entry.interactions.runtime.toContentTypeId
import mihon.feature.graph.execution.FeatureCommitExecutionResult
import mihon.feature.graph.execution.FeatureExecutionRuntime
import mihon.feature.graph.execution.coordinateFeatureCommit
import tachiyomi.domain.entry.model.Entry

internal class EntryDestructiveRemovalCoordinator(
    private val host: EntryDestructiveRemovalHost,
    private val executions: FeatureExecutionRuntime,
) : EntryDestructiveRemovalFeature {
    override suspend fun remove(entries: List<Entry>): EntryDestructiveRemovalResult {
        val requested = entries.distinctBy { it.profileId to it.id }
        if (requested.isEmpty()) return EntryDestructiveRemovalResult.NoChange
        return try {
            val collected = MutableEntryDestructiveRemovalOutcomes()
            when (
                val execution = executions.coordinateFeatureCommit(
                    commit = {
                        host.remove(
                            requested = requested,
                            beforeDelete = callback { persisted ->
                                persisted.groupBy(Entry::type).forEach { (type, typedEntries) ->
                                    val result = execute(
                                        point = ENTRY_DESTRUCTIVE_REMOVING_EXECUTION_POINT,
                                        contentType = type.toContentTypeId(),
                                        event = EntryDestructiveRemovingEvent(typedEntries, collected),
                                    )
                                    check(result.isSuccessful) {
                                        "Transactional destructive-removal participants failed: " +
                                            result.failures.joinToString { it.participant.value }
                                    }
                                }
                            },
                        )
                    },
                    committed = { it is EntryDestructiveRemovalCommit.Applied },
                    volatileConsequences = { commit ->
                        val entries = (commit as EntryDestructiveRemovalCommit.Applied).entries
                        val failures = entries.groupBy(Entry::type).flatMap { (type, typedEntries) ->
                            execute(
                                point = ENTRY_DESTRUCTIVE_REMOVED_EXECUTION_POINT,
                                contentType = type.toContentTypeId(),
                                event = EntryDestructiveRemovedEvent(typedEntries, collected.snapshot()),
                            ).toLifecycleFailures()
                        }
                        EntryDestructiveRemovalResult.Removed(entries, failures)
                    },
                )
            ) {
                is FeatureCommitExecutionResult.Committed -> execution.volatileConsequences
                is FeatureCommitExecutionResult.NotCommitted -> when (execution.commit) {
                    EntryDestructiveRemovalCommit.NoChange -> EntryDestructiveRemovalResult.NoChange
                    EntryDestructiveRemovalCommit.Conflict -> EntryDestructiveRemovalResult.Failed(
                        requested,
                        IllegalStateException("Entries changed before destructive removal"),
                    )
                    is EntryDestructiveRemovalCommit.Applied -> error("Applied destructive removal was not committed")
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            EntryDestructiveRemovalResult.Failed(requested, error)
        }
    }
}

private class MutableEntryDestructiveRemovalOutcomes : EntryDestructiveRemovalOutcomeSink {
    private val downloadOwners = linkedMapOf<Pair<Long, Long>, Entry>()

    override fun addDownloadPlan(plan: EntryDownloadRemovalPlan) {
        plan.owners.forEach { owner -> downloadOwners[owner.profileId to owner.id] = owner }
    }

    fun snapshot(): EntryDestructiveRemovalOutcomes {
        val owners = downloadOwners.values.toList()
        return EntryDestructiveRemovalOutcomes(
            downloadPlans = if (owners.isEmpty()) {
                emptyList()
            } else {
                listOf(
                    EntryDownloadRemovalPlan(
                        owners,
                    ),
                )
            },
        )
    }
}
