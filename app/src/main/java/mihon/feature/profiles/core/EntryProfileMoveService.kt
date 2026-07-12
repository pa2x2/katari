package mihon.feature.profiles.core

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import eu.kanade.domain.source.service.SourcePreferences
import tachiyomi.data.Database
import tachiyomi.data.DatabaseHandler
import tachiyomi.data.entry.EntryMapper
import tachiyomi.domain.entry.model.Entry

data class EntryProfileMoveRequest(
    val sourceProfileId: Long,
    val destinationProfileId: Long,
    val destinationCategoryId: Long?,
    val selectedVisibleEntryIds: List<Long>,
)

data class EntryProfileMoveGroup(
    val targetEntryId: Long?,
    val entries: List<Entry>,
)

data class EntryProfileMoveConflict(
    val sourceEntry: Entry,
    val destinationEntry: Entry,
    val groupTargetEntryId: Long?,
    val destinationMergeAffected: Boolean,
)

data class EntryProfileMovePreview(
    val request: EntryProfileMoveRequest,
    val groups: List<EntryProfileMoveGroup>,
    val conflicts: List<EntryProfileMoveConflict>,
)

enum class EntryProfileMoveConflictResolution {
    KEEP_SOURCE,
    OVERWRITE_DESTINATION,
    KEEP_DESTINATION_REMOVE_SOURCE,
}

internal fun Entry.sameProfileMoveIdentity(other: Entry): Boolean {
    return source == other.source && url == other.url && type == other.type
}

internal fun EntryProfileMoveGroup.shouldSkip(
    conflicts: List<EntryProfileMoveConflict>,
    resolutions: Map<Long, EntryProfileMoveConflictResolution>,
): Boolean {
    val entryIds = entries.map(Entry::id).toSet()
    return conflicts.any {
        it.sourceEntry.id in entryIds &&
            resolutions[it.sourceEntry.id] == EntryProfileMoveConflictResolution.KEEP_SOURCE
    }
}

data class EntryProfileMoveResult(
    val movedSelectedItemCount: Int,
    val skippedSelectedItemCount: Int,
    val overwrittenDuplicateCount: Int,
    val removedSourceDuplicateCount: Int,
)

class EntryProfileMoveService(
    private val handler: DatabaseHandler,
    private val profileStore: ProfileStore,
) {

    suspend fun preview(request: EntryProfileMoveRequest): EntryProfileMovePreview {
        require(request.sourceProfileId != request.destinationProfileId)
        require(request.selectedVisibleEntryIds.isNotEmpty())

        return handler.await {
            validateDestination(request)
            val groups = resolveGroups(request)
            require(groups.isNotEmpty()) { "No selected entries still belong to the source profile" }

            val conflicts = groups.flatMap { group ->
                group.entries.mapNotNull { sourceEntry ->
                    val destinationEntry = findDestinationEntry(request.destinationProfileId, sourceEntry)
                        ?: return@mapNotNull null
                    EntryProfileMoveConflict(
                        sourceEntry = sourceEntry,
                        destinationEntry = destinationEntry,
                        groupTargetEntryId = group.targetEntryId,
                        destinationMergeAffected = merged_entriesQueries
                            .getEntriesByEntryId(request.destinationProfileId, destinationEntry.id)
                            .awaitAsList()
                            .isNotEmpty(),
                    )
                }
            }

            EntryProfileMovePreview(request, groups, conflicts)
        }
    }

    suspend fun execute(
        preview: EntryProfileMovePreview,
        resolutions: Map<Long, EntryProfileMoveConflictResolution>,
    ): EntryProfileMoveResult {
        val conflictIds = preview.conflicts.map { it.sourceEntry.id }.toSet()
        require(resolutions.keys.containsAll(conflictIds)) { "Every conflict must be resolved" }

        val (result, movedSourceIds) = handler.await(inTransaction = true) {
            val request = preview.request
            validateDestination(request)
            revalidatePreview(preview)

            var movedItems = 0
            var skippedItems = 0
            var overwritten = 0
            var removedSource = 0
            val movedSourceIds = mutableSetOf<Long>()

            preview.groups.forEach { group ->
                val groupConflicts = preview.conflicts.filter { conflict ->
                    conflict.sourceEntry.id in group.entries.map(Entry::id)
                }
                if (group.shouldSkip(groupConflicts, resolutions)) {
                    skippedItems++
                    return@forEach
                }

                group.targetEntryId?.let { targetId ->
                    merged_entriesQueries.deleteGroupsContainingEntry(request.sourceProfileId, targetId)
                }

                val destinationIdsBySourceId = mutableMapOf<Long, Long>()
                group.entries.forEach { sourceEntry ->
                    val conflict = groupConflicts.firstOrNull { it.sourceEntry.id == sourceEntry.id }
                    when (conflict?.let { resolutions[it.sourceEntry.id] }) {
                        EntryProfileMoveConflictResolution.OVERWRITE_DESTINATION -> {
                            merged_entriesQueries.deleteGroupsContainingEntry(
                                request.destinationProfileId,
                                conflict.destinationEntry.id,
                            )
                            entriesQueries.deleteById(request.destinationProfileId, conflict.destinationEntry.id)
                            moveEntry(request, sourceEntry.id)
                            destinationIdsBySourceId[sourceEntry.id] = sourceEntry.id
                            overwritten++
                        }
                        EntryProfileMoveConflictResolution.KEEP_DESTINATION_REMOVE_SOURCE -> {
                            val destinationId = conflict.destinationEntry.id
                            merged_entriesQueries.deleteGroupsContainingEntry(
                                request.destinationProfileId,
                                destinationId,
                            )
                            entriesQueries.deleteById(request.sourceProfileId, sourceEntry.id)
                            assignDestinationCategory(request, destinationId)
                            entriesQueries.setFavoriteForProfile(true, request.destinationProfileId, destinationId)
                            destinationIdsBySourceId[sourceEntry.id] = destinationId
                            removedSource++
                        }
                        EntryProfileMoveConflictResolution.KEEP_SOURCE -> error(
                            "Skipped groups are handled before mutation",
                        )
                        null -> {
                            moveEntry(request, sourceEntry.id)
                            destinationIdsBySourceId[sourceEntry.id] = sourceEntry.id
                        }
                    }
                }

                if (group.targetEntryId != null && group.entries.size > 1) {
                    val destinationTargetId = destinationIdsBySourceId.getValue(group.targetEntryId)
                    group.entries.forEachIndexed { index, entry ->
                        merged_entriesQueries.insert(
                            request.destinationProfileId,
                            destinationTargetId,
                            destinationIdsBySourceId.getValue(entry.id),
                            index.toLong(),
                        )
                    }
                }
                movedSourceIds += group.entries.map(Entry::source)
                movedItems++
            }

            EntryProfileMoveResult(
                movedSelectedItemCount = movedItems,
                skippedSelectedItemCount = skippedItems,
                overwrittenDuplicateCount = overwritten,
                removedSourceDuplicateCount = removedSource,
            ) to movedSourceIds
        }
        makeSourcesVisible(preview.request.destinationProfileId, movedSourceIds)
        return result
    }

    private suspend fun Database.validateDestination(request: EntryProfileMoveRequest) {
        val profile = profilesQueries.getProfileById(request.destinationProfileId).awaitAsOneOrNull()
            ?: error("Destination profile no longer exists")
        require(!profile.is_archived) { "Destination profile is archived" }
        require(request.sourceProfileId != request.destinationProfileId)

        request.destinationCategoryId?.let { categoryId ->
            val category = categoriesQueries.getCategory(categoryId, request.destinationProfileId).awaitAsOneOrNull()
            require(category != null && category.id != 0L) { "Destination category no longer exists" }
        }
    }

    private suspend fun Database.resolveGroups(request: EntryProfileMoveRequest): List<EntryProfileMoveGroup> {
        val groups = linkedMapOf<String, EntryProfileMoveGroup>()
        request.selectedVisibleEntryIds.distinct().forEach { selectedId ->
            val selectedEntry = entriesQueries
                .getEntryById(selectedId, request.sourceProfileId, EntryMapper::mapEntry)
                .awaitAsOneOrNull()
                ?: return@forEach
            val mergeRows = merged_entriesQueries
                .getEntriesByEntryId(request.sourceProfileId, selectedEntry.id)
                .awaitAsList()
            if (mergeRows.isNotEmpty()) {
                val targetId = mergeRows.first().target_entry_id
                val entries = mergeRows.map { row ->
                    entriesQueries.getEntryById(row.entry_id, request.sourceProfileId, EntryMapper::mapEntry)
                        .awaitAsOneOrNull()
                        ?: error("Merged entry ${row.entry_id} no longer exists")
                }
                groups.putIfAbsent("merge:$targetId", EntryProfileMoveGroup(targetId, entries))
            } else {
                groups.putIfAbsent("entry:${selectedEntry.id}", EntryProfileMoveGroup(null, listOf(selectedEntry)))
            }
        }
        return groups.values.toList()
    }

    private suspend fun Database.revalidatePreview(preview: EntryProfileMovePreview) {
        val destinationConflicts = preview.conflicts.associateBy { it.sourceEntry.id }
        preview.groups.flatMap(EntryProfileMoveGroup::entries).forEach { previewEntry ->
            val sourceEntry = entriesQueries
                .getEntryById(previewEntry.id, preview.request.sourceProfileId, EntryMapper::mapEntry)
                .awaitAsOneOrNull()
                ?: error("Selected entry changed before the move")
            require(sourceEntry.sameProfileMoveIdentity(previewEntry)) {
                "Selected entry identity changed before the move"
            }
            val currentDestination = findDestinationEntry(preview.request.destinationProfileId, sourceEntry)
            require(currentDestination?.id == destinationConflicts[sourceEntry.id]?.destinationEntry?.id) {
                "Destination entries changed before the move"
            }
        }
        preview.groups.forEach { group ->
            val currentRows = merged_entriesQueries
                .getEntriesByEntryId(preview.request.sourceProfileId, group.entries.first().id)
                .awaitAsList()
            if (group.targetEntryId == null) {
                require(currentRows.isEmpty()) { "Selected merge group changed before the move" }
            } else {
                require(currentRows.map { it.target_entry_id }.distinct() == listOf(group.targetEntryId)) {
                    "Selected merge target changed before the move"
                }
                require(currentRows.map { it.entry_id } == group.entries.map(Entry::id)) {
                    "Selected merge members changed before the move"
                }
            }
        }
    }

    private suspend fun Database.findDestinationEntry(profileId: Long, sourceEntry: Entry): Entry? {
        return entriesQueries.getEntryByUrlAndSource(
            profileId,
            sourceEntry.url,
            sourceEntry.source,
            sourceEntry.type.name.lowercase(),
            EntryMapper::mapEntry,
        ).awaitAsOneOrNull()
    }

    private suspend fun Database.moveEntry(request: EntryProfileMoveRequest, entryId: Long) {
        entries_categoriesQueries.deleteByEntryId(request.sourceProfileId, entryId)
        entry_syncQueries.moveEntryToProfile(request.destinationProfileId, request.sourceProfileId, entryId)
        excluded_scanlatorsQueries.moveEntryToProfile(request.destinationProfileId, request.sourceProfileId, entryId)
        entry_cover_hashesQueries.moveEntryToProfile(request.destinationProfileId, request.sourceProfileId, entryId)
        entriesQueries.moveToProfile(request.destinationProfileId, request.sourceProfileId, entryId)
        assignDestinationCategory(request, entryId)
    }

    private suspend fun Database.assignDestinationCategory(request: EntryProfileMoveRequest, entryId: Long) {
        entries_categoriesQueries.deleteByEntryId(request.destinationProfileId, entryId)
        request.destinationCategoryId?.let { categoryId ->
            entries_categoriesQueries.insert(request.destinationProfileId, entryId, categoryId)
        }
    }

    private fun makeSourcesVisible(profileId: Long, sourceIds: Set<Long>) {
        if (sourceIds.isEmpty()) return
        val hiddenSources = profileStore.profileStore(profileId)
            .getStringSet(SourcePreferences.HIDDEN_SOURCES_KEY, emptySet())
        val updatedHiddenSources = hiddenSourcesAfterMove(hiddenSources.get(), sourceIds)
        if (updatedHiddenSources != hiddenSources.get()) {
            hiddenSources.set(updatedHiddenSources)
        }
    }
}

internal fun hiddenSourcesAfterMove(hiddenSources: Set<String>, movedSourceIds: Set<Long>): Set<String> {
    return hiddenSources - movedSourceIds.mapTo(mutableSetOf(), Long::toString)
}
