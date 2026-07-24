package mihon.entry.interactions.host.lifecycle.profile

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import mihon.entry.interactions.EntryProfileMoveCommit
import mihon.entry.interactions.EntryProfileMoveConflict
import mihon.entry.interactions.EntryProfileMoveHost
import mihon.entry.interactions.EntryProfileMovePlan
import mihon.entry.interactions.EntryProfileMovePreview
import mihon.entry.interactions.EntryProfileMoveRequest
import tachiyomi.data.Database
import tachiyomi.data.DatabaseHandler
import tachiyomi.data.entry.EntryMapper
import tachiyomi.domain.entry.model.Entry

class AppEntryProfileMoveHost(
    private val handler: DatabaseHandler,
) : EntryProfileMoveHost {
    override suspend fun selectedEntries(request: EntryProfileMoveRequest): List<Entry> {
        return handler.await {
            validateDestination(request)
            request.selectedVisibleEntryIds.distinct().mapNotNull { entryId ->
                entriesQueries.getEntryById(entryId, request.sourceProfileId, EntryMapper::mapEntry).awaitAsOneOrNull()
            }
        }
    }

    override suspend fun destinationConflicts(
        request: EntryProfileMoveRequest,
        sourceEntries: List<Entry>,
    ): List<EntryProfileMoveConflict> {
        return handler.await {
            validateDestination(request)
            sourceEntries.mapNotNull { sourceEntry ->
                val destination = findDestinationEntry(request.destinationProfileId, sourceEntry)
                    ?: return@mapNotNull null
                EntryProfileMoveConflict(sourceEntry, destination, destinationMergeAffected = false)
            }
        }
    }

    override suspend fun execute(
        preview: EntryProfileMovePreview,
        plan: EntryProfileMovePlan,
        beforeCoreMutation: suspend () -> Unit,
        afterCoreMutation: suspend () -> Unit,
    ): EntryProfileMoveCommit {
        return handler.await(inTransaction = true) {
            validateDestination(preview.request)
            if (!revalidate(preview)) return@await EntryProfileMoveCommit.Conflict

            beforeCoreMutation()
            val conflicts = preview.conflicts.associateBy { it.sourceEntry.id }
            plan.mappings.forEach { mapping ->
                val source = mapping.sourceEntry
                val conflict = conflicts[source.id]
                when {
                    mapping.destinationEntryId != source.id -> {
                        entriesQueries.deleteById(plan.sourceProfileId, source.id)
                        assignDestinationCategory(plan, mapping.destinationEntryId)
                        entriesQueries.setFavoriteForProfile(
                            true,
                            plan.destinationProfileId,
                            mapping.destinationEntryId,
                        )
                    }
                    else -> {
                        conflict?.let { duplicate ->
                            entriesQueries.deleteById(plan.destinationProfileId, duplicate.destinationEntry.id)
                        }
                        moveEntry(plan, source.id)
                    }
                }
            }
            afterCoreMutation()
            EntryProfileMoveCommit.Applied
        }
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

    private suspend fun Database.revalidate(preview: EntryProfileMovePreview): Boolean {
        val conflicts = preview.conflicts.associateBy { it.sourceEntry.id }
        return preview.groups.flatMap { it.entries }.all { expected ->
            val source = entriesQueries.getEntryById(
                expected.id,
                preview.request.sourceProfileId,
                EntryMapper::mapEntry,
            ).awaitAsOneOrNull() ?: return@all false
            source.sameProfileMoveIdentity(expected) &&
                findDestinationEntry(preview.request.destinationProfileId, source)?.id ==
                conflicts[source.id]?.destinationEntry?.id
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

    private suspend fun Database.moveEntry(plan: EntryProfileMovePlan, entryId: Long) {
        entries_categoriesQueries.deleteByEntryId(plan.sourceProfileId, entryId)
        entriesQueries.moveToProfile(plan.destinationProfileId, plan.sourceProfileId, entryId)
        assignDestinationCategory(plan, entryId)
    }

    private suspend fun Database.assignDestinationCategory(plan: EntryProfileMovePlan, entryId: Long) {
        entries_categoriesQueries.deleteByEntryId(plan.destinationProfileId, entryId)
        plan.destinationCategoryId?.let { categoryId ->
            entries_categoriesQueries.insert(plan.destinationProfileId, entryId, categoryId)
        }
    }
}

private fun Entry.sameProfileMoveIdentity(other: Entry): Boolean {
    return source == other.source && url == other.url && type == other.type
}
