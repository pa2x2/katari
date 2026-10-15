package mihon.entry.interactions.lifecycle.profile

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.download.EntryDownloadMaintenanceFeature
import mihon.entry.interactions.download.maintenance.EntryDownloadRemovalCollector
import mihon.entry.interactions.lifecycle.EntryLifecycleConsequenceFailure
import mihon.entry.interactions.lifecycle.attempt
import mihon.entry.interactions.lifecycle.profile.host.EntryProfileMoveChildGroupFilterStateHost
import mihon.entry.interactions.lifecycle.profile.host.EntryProfileMoveCommit
import mihon.entry.interactions.lifecycle.profile.host.EntryProfileMoveCoverHashStateHost
import mihon.entry.interactions.lifecycle.profile.host.EntryProfileMoveCustomCoverHost
import mihon.entry.interactions.lifecycle.profile.host.EntryProfileMoveHost
import mihon.entry.interactions.lifecycle.profile.host.EntryProfileMoveSourceVisibilityHost
import mihon.entry.interactions.lifecycle.profile.host.EntryProfileMoveTrackingStateHost
import mihon.entry.interactions.merge.EntryMergeProfileMoveDestinationResult
import mihon.entry.interactions.merge.EntryMergeProfileMoveExecutionResult
import mihon.entry.interactions.merge.EntryMergeProfileMoveFeature
import mihon.entry.interactions.merge.EntryMergeProfileMovePreparationResult
import mihon.entry.interactions.merge.EntryMergeProfileMoveReference
import mihon.entry.interactions.merge.lifecycle.toMergeIntent
import tachiyomi.domain.entry.model.Entry

/**
 * Moves entries to another profile. Merge groups move as one unit, so preview expands the selection to whole groups.
 * Inside the move transaction, downloads of replaced entries are collected and merge, child-group filter, cover hash
 * and tracking state follow the entries; after commit, replaced custom covers and downloads are removed and the
 * moved entries' sources become visible in the destination profile.
 */
internal class EntryProfileMoveCoordinator(
    private val host: EntryProfileMoveHost,
    private val mergeProfileMove: () -> EntryMergeProfileMoveFeature,
    private val downloads: () -> EntryDownloadMaintenanceFeature,
    private val childGroupFilterStateHost: EntryProfileMoveChildGroupFilterStateHost,
    private val childGroupFilterTypes: Set<EntryType>,
    private val coverHashStateHost: EntryProfileMoveCoverHashStateHost,
    private val trackingStateHost: EntryProfileMoveTrackingStateHost,
    private val customCoverHost: EntryProfileMoveCustomCoverHost,
    private val sourceVisibilityHost: EntryProfileMoveSourceVisibilityHost,
) : EntryProfileMoveFeature {
    override suspend fun preview(request: EntryProfileMoveRequest): EntryProfileMovePreview {
        require(request.sourceProfileId != request.destinationProfileId)
        require(request.selectedVisibleEntryIds.isNotEmpty())

        val selected = host.selectedEntries(request)
        check(selected.isNotEmpty()) { "No selected entries still belong to the source profile" }
        val preparation = MutableEntryProfileMovePreparation(request, selected)
        val mergeReferences = mutableMapOf<EntryType, EntryMergeProfileMoveReference>()
        selected.groupBy(Entry::type).forEach { (type, typedEntries) ->
            val result = mergeProfileMove().prepare(request.sourceProfileId, typedEntries.map(Entry::id))
            if (result is EntryMergeProfileMovePreparationResult.Ready) {
                result.units.forEach { unit -> preparation.addAtomicUnit(unit.entries) }
                mergeReferences[type] = result.reference
            }
        }

        val groups = preparation.groups()
        val conflicts = host.destinationConflicts(request, groups.flatMap(EntryProfileMoveGroup::entries))
        val mergeAffectedIds = mutableSetOf<Long>()
        groups.flatMap(EntryProfileMoveGroup::entries).map(Entry::type).distinct().forEach { type ->
            val reference = mergeReferences[type] ?: return@forEach
            val result = mergeProfileMove().inspectDestination(
                reference,
                request.destinationProfileId,
                conflicts.filter { it.sourceEntry.type == type }.map { it.destinationEntry.id },
            )
            when (result) {
                is EntryMergeProfileMoveDestinationResult.Ready -> {
                    mergeReferences[type] = result.reference
                    mergeAffectedIds += result.mergeAffectedEntryIds
                }
                EntryMergeProfileMoveDestinationResult.InvalidReference ->
                    error("Merge Profile-move destination changed during preparation")
            }
        }
        return EntryProfileMovePreview(
            request = request,
            reference = RuntimeEntryProfileMoveReference(mergeReferences.toMap()),
            groups = groups,
            conflicts = conflicts.map { conflict ->
                conflict.copy(destinationMergeAffected = conflict.destinationEntry.id in mergeAffectedIds)
            },
        )
    }

    override suspend fun execute(
        preview: EntryProfileMovePreview,
        resolutions: Map<Long, EntryProfileMoveConflictResolution>,
    ): EntryProfileMoveResult {
        val conflictIds = preview.conflicts.map { it.sourceEntry.id }.toSet()
        require(resolutions.keys.containsAll(conflictIds)) { "Every conflict must be resolved" }
        val reference = checkNotNull(preview.reference as? RuntimeEntryProfileMoveReference) {
            "Unknown Profile-move reference"
        }

        val prepared = buildPlan(preview, resolutions)
        val typedPlans = prepared.plan.byType()
        val replacedDownloads = EntryDownloadRemovalCollector(downloads)
        val commit = host.execute(
            preview = preview,
            plan = prepared.plan,
            beforeCoreMutation = {
                typedPlans.forEach { (type, plan) ->
                    replacedDownloads.prepare(plan.removedEntries)
                    reference.merge[type]?.let { mergeProfileMove().begin(plan.toMergeIntent(it)).requireApplied() }
                }
            },
            afterCoreMutation = {
                typedPlans.forEach { (type, plan) ->
                    if (type in childGroupFilterTypes) childGroupFilterStateHost.move(plan.stateRequest())
                    coverHashStateHost.move(plan.stateRequest())
                    reference.merge[type]?.let { mergeProfileMove().complete(plan.toMergeIntent(it)).requireApplied() }
                    trackingStateHost.move(plan.stateRequest())
                }
            },
        )
        check(commit == EntryProfileMoveCommit.Applied) { "Entries changed before Profile movement could commit" }

        val failures = mutableListOf<EntryLifecycleConsequenceFailure>()
        typedPlans.forEach { (type, plan) ->
            failures.attempt("entry.custom-cover.profile-move") {
                customCoverHost.removeCustomCovers(plan.removedEntries)
            }
            failures.attempt("entry.download.maintenance.profile-move") { replacedDownloads.apply(type) }
            failures.attempt("entry.profile-move.source-visibility") {
                sourceVisibilityHost.makeSourcesVisible(
                    plan.destinationProfileId,
                    plan.movedEntries.mapTo(mutableSetOf()) { it.source },
                )
            }
        }
        return prepared.result.copy(consequenceFailures = failures)
    }
}

private fun EntryMergeProfileMoveExecutionResult.requireApplied() {
    check(this == EntryMergeProfileMoveExecutionResult.Applied) { "Merge Profile-move participation failed: $this" }
}

private class MutableEntryProfileMovePreparation(
    private val request: EntryProfileMoveRequest,
    selected: List<Entry>,
) {
    private val entries = linkedMapOf<Pair<Long, Long>, Entry>()
    private val atomicUnits = mutableListOf<Set<Pair<Long, Long>>>()

    init {
        selected.forEach(::accept)
    }

    fun addAtomicUnit(entries: List<Entry>) {
        if (entries.isEmpty()) return
        require(entries.map(Entry::type).distinct().size == 1) { "A Profile-move atomic unit cannot mix Entry types" }
        atomicUnits += entries.mapTo(linkedSetOf()) { entry -> accept(entry) }
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

    private fun accept(entry: Entry): Pair<Long, Long> {
        require(entry.profileId == request.sourceProfileId) { "Profile-move contribution belongs to another profile" }
        val key = entry.profileId to entry.id
        val previous = entries.putIfAbsent(key, entry)
        require(previous == null || previous == entry) { "Profile-move contribution changed Entry identity" }
        return key
    }
}

private data class RuntimeEntryProfileMoveReference(
    val merge: Map<EntryType, EntryMergeProfileMoveReference>,
) : EntryProfileMoveReference

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
