package mihon.entry.interactions.host

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tachiyomi.data.Database
import tachiyomi.data.DatabaseHandler
import tachiyomi.data.entry.EntryMapper
import tachiyomi.domain.entry.model.DuplicateEntryCandidate
import tachiyomi.domain.entry.model.Entry

internal class AppEntryMergeHost(
    private val handler: DatabaseHandler,
    private val duplicateCandidates: AppEntryMergeDuplicateCandidateHost,
    private val defaultChildFlags: () -> Long,
    private val clockMillis: () -> Long = System::currentTimeMillis,
) : EntryMergeHost {
    override fun profile(profileId: Long): EntryMergeProfileHost = ProfileHost(profileId)

    override suspend fun resolveLegacyNotificationEntry(entryId: Long): Entry? {
        return handler.awaitOneOrNull {
            entriesQueries.getEntryByIdAcrossProfiles(entryId, EntryMapper::mapEntry)
        }
    }

    override suspend fun pendingConsequences(limit: Int): List<EntryMergePendingConsequence> {
        require(limit > 0) { "Merge consequence batch size must be positive" }
        return handler.awaitList {
            merge_consequencesQueries.pending(clockMillis(), limit.toLong()) {
                    id,
                    operationId,
                    profileId,
                    entryId,
                    participantId,
                    schemaVersion,
                    payload,
                    attempts,
                ->
                EntryMergePendingConsequence(
                    id = id,
                    operationId = operationId,
                    profileId = profileId,
                    entryId = entryId,
                    participantId = participantId,
                    schemaVersion = schemaVersion.toInt(),
                    payload = payload,
                    attempts = attempts,
                )
            }
        }
    }

    override suspend fun acknowledgeConsequence(consequenceId: String) {
        handler.await { merge_consequencesQueries.acknowledge(consequenceId) }
    }

    override suspend fun recordConsequenceFailure(
        consequenceId: String,
        message: String,
        retryAtMillis: Long,
    ) {
        handler.await {
            merge_consequencesQueries.recordFailure(retryAtMillis, message.take(MAX_ERROR_LENGTH), consequenceId)
        }
    }

    override suspend fun pendingConsequenceCount(operationId: String): Long {
        return handler.awaitOne { merge_consequencesQueries.countByOperation(operationId) }
    }

    override fun observeConsequenceStatus(): Flow<EntryMergeConsequenceStatusSnapshot> {
        return handler.subscribeToOne {
            merge_consequencesQueries.consequenceStatus { pendingCount, failedCount, lastFailure ->
                EntryMergeConsequenceStatusSnapshot(pendingCount, failedCount, lastFailure)
            }
        }
    }

    override suspend fun makeConsequencesRetryable() {
        handler.await { merge_consequencesQueries.makeRetryable() }
    }

    private inner class ProfileHost(
        override val profileId: Long,
    ) : EntryMergeProfileHost {
        override suspend fun entries(entryIds: List<Long>): List<Entry> {
            if (entryIds.isEmpty()) return emptyList()
            val entriesById = handler.await {
                entryIds.distinct().mapNotNull { entryId -> loadEntry(profileId, entryId) }.associateBy(Entry::id)
            }
            return entryIds.mapNotNull(entriesById::get)
        }

        override suspend fun resolveEntryIdentity(entry: Entry): Entry? {
            require(entry.profileId == profileId) { "Merge Entry identity belongs to another profile" }
            return handler.awaitOneOrNull {
                entriesQueries.getEntryByUrlAndSource(
                    profileId,
                    entry.url,
                    entry.source,
                    entry.type.name.lowercase(),
                    EntryMapper::mapEntry,
                )
            }
        }

        override suspend fun membership(entryId: Long): EntryMergeMembershipSnapshot? {
            return handler.await { loadMembership(profileId, entryId) }
        }

        override fun observeMembership(entryId: Long): Flow<EntryMergeMembershipSnapshot?> {
            return handler.subscribeToList {
                merged_entriesQueries.getEntriesByEntryId(profileId, entryId) { targetId, memberId, _ ->
                    targetId to memberId
                }
            }.map { rows -> rows.toMembership(profileId) }
        }

        override suspend fun memberships(): List<EntryMergeMembershipSnapshot> {
            return handler.await { loadMemberships(profileId) }
        }

        override fun observeMemberships(): Flow<List<EntryMergeMembershipSnapshot>> {
            return handler.subscribeToList {
                merged_entriesQueries.getAll(profileId) { targetId, entryId, _ -> targetId to entryId }
            }.map { rows -> rows.toMemberships(profileId) }
        }

        override suspend fun duplicateCandidates(entry: Entry): List<DuplicateEntryCandidate> {
            require(entry.profileId == profileId) { "Merge candidate lookup cannot cross profiles" }
            return duplicateCandidates.candidates(profileId, entry, memberships())
        }

        override fun observeDuplicateCandidates(entry: Flow<Entry>): Flow<List<DuplicateEntryCandidate>> {
            return duplicateCandidates.observeCandidates(profileId, entry, observeMemberships())
        }

        override suspend fun applyTransition(
            transition: EntryMergeHostTransition,
        ): EntryMergeHostTransitionResult {
            require(transition.profileId == profileId) { "Merge transition belongs to another profile" }
            return try {
                handler.await(inTransaction = true) {
                    when (transition) {
                        is EntryMergeHostTransition.CommitEditor -> commitEditor(transition)
                        is EntryMergeHostTransition.ChangeExistingGroup -> changeExistingGroup(transition)
                        is EntryMergeHostTransition.ReplaceForMigration -> replaceForMigration(transition)
                        is EntryMergeHostTransition.RestoreBackupGroup -> restoreBackupGroup(transition)
                    }
                }
            } catch (_: MergeTransitionConflict) {
                EntryMergeHostTransitionResult.Conflict
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                EntryMergeHostTransitionResult.OperationalFailure(retryable = error !is IllegalArgumentException)
            }
        }

        override suspend fun beginProfileMove(
            transition: EntryMergeProfileMoveHostTransition,
        ): EntryMergeHostTransitionResult {
            require(transition.sourceProfileId == profileId) { "Merge Profile Move source belongs to another profile" }
            return try {
                handler.await {
                    beginProfileMove(transition)
                }
            } catch (_: MergeTransitionConflict) {
                EntryMergeHostTransitionResult.Conflict
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                EntryMergeHostTransitionResult.OperationalFailure(retryable = error !is IllegalArgumentException)
            }
        }

        override suspend fun completeProfileMove(
            transition: EntryMergeProfileMoveHostTransition,
        ): EntryMergeHostTransitionResult {
            require(transition.sourceProfileId == profileId) { "Merge Profile Move source belongs to another profile" }
            return try {
                handler.await { completeProfileMove(transition) }
            } catch (_: MergeTransitionConflict) {
                EntryMergeHostTransitionResult.Conflict
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                EntryMergeHostTransitionResult.OperationalFailure(retryable = error !is IllegalArgumentException)
            }
        }

        private suspend fun Database.commitEditor(
            transition: EntryMergeHostTransition.CommitEditor,
        ): EntryMergeHostTransitionResult.Applied {
            val expectedByKey = transition.expected.entries.associateBy { it.key }
            if (expectedByKey.size != transition.expected.entries.size) conflict()
            if (transition.expected.entries.any { it.entry.type != transition.expected.type }) conflict()
            if (transition.orderedEntries.toSet() != expectedByKey.keys) conflict()
            if (transition.orderedEntries.distinct().size != transition.orderedEntries.size) conflict()
            if (transition.target !in expectedByKey) conflict()
            val preparationsByKey = transition.preparations.groupBy(EntryMergeHostPreparation::key)
            if (preparationsByKey.any { (key, preparations) ->
                    key !in expectedByKey || preparations.size != 1 ||
                        !preparations.single().entry.sameMergeIdentity(expectedByKey.getValue(key).entry)
                }
            ) {
                conflict()
            }
            if (transition.consequenceRequests.any { request ->
                    request.memberKey?.let { it !in expectedByKey } == true
                }
            ) {
                conflict()
            }

            transition.expected.entries.forEach { expected -> revalidate(expected) }
            val resolvedIds = transition.expected.entries.associate { expected ->
                expected.key to resolveExpectedEntry(expected, transition.preparations)
            }
            if (transition.consequenceRequests.any { request ->
                    request.entryId?.let { it !in resolvedIds.values } == true
                }
            ) {
                conflict()
            }
            val removed = transition.removedEntries + transition.libraryRemovalEntries
            if (transition.target in removed || removed.any { it !in expectedByKey }) conflict()
            val finalIds = transition.orderedEntries.filterNot(removed::contains).map(resolvedIds::getValue)
            if (finalIds.distinct().size != finalIds.size) conflict()
            validatePersistedGroup(finalIds)

            transition.expected.entries.mapNotNull { it.membership }
                .distinctBy(EntryMergeMembershipSnapshot::targetEntryId)
                .forEach { expected -> merged_entriesQueries.deleteByTargetId(profileId, expected.targetEntryId) }
            val targetId = resolvedIds.getValue(transition.target)
            val visibleEntryId = if (finalIds.size > 1) {
                finalIds.forEachIndexed { index, entryId ->
                    merged_entriesQueries.insert(profileId, targetId, entryId, index.toLong())
                }
                targetId
            } else {
                finalIds.singleOrNull()
            }
            transition.libraryRemovalEntries.forEach { key ->
                entriesQueries.setFavoriteForProfile(false, profileId, resolvedIds.getValue(key))
            }
            insertConsequences(transition, resolvedIds)
            return EntryMergeHostTransitionResult.Applied(visibleEntryId)
        }

        private suspend fun Database.revalidate(expected: EntryMergeHostExpectedEntry) {
            val persistedId = expected.persistedEntryId
            if (persistedId == null) {
                if (loadByIdentity(profileId, expected.entry) != null) conflict()
                if (expected.membership != null) conflict()
                return
            }
            val actual = loadEntry(profileId, persistedId) ?: conflict()
            if (!actual.sameMergeIdentity(expected.entry) || actual.type != expected.entry.type) conflict()
            if (loadMembership(profileId, persistedId) != expected.membership) conflict()
        }

        private suspend fun Database.resolveExpectedEntry(
            expected: EntryMergeHostExpectedEntry,
            preparations: List<EntryMergeHostPreparation>,
        ): Long {
            val preparation = preparations.singleOrNull { it.key == expected.key }
            val persistedId = expected.persistedEntryId
            if (preparation == null) return persistedId ?: conflict()
            if (!preparation.entry.sameMergeIdentity(expected.entry)) conflict()
            val persisted = loadByIdentity(profileId, preparation.entry) ?: materialize(preparation.entry)
            entriesQueries.prepareForLibrary(
                dateAdded = clockMillis(),
                chapterFlags = defaultChildFlags(),
                profileId = profileId,
                entryId = persisted.id,
            )
            entries_categoriesQueries.deleteByEntryId(profileId, persisted.id)
            preparation.categoryIds.forEach { categoryId ->
                val category = categoriesQueries.getCategory(categoryId, profileId).awaitAsOneOrNull() ?: conflict()
                if (category.id != categoryId) conflict()
                entries_categoriesQueries.insert(profileId, persisted.id, categoryId)
            }
            return persisted.id
        }

        private suspend fun Database.materialize(entry: Entry): Entry {
            return entriesQueries.insertNetworkEntry(
                profileId = profileId,
                source = entry.source,
                url = entry.url,
                title = entry.title,
                artist = entry.artist,
                author = entry.author,
                description = entry.description,
                genre = entry.genre,
                status = entry.status.value.toLong(),
                thumbnailUrl = entry.thumbnailUrl,
                favorite = entry.favorite,
                lastUpdate = entry.lastUpdate,
                nextUpdate = entry.nextUpdate,
                initialized = entry.initialized,
                viewerFlags = entry.viewerFlags,
                chapterFlags = entry.chapterFlags,
                coverLastModified = entry.coverLastModified,
                dateAdded = entry.dateAdded,
                updateStrategy = entry.updateStrategy,
                calculateInterval = entry.fetchInterval.toLong(),
                version = entry.version,
                memo = entry.memo,
                type = entry.type.name.lowercase(),
                updateTitle = entry.title.isNotBlank(),
                updateCover = !entry.thumbnailUrl.isNullOrBlank(),
                updateDetails = entry.initialized,
            ).awaitAsOne().let(EntryMapper::mapEntry)
        }

        private suspend fun Database.changeExistingGroup(
            transition: EntryMergeHostTransition.ChangeExistingGroup,
        ): EntryMergeHostTransitionResult.Applied {
            if (loadMembership(profileId, transition.expected.targetEntryId) != transition.expected) conflict()
            if (transition.replacementTargetEntryId == null) {
                if (transition.replacementOrderedEntryIds.isNotEmpty()) conflict()
            } else {
                if (transition.replacementOrderedEntryIds.size < 2) conflict()
                if (transition.replacementTargetEntryId !in transition.replacementOrderedEntryIds) conflict()
                if (transition.replacementOrderedEntryIds.distinct().size !=
                    transition.replacementOrderedEntryIds.size
                ) {
                    conflict()
                }
                if (!transition.expected.orderedEntryIds.containsAll(transition.replacementOrderedEntryIds)) conflict()
                validatePersistedGroup(transition.replacementOrderedEntryIds)
            }
            if (!transition.expected.orderedEntryIds.containsAll(transition.libraryRemovalEntryIds)) conflict()
            if (transition.replacementOrderedEntryIds.any { it in transition.libraryRemovalEntryIds }) conflict()
            if (transition.consequenceRequests.any { request ->
                    request.memberKey != null || request.entryId !in transition.expected.orderedEntryIds
                }
            ) {
                conflict()
            }
            val visibleEntryId = transition.visibleEntryId
            if (
                visibleEntryId != null &&
                (
                    visibleEntryId !in transition.expected.orderedEntryIds ||
                        visibleEntryId in transition.libraryRemovalEntryIds ||
                        loadEntry(profileId, visibleEntryId) == null
                    )
            ) {
                conflict()
            }

            merged_entriesQueries.deleteByTargetId(profileId, transition.expected.targetEntryId)
            transition.replacementTargetEntryId?.let { targetId ->
                transition.replacementOrderedEntryIds.forEachIndexed { index, entryId ->
                    merged_entriesQueries.insert(profileId, targetId, entryId, index.toLong())
                }
            }
            transition.libraryRemovalEntryIds.forEach { entryId ->
                entriesQueries.setFavoriteForProfile(false, profileId, entryId)
            }
            insertConsequences(transition, emptyMap())
            return EntryMergeHostTransitionResult.Applied(transition.visibleEntryId)
        }

        private suspend fun Database.replaceForMigration(
            transition: EntryMergeHostTransition.ReplaceForMigration,
        ): EntryMergeHostTransitionResult.Applied {
            val expectedTargets = transition.expectedGroups.mapTo(mutableSetOf()) { it.targetEntryId }
            if (expectedTargets.size != transition.expectedGroups.size) conflict()
            transition.expectedGroups.forEach { expected ->
                if (loadMembership(profileId, expected.targetEntryId) != expected) conflict()
            }
            val current = loadEntry(profileId, transition.currentEntryId) ?: conflict()
            val replacement = loadEntry(profileId, transition.replacementEntryId) ?: conflict()
            if (current.type != replacement.type) conflict()
            if (transition.expectedGroups.none { transition.currentEntryId in it.orderedEntryIds }) conflict()
            val replacementMembership = loadMembership(profileId, transition.replacementEntryId)
            if (replacementMembership != null && replacementMembership !in transition.expectedGroups) conflict()
            val replacementIds = transition.replacementGroups.flatMap { it.orderedEntryIds }
            if (replacementIds.distinct().size != replacementIds.size) conflict()
            transition.replacementGroups.forEach { group ->
                if (group.profileId != profileId) conflict()
                validatePersistedGroup(group.orderedEntryIds)
                if (group.orderedEntryIds.any { entryId ->
                        loadEntry(profileId, entryId)?.type != current.type
                    }
                ) {
                    conflict()
                }
                group.orderedEntryIds.forEach { entryId ->
                    val currentMembership = loadMembership(profileId, entryId)
                    if (currentMembership != null && currentMembership.targetEntryId !in expectedTargets) conflict()
                }
            }

            transition.expectedGroups.forEach { expected ->
                merged_entriesQueries.deleteByTargetId(profileId, expected.targetEntryId)
            }
            transition.replacementGroups.forEach { group ->
                group.orderedEntryIds.forEachIndexed { index, entryId ->
                    merged_entriesQueries.insert(profileId, group.targetEntryId, entryId, index.toLong())
                }
            }
            val visible = transition.replacementGroups
                .firstOrNull { transition.replacementEntryId in it.orderedEntryIds }
                ?.targetEntryId
                ?: transition.replacementEntryId
            return EntryMergeHostTransitionResult.Applied(visible)
        }

        private suspend fun Database.restoreBackupGroup(
            transition: EntryMergeHostTransition.RestoreBackupGroup,
        ): EntryMergeHostTransitionResult.Applied {
            if (transition.expectedGroups.map { it.targetEntryId }.distinct().size != transition.expectedGroups.size) {
                conflict()
            }
            transition.expectedGroups.forEach { expected ->
                if (loadMembership(profileId, expected.targetEntryId) != expected) conflict()
            }
            if (transition.orderedEntryIds.size < 2) conflict()
            if (transition.orderedEntryIds.distinct().size != transition.orderedEntryIds.size) conflict()
            if (transition.targetEntryId !in transition.orderedEntryIds) conflict()
            val expectedTargets = transition.expectedGroups.mapTo(mutableSetOf()) { it.targetEntryId }
            transition.orderedEntryIds.forEach { entryId ->
                val membership = loadMembership(profileId, entryId)
                if (membership != null && membership.targetEntryId !in expectedTargets) conflict()
            }
            validatePersistedGroup(transition.orderedEntryIds)

            transition.expectedGroups.forEach { expected ->
                merged_entriesQueries.deleteByTargetId(profileId, expected.targetEntryId)
            }
            transition.orderedEntryIds.forEachIndexed { index, entryId ->
                merged_entriesQueries.insert(profileId, transition.targetEntryId, entryId, index.toLong())
            }
            return EntryMergeHostTransitionResult.Applied(transition.targetEntryId)
        }

        private suspend fun Database.beginProfileMove(
            transition: EntryMergeProfileMoveHostTransition,
        ): EntryMergeHostTransitionResult.Applied {
            if (transition.sourceProfileId == transition.destinationProfileId) conflict()
            val expectedSourceIds = transition.expectedSourceGroups.flatMapTo(mutableSetOf()) { it.orderedEntryIds } +
                transition.expectedStandaloneEntryIds
            if (transition.destinationEntryIdsBySourceEntryId.keys != expectedSourceIds) conflict()
            val sourceEntries = expectedSourceIds.associateWith { entryId ->
                loadEntry(transition.sourceProfileId, entryId) ?: conflict()
            }
            if (transition.expectedSourceEntries.map(Entry::id).toSet() != expectedSourceIds) conflict()
            transition.expectedSourceEntries.forEach { expected ->
                if (!sourceEntries.getValue(expected.id).sameMergeIdentity(expected)) conflict()
            }

            transition.expectedSourceGroups.forEach { expected ->
                if (loadMembership(transition.sourceProfileId, expected.targetEntryId) != expected) conflict()
            }
            transition.expectedStandaloneEntryIds.forEach { entryId ->
                if (loadMembership(transition.sourceProfileId, entryId) != null) conflict()
            }
            transition.expectedDestinationGroups.forEach { expected ->
                if (expected.profileId != transition.destinationProfileId) conflict()
                if (loadMembership(transition.destinationProfileId, expected.targetEntryId) != expected) conflict()
            }
            transition.expectedStandaloneDestinationEntryIds.forEach { entryId ->
                if (loadMembership(transition.destinationProfileId, entryId) != null) conflict()
            }
            val expectedDetachedIds = transition.expectedDestinationGroups
                .flatMapTo(mutableSetOf()) { it.orderedEntryIds } + transition.expectedStandaloneDestinationEntryIds
            if (transition.destinationEntryIdsToDetach.any { it !in expectedDetachedIds }) conflict()

            transition.expectedSourceGroups.forEach { expected ->
                merged_entriesQueries.deleteByTargetId(transition.sourceProfileId, expected.targetEntryId)
            }
            transition.expectedDestinationGroups.forEach { expected ->
                merged_entriesQueries.deleteByTargetId(transition.destinationProfileId, expected.targetEntryId)
            }

            return EntryMergeHostTransitionResult.Applied(null)
        }

        private suspend fun Database.completeProfileMove(
            transition: EntryMergeProfileMoveHostTransition,
        ): EntryMergeHostTransitionResult.Applied {
            val sourceEntries = transition.expectedSourceEntries.associateBy(Entry::id)
            val destinationIds = transition.destinationEntryIdsBySourceEntryId.values.toList()
            if (destinationIds.distinct().size != destinationIds.size) conflict()
            transition.destinationEntryIdsBySourceEntryId.forEach { (sourceEntryId, destinationEntryId) ->
                val destinationEntry = loadEntry(transition.destinationProfileId, destinationEntryId) ?: conflict()
                if (destinationEntry.type != sourceEntries.getValue(sourceEntryId).type) conflict()
            }
            transition.expectedSourceGroups.forEach { sourceGroup ->
                val orderedDestinationIds = sourceGroup.orderedEntryIds.map {
                    transition.destinationEntryIdsBySourceEntryId.getValue(it)
                }
                val destinationTargetId = transition.destinationEntryIdsBySourceEntryId
                    .getValue(sourceGroup.targetEntryId)
                validatePersistedGroup(transition.destinationProfileId, orderedDestinationIds)
                orderedDestinationIds.forEachIndexed { index, entryId ->
                    merged_entriesQueries.insert(
                        transition.destinationProfileId,
                        destinationTargetId,
                        entryId,
                        index.toLong(),
                    )
                }
            }
            return EntryMergeHostTransitionResult.Applied(null)
        }

        private suspend fun Database.validatePersistedGroup(entryIds: List<Long>) {
            if (entryIds.isEmpty()) return
            val entries = entryIds.map { entryId -> loadEntry(profileId, entryId) ?: conflict() }
            if (entries.map(Entry::type).distinct().size != 1) conflict()
        }

        private suspend fun Database.insertConsequences(
            transition: EntryMergeHostTransition,
            resolvedIds: Map<EntryMergeHostMemberKey, Long>,
        ) {
            val requests = when (transition) {
                is EntryMergeHostTransition.CommitEditor -> transition.consequenceRequests
                is EntryMergeHostTransition.ChangeExistingGroup -> transition.consequenceRequests
                is EntryMergeHostTransition.ReplaceForMigration,
                is EntryMergeHostTransition.RestoreBackupGroup,
                -> emptyList()
            }
            requests.distinctBy { request ->
                val entryId = request.entryId ?: resolvedIds.getValue(checkNotNull(request.memberKey))
                entryId to request.participantId
            }.forEach { request ->
                val entryId = request.entryId ?: resolvedIds.getValue(checkNotNull(request.memberKey))
                val consequenceId = "${transition.operationId}:$entryId:${request.participantId}"
                merge_consequencesQueries.insert(
                    consequenceId = consequenceId,
                    operationId = transition.operationId,
                    profileId = profileId,
                    entryId = entryId,
                    participantId = request.participantId,
                    schemaVersion = request.schemaVersion.toLong(),
                    payload = request.payload,
                    createdAt = clockMillis(),
                )
            }
        }

        private suspend fun Database.validatePersistedGroup(profileId: Long, entryIds: List<Long>) {
            if (entryIds.isEmpty()) return
            val entries = entryIds.map { entryId -> loadEntry(profileId, entryId) ?: conflict() }
            if (entries.map(Entry::type).distinct().size != 1) conflict()
        }
    }

    private companion object {
        const val MAX_ERROR_LENGTH = 2_000
    }
}

private class MergeTransitionConflict : RuntimeException()

private fun conflict(): Nothing = throw MergeTransitionConflict()

private suspend fun Database.loadEntry(profileId: Long, entryId: Long): Entry? {
    return entriesQueries.getEntryById(entryId, profileId, EntryMapper::mapEntry).awaitAsOneOrNull()
}

private suspend fun Database.loadByIdentity(profileId: Long, entry: Entry): Entry? {
    return entriesQueries.getEntryByUrlAndSource(
        profileId,
        entry.url,
        entry.source,
        entry.type.name.lowercase(),
        EntryMapper::mapEntry,
    ).awaitAsOneOrNull()
}

private suspend fun Database.loadMembership(profileId: Long, entryId: Long): EntryMergeMembershipSnapshot? {
    return merged_entriesQueries.getEntriesByEntryId(profileId, entryId) { targetId, memberId, _ ->
        targetId to memberId
    }.awaitAsList().toMembership(profileId)
}

private suspend fun Database.loadMemberships(profileId: Long): List<EntryMergeMembershipSnapshot> {
    return merged_entriesQueries.getAll(profileId) { targetId, entryId, _ -> targetId to entryId }
        .awaitAsList()
        .toMemberships(profileId)
}

private fun List<Pair<Long, Long>>.toMembership(profileId: Long): EntryMergeMembershipSnapshot? {
    if (isEmpty()) return null
    val targetId = first().first
    if (any { it.first != targetId }) conflict()
    return EntryMergeMembershipSnapshot(profileId, targetId, map { it.second })
}

private fun List<Pair<Long, Long>>.toMemberships(profileId: Long): List<EntryMergeMembershipSnapshot> {
    return groupBy { it.first }.values.mapNotNull { it.toMembership(profileId) }
}

private fun Entry.sameMergeIdentity(other: Entry): Boolean {
    return profileId == other.profileId && type == other.type && source == other.source && url == other.url
}
