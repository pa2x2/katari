package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.feature.graph.FeatureCommitExecutionResult
import mihon.feature.graph.FeatureExecutionResult
import mihon.feature.graph.FeatureExecutionRuntime
import mihon.feature.graph.FeatureTransactionalExecutionScope
import mihon.feature.graph.InlineFeatureExecutionPointDefinition
import mihon.feature.graph.TransactionalFeatureExecutionPointDefinition
import mihon.feature.graph.coordinateFeatureCommit
import tachiyomi.domain.entry.model.Entry

internal class EntryProfileMoveCoordinator(
    private val host: EntryProfileMoveHost,
    private val executions: FeatureExecutionRuntime,
) : EntryProfileMoveFeature {
    override suspend fun preview(request: EntryProfileMoveRequest): EntryProfileMovePreview {
        require(request.sourceProfileId != request.destinationProfileId)
        require(request.selectedVisibleEntryIds.isNotEmpty())

        val selected = host.selectedEntries(request)
        check(selected.isNotEmpty()) { "No selected entries still belong to the source profile" }
        val contributions = MutableEntryProfileMovePreparation(request, selected)
        selected.groupBy(Entry::type).forEach { (type, typedEntries) ->
            executions.executeInlineOrThrow(
                point = ENTRY_PROFILE_MOVE_PREPARING_EXECUTION_POINT,
                type = type,
                event = EntryProfileMovePreparingEvent(request, typedEntries, contributions),
            )
        }

        val groups = contributions.groups()
        val conflicts = host.destinationConflicts(request, groups.flatMap(EntryProfileMoveGroup::entries))
        groups.flatMap(EntryProfileMoveGroup::entries).map(Entry::type).distinct().forEach { type ->
            executions.executeInlineOrThrow(
                point = ENTRY_PROFILE_MOVE_DESTINATION_INSPECTING_EXECUTION_POINT,
                type = type,
                event = EntryProfileMoveDestinationInspectingEvent(
                    type,
                    request,
                    conflicts.filter { it.sourceEntry.type == type },
                    contributions,
                ),
            )
        }
        return EntryProfileMovePreview(
            request = request,
            reference = contributions.reference(),
            groups = groups,
            conflicts = conflicts.map { conflict ->
                conflict.copy(destinationMergeAffected = conflict.destinationEntry.id in contributions.affectedIds)
            },
        )
    }

    override suspend fun execute(
        preview: EntryProfileMovePreview,
        resolutions: Map<Long, EntryProfileMoveConflictResolution>,
    ): EntryProfileMoveResult {
        val conflictIds = preview.conflicts.map { it.sourceEntry.id }.toSet()
        require(resolutions.keys.containsAll(conflictIds)) { "Every conflict must be resolved" }
        check(preview.reference is RuntimeEntryProfileMoveReference) { "Unknown Profile-move reference" }

        val prepared = buildPlan(preview, resolutions)
        val outcomes = MutableEntryProfileMoveOutcomes()
        val typedPlans = prepared.plan.byType()
        val execution = executions.coordinateFeatureCommit(
            commit = {
                host.execute(
                    preview = preview,
                    plan = prepared.plan,
                    beforeCoreMutation = callback {
                        typedPlans.forEach { (type, plan) ->
                            executeOrThrow(
                                point = ENTRY_PROFILE_MOVING_EXECUTION_POINT,
                                type = type,
                                event = EntryProfileMovingEvent(type, plan, preview.reference, outcomes),
                            )
                        }
                    },
                    afterCoreMutation = callback {
                        typedPlans.forEach { (type, plan) ->
                            executeOrThrow(
                                point = ENTRY_PROFILE_STATE_MOVED_EXECUTION_POINT,
                                type = type,
                                event = EntryProfileStateMovedEvent(type, plan, preview.reference),
                            )
                        }
                    },
                )
            },
            committed = { it == EntryProfileMoveCommit.Applied },
            volatileConsequences = {
                typedPlans.flatMap { (type, plan) ->
                    execute(
                        point = ENTRY_PROFILE_MOVED_EXECUTION_POINT,
                        contentType = type.toContentTypeId(),
                        event = EntryProfileMovedEvent(type, plan, outcomes.snapshot()),
                    ).toLifecycleFailures()
                }
            },
        )
        val failures = when (execution) {
            is FeatureCommitExecutionResult.Committed -> execution.volatileConsequences
            is FeatureCommitExecutionResult.NotCommitted -> when (execution.commit) {
                EntryProfileMoveCommit.Conflict -> error("Entries changed before Profile movement could commit")
                EntryProfileMoveCommit.Applied -> error("Applied Profile movement was not committed")
            }
        }

        return prepared.result.copy(consequenceFailures = failures)
    }
}

private suspend fun <E : Any> FeatureExecutionRuntime.executeInlineOrThrow(
    point: InlineFeatureExecutionPointDefinition<E>,
    type: EntryType,
    event: E,
) {
    val result = executeInline(point, type.toContentTypeId(), event)
    check(result.isSuccessful) { result.failureMessage() }
}

private suspend fun <E : Any> FeatureTransactionalExecutionScope.executeOrThrow(
    point: TransactionalFeatureExecutionPointDefinition<E>,
    type: EntryType,
    event: E,
) {
    val result = execute(point, type.toContentTypeId(), event)
    check(result.isSuccessful) { result.failureMessage() }
}

private fun FeatureExecutionResult.failureMessage(): String {
    return "Profile-move participant failure at ${point.value}: " +
        failures.joinToString { "${it.participant.value} (${it.error.message})" }
}

private class MutableEntryProfileMovePreparation(
    private val request: EntryProfileMoveRequest,
    selected: List<Entry>,
) : EntryProfileMovePreparationSink {
    private val entries = linkedMapOf<Pair<Long, Long>, Entry>()
    private val atomicUnits = mutableListOf<Set<Pair<Long, Long>>>()
    private val references = linkedMapOf<Pair<String, EntryType>, EntryProfileMoveParticipantReference>()
    val affectedIds = mutableSetOf<Long>()

    init {
        selected.forEach(::accept)
    }

    override fun addAtomicUnit(entries: List<Entry>) {
        if (entries.isEmpty()) return
        require(entries.map(Entry::type).distinct().size == 1) { "A Profile-move atomic unit cannot mix Entry types" }
        atomicUnits += entries.mapTo(linkedSetOf()) { entry -> accept(entry) }
    }

    override fun setParticipantReference(
        participantId: String,
        type: EntryType,
        reference: EntryProfileMoveParticipantReference,
    ) {
        references[participantId to type] = reference
    }

    override fun participantReference(
        participantId: String,
        type: EntryType,
    ): EntryProfileMoveParticipantReference? = references[participantId to type]

    override fun markDestinationAffected(entryIds: Set<Long>) {
        affectedIds += entryIds
    }

    fun groups(): List<EntryProfileMoveGroup> {
        val components = mutableListOf<LinkedHashSet<Pair<Long, Long>>>()
        atomicUnits.forEach { unit ->
            val overlaps = components.filter { component -> component.any(unit::contains) }
            val merged = linkedSetOf<Pair<Long, Long>>().apply {
                overlaps.forEach(::addAll)
                addAll(unit)
            }
            components.removeAll(overlaps)
            components += merged
        }
        entries.keys.filterNot { key -> components.any { key in it } }.forEach { key ->
            components += linkedSetOf(key)
        }
        return components.map { component -> EntryProfileMoveGroup(component.map(entries::getValue)) }
    }

    fun reference(): EntryProfileMoveReference = RuntimeEntryProfileMoveReference(references.toMap())

    private fun accept(entry: Entry): Pair<Long, Long> {
        require(entry.profileId == request.sourceProfileId) { "Profile-move contribution belongs to another profile" }
        val key = entry.profileId to entry.id
        val previous = entries.putIfAbsent(key, entry)
        require(previous == null || previous == entry) { "Profile-move contribution changed Entry identity" }
        return key
    }
}

private data class RuntimeEntryProfileMoveReference(
    private val references: Map<Pair<String, EntryType>, EntryProfileMoveParticipantReference>,
) : EntryProfileMoveReference {
    override fun participantReference(
        participantId: String,
        type: EntryType,
    ): EntryProfileMoveParticipantReference? = references[participantId to type]
}

private data class PreparedEntryProfileMove(
    val plan: EntryProfileMovePlan,
    val result: EntryProfileMoveResult,
)

private fun buildPlan(
    preview: EntryProfileMovePreview,
    resolutions: Map<Long, EntryProfileMoveConflictResolution>,
): PreparedEntryProfileMove {
    val conflicts = preview.conflicts.associateBy { it.sourceEntry.id }
    val mappings = mutableListOf<EntryProfileMoveMapping>()
    val moved = mutableListOf<Entry>()
    val removed = mutableListOf<Entry>()
    val detached = mutableSetOf<Long>()
    var movedGroups = 0
    var skippedGroups = 0
    var overwritten = 0
    var removedSources = 0

    preview.groups.forEach { group ->
        val skip = group.entries.any { entry ->
            conflicts[entry.id]?.let { resolutions[it.sourceEntry.id] } ==
                EntryProfileMoveConflictResolution.KEEP_SOURCE
        }
        if (skip) {
            skippedGroups++
            return@forEach
        }
        movedGroups++
        group.entries.forEach { source ->
            val conflict = conflicts[source.id]
            when (conflict?.let { resolutions[source.id] }) {
                EntryProfileMoveConflictResolution.OVERWRITE_DESTINATION -> {
                    mappings += EntryProfileMoveMapping(source, source.id)
                    moved += source
                    removed += conflict.destinationEntry
                    detached += conflict.destinationEntry.id
                    overwritten++
                }
                EntryProfileMoveConflictResolution.KEEP_DESTINATION_REMOVE_SOURCE -> {
                    mappings += EntryProfileMoveMapping(source, conflict.destinationEntry.id)
                    removed += source
                    detached += conflict.destinationEntry.id
                    removedSources++
                }
                EntryProfileMoveConflictResolution.KEEP_SOURCE -> error("Skipped groups cannot enter the move plan")
                null -> {
                    mappings += EntryProfileMoveMapping(source, source.id)
                    moved += source
                }
            }
        }
    }
    return PreparedEntryProfileMove(
        plan = EntryProfileMovePlan(
            sourceProfileId = preview.request.sourceProfileId,
            destinationProfileId = preview.request.destinationProfileId,
            destinationCategoryId = preview.request.destinationCategoryId,
            mappings = mappings,
            movedEntries = moved,
            removedEntries = removed,
            destinationEntryIdsToDetach = detached,
        ),
        result = EntryProfileMoveResult(movedGroups, skippedGroups, overwritten, removedSources),
    )
}

private fun EntryProfileMovePlan.byType(): Map<EntryType, EntryProfileMovePlan> {
    return mappings.groupBy { it.sourceEntry.type }.mapValues { (type, mappings) ->
        val relevantIds = mappings.mapTo(mutableSetOf()) { it.destinationEntryId } +
            removedEntries.filter { it.type == type }.mapTo(mutableSetOf(), Entry::id)
        copy(
            mappings = mappings,
            movedEntries = movedEntries.filter { it.type == type },
            removedEntries = removedEntries.filter { it.type == type },
            destinationEntryIdsToDetach = destinationEntryIdsToDetach.filterTo(mutableSetOf(), relevantIds::contains),
        )
    }
}

private class MutableEntryProfileMoveOutcomes : EntryProfileMoveOutcomeSink {
    private val downloadOwners = linkedMapOf<Pair<Long, Long>, Entry>()

    override fun addDownloadPlan(plan: EntryDownloadRemovalPlan) {
        plan.owners.forEach { owner -> downloadOwners[owner.profileId to owner.id] = owner }
    }

    fun snapshot(): EntryProfileMoveOutcomes {
        val owners = downloadOwners.values.toList()
        return EntryProfileMoveOutcomes(
            downloadPlans = if (owners.isEmpty()) emptyList() else listOf(EntryDownloadRemovalPlan(owners)),
        )
    }
}
