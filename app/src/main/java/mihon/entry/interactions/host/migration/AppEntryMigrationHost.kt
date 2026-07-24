package mihon.entry.interactions.host

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import mihon.entry.interactions.EntryMergeMigrationReplacementResult
import mihon.entry.interactions.EntryMigrationMode
import tachiyomi.data.Database
import tachiyomi.data.DatabaseHandler
import tachiyomi.data.MemoColumnAdapter
import tachiyomi.data.StringListColumnAdapter
import tachiyomi.data.UpdateStrategyColumnAdapter
import tachiyomi.data.entry.EntryMapper
import tachiyomi.data.track.TrackMapper
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.track.model.EntryTrack

private const val MAX_ERROR_LENGTH = 2_000

internal class AppEntryMigrationHost(
    private val handler: DatabaseHandler,
    private val hasCustomCover: (Long) -> Boolean,
    private val clockMillis: () -> Long = System::currentTimeMillis,
) : EntryMigrationPreparationHost, EntryMigrationExecutionHost, EntryMigrationConsequenceHost {
    override fun profile(profileId: Long): ProfileHost = ProfileHost(profileId)

    override suspend fun pendingConsequences(limit: Int): List<EntryMigrationPendingConsequence> {
        require(limit > 0) { "Migration consequence batch size must be positive" }
        return handler.awaitList {
            entry_migration_consequencesQueries.pending(clockMillis(), limit.toLong()) {
                    id,
                    operationId,
                    profileId,
                    participantId,
                    schemaVersion,
                    payload,
                    attempts,
                ->
                EntryMigrationPendingConsequence(
                    id,
                    operationId,
                    profileId,
                    participantId,
                    schemaVersion.toInt(),
                    payload,
                    attempts,
                )
            }
        }
    }

    override suspend fun pendingConsequences(
        operationId: String,
        limit: Int,
    ): List<EntryMigrationPendingConsequence> {
        require(limit > 0) { "Migration consequence batch size must be positive" }
        return handler.awaitList {
            entry_migration_consequencesQueries.pendingByOperation(operationId, clockMillis(), limit.toLong()) {
                    id,
                    persistedOperationId,
                    profileId,
                    participantId,
                    schemaVersion,
                    payload,
                    attempts,
                ->
                EntryMigrationPendingConsequence(
                    id,
                    persistedOperationId,
                    profileId,
                    participantId,
                    schemaVersion.toInt(),
                    payload,
                    attempts,
                )
            }
        }
    }

    override suspend fun acknowledgeConsequence(consequenceId: String) {
        handler.await { entry_migration_consequencesQueries.acknowledge(consequenceId) }
    }

    override suspend fun recordConsequenceFailure(
        consequenceId: String,
        message: String,
        retryAtMillis: Long,
    ) {
        handler.await {
            entry_migration_consequencesQueries.recordFailure(
                retryAtMillis,
                message.take(MAX_ERROR_LENGTH),
                consequenceId,
            )
        }
    }

    override suspend fun pendingConsequenceCount(operationId: String): Long {
        return handler.awaitOne { entry_migration_consequencesQueries.countByOperation(operationId) }
    }

    override suspend fun participantPayloads(participantId: String): List<EntryMigrationPersistedPayload> {
        return handler.awaitList {
            entry_migration_consequencesQueries.payloadsByParticipant(participantId) { schemaVersion, payload ->
                EntryMigrationPersistedPayload(schemaVersion.toInt(), payload)
            }
        }
    }

    override fun observeConsequenceStatus(): Flow<EntryMigrationConsequenceStatusSnapshot> {
        return handler.subscribeToOne {
            entry_migration_consequencesQueries.consequenceStatus { pendingCount, failedCount, lastFailure ->
                EntryMigrationConsequenceStatusSnapshot(pendingCount, failedCount, lastFailure)
            }
        }
    }

    override suspend fun makeConsequencesRetryable() {
        handler.await { entry_migration_consequencesQueries.makeRetryable() }
    }

    internal inner class ProfileHost(
        private val profileId: Long,
    ) : EntryMigrationPreparationProfileHost, EntryMigrationExecutionProfileHost {
        override suspend fun replay(operation: EntryMigrationHostOperation): EntryMigrationHostReplayResult {
            return try {
                handler.await {
                    val persisted = entry_migration_operationsQueries.getById(operation.operationId)
                        .awaitAsOneOrNull()
                        ?: return@await EntryMigrationHostReplayResult.NotApplied
                    if (
                        persisted.intent_fingerprint != operation.intentFingerprint ||
                        persisted.profile_id != profileId ||
                        persisted.source_entry_id != operation.sourceEntryId ||
                        persisted.target_entry_id != operation.targetEntryId ||
                        persisted.mode != operation.mode.name
                    ) {
                        return@await EntryMigrationHostReplayResult.Conflict
                    }
                    val pending = entry_migration_consequencesQueries.countByOperation(operation.operationId)
                        .awaitAsOneOrNull() ?: 0L
                    EntryMigrationHostReplayResult.Applied(pending > 0L)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                EntryMigrationHostReplayResult.OperationalFailure(retryable = true)
            }
        }

        override suspend fun inspectPair(
            sourceEntryId: Long,
            targetEntryId: Long,
        ): EntryMigrationHostInspectionResult {
            return try {
                val inspection = handler.await {
                    val source = loadEntry(profileId, sourceEntryId)
                        ?: return@await EntryMigrationHostInspectionResult.SourceMissing
                    val target = loadEntry(profileId, targetEntryId)
                        ?: return@await EntryMigrationHostInspectionResult.TargetMissing
                    EntryMigrationHostInspectionResult.Ready(
                        source = source,
                        target = target,
                        sourceCategoryIds = loadCategoryIds(profileId, sourceEntryId),
                        sourceHasCustomCover = false,
                    )
                }
                if (inspection is EntryMigrationHostInspectionResult.Ready) {
                    inspection.copy(sourceHasCustomCover = hasCustomCover(sourceEntryId))
                } else {
                    inspection
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                EntryMigrationHostInspectionResult.OperationalFailure(retryable = true)
            }
        }

        override suspend fun inspectExecution(
            sourceEntryId: Long,
            targetEntryId: Long,
        ): EntryMigrationExecutionInspectionResult {
            return try {
                val state = handler.await {
                    val source = loadEntry(profileId, sourceEntryId)
                        ?: return@await ExecutionState.SourceMissing
                    val target = loadEntry(profileId, targetEntryId)
                        ?: return@await ExecutionState.TargetMissing
                    ExecutionState.Ready(
                        source = source,
                        target = target,
                        sourceChildren = loadChildren(sourceEntryId),
                        targetChildren = loadChildren(targetEntryId),
                        sourceCategoryIds = loadCategoryIds(profileId, sourceEntryId),
                        sourceTracks = loadTracks(profileId, sourceEntryId),
                    )
                }
                when (state) {
                    ExecutionState.SourceMissing -> EntryMigrationExecutionInspectionResult.SourceMissing
                    ExecutionState.TargetMissing -> EntryMigrationExecutionInspectionResult.TargetMissing
                    is ExecutionState.Ready -> EntryMigrationExecutionInspectionResult.Ready(
                        source = state.source,
                        target = state.target,
                        sourceChildren = state.sourceChildren,
                        targetChildren = state.targetChildren,
                        sourceCategoryIds = state.sourceCategoryIds,
                        sourceTracks = state.sourceTracks,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                EntryMigrationExecutionInspectionResult.OperationalFailure(retryable = true)
            }
        }

        override suspend fun applyTransition(
            transition: EntryMigrationHostTransition,
            participateMergeReplacement: (suspend () -> EntryMergeMigrationReplacementResult)?,
        ): EntryMigrationHostTransitionResult {
            require(transition.profileId == profileId) { "Migration transition belongs to another profile" }
            require(
                (transition.mode == EntryMigrationMode.REPLACE) ==
                    (participateMergeReplacement != null),
            ) { "Replace Migration requires exactly one Merge transaction participant" }
            return try {
                handler.await(inTransaction = true) {
                    replayResult(transition)?.let { return@await it }
                    validate(transition)
                    transition.sourceUpdate?.let { updateEntry(profileId, it) }
                    updateEntry(profileId, transition.targetUpdate)
                    transition.targetCategoryIds?.let { categoryIds ->
                        entries_categoriesQueries.deleteByEntryId(profileId, transition.targetUpdate.id)
                        categoryIds.forEach { categoryId ->
                            categoriesQueries.getCategory(categoryId, profileId).awaitAsOneOrNull()
                                ?: migrationConflict()
                            entries_categoriesQueries.insert(profileId, transition.targetUpdate.id, categoryId)
                        }
                    }
                    transition.childUpdates.forEach { update ->
                        updateChild(update.updated)
                    }
                    transition.preparedTracks.forEach { track -> insertTrack(profileId, track) }
                    participateMergeReplacement?.let { replacement ->
                        when (val result = replacement()) {
                            EntryMergeMigrationReplacementResult.Applied,
                            EntryMergeMigrationReplacementResult.NoMembership,
                            -> Unit
                            EntryMergeMigrationReplacementResult.Conflict -> migrationConflict()
                            is EntryMergeMigrationReplacementResult.OperationalFailure -> {
                                throw MigrationParticipantFailure(result.retryable)
                            }
                        }
                    }
                    entry_migration_operationsQueries.insert(
                        operationId = transition.operationId,
                        intentFingerprint = transition.intentFingerprint,
                        profileId = profileId,
                        sourceEntryId = transition.expectedSource.id,
                        targetEntryId = transition.expectedTarget.id,
                        mode = transition.mode.name,
                        createdAt = clockMillis(),
                    )
                    transition.consequenceRequests.forEach { request ->
                        entry_migration_consequencesQueries.insert(
                            consequenceId = "${transition.operationId}:${request.participantId}",
                            operationId = transition.operationId,
                            profileId = profileId,
                            participantId = request.participantId,
                            schemaVersion = request.schemaVersion.toLong(),
                            payload = request.payload,
                            createdAt = clockMillis(),
                        )
                    }
                    EntryMigrationHostTransitionResult.Applied(
                        replayed = false,
                        hasPendingConsequences = transition.consequenceRequests.isNotEmpty(),
                    )
                }
            } catch (_: MigrationTransitionConflict) {
                EntryMigrationHostTransitionResult.Conflict
            } catch (error: MigrationParticipantFailure) {
                EntryMigrationHostTransitionResult.OperationalFailure(error.retryable)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                EntryMigrationHostTransitionResult.OperationalFailure(retryable = error !is IllegalArgumentException)
            }
        }

        private suspend fun Database.replayResult(
            transition: EntryMigrationHostTransition,
        ): EntryMigrationHostTransitionResult.Applied? {
            val operation = entry_migration_operationsQueries.getById(transition.operationId).awaitAsOneOrNull()
                ?: return null
            if (
                operation.intent_fingerprint != transition.intentFingerprint ||
                operation.profile_id != profileId ||
                operation.source_entry_id != transition.expectedSource.id ||
                operation.target_entry_id != transition.expectedTarget.id ||
                operation.mode != transition.mode.name
            ) {
                migrationConflict()
            }
            val pending = entry_migration_consequencesQueries.countByOperation(transition.operationId)
                .awaitAsOneOrNull() ?: 0L
            return EntryMigrationHostTransitionResult.Applied(
                replayed = true,
                hasPendingConsequences = pending > 0L,
            )
        }

        private suspend fun Database.validate(transition: EntryMigrationHostTransition) {
            if (loadEntry(profileId, transition.expectedSource.id) != transition.expectedSource) migrationConflict()
            if (loadEntry(profileId, transition.expectedTarget.id) != transition.expectedTarget) migrationConflict()
            transition.expectedSourceCategoryIds?.let { expected ->
                if (loadCategoryIds(profileId, transition.expectedSource.id).sorted() != expected.sorted()) {
                    migrationConflict()
                }
            }
            transition.expectedTargetChildren?.let { expected ->
                if (loadChildren(transition.expectedTarget.id) != expected) migrationConflict()
            }
            transition.childUpdates.forEach { update ->
                if (chaptersQueries.getChapterById(update.expected.id, EntryMapper::mapChapter).awaitAsOneOrNull() !=
                    update.expected
                ) {
                    migrationConflict()
                }
                if (update.updated.id != update.expected.id || update.updated.entryId != transition.expectedTarget.id) {
                    migrationConflict()
                }
            }
            if (
                loadTracks(profileId, transition.expectedSource.id).sortedBy(EntryTrack::trackerId) !=
                transition.expectedSourceTracks.sortedBy(EntryTrack::trackerId)
            ) {
                migrationConflict()
            }
            if (transition.targetUpdate.id != transition.expectedTarget.id) migrationConflict()
            if (transition.sourceUpdate?.id?.let { it != transition.expectedSource.id } == true) migrationConflict()
            if (transition.preparedTracks.any { it.entryId != transition.expectedTarget.id }) migrationConflict()
        }
    }

    private sealed interface ExecutionState {
        data object SourceMissing : ExecutionState
        data object TargetMissing : ExecutionState

        data class Ready(
            val source: Entry,
            val target: Entry,
            val sourceChildren: List<EntryChapter>,
            val targetChildren: List<EntryChapter>,
            val sourceCategoryIds: List<Long>,
            val sourceTracks: List<EntryTrack>,
        ) : ExecutionState
    }
}

private class MigrationTransitionConflict : RuntimeException()

private class MigrationParticipantFailure(
    val retryable: Boolean,
) : RuntimeException()

private fun migrationConflict(): Nothing = throw MigrationTransitionConflict()

private suspend fun Database.loadEntry(profileId: Long, entryId: Long): Entry? {
    return entriesQueries.getEntryById(entryId, profileId, EntryMapper::mapEntry).awaitAsOneOrNull()
}

private suspend fun Database.loadChildren(entryId: Long): List<EntryChapter> {
    return chaptersQueries.getChaptersByEntryId(entryId, 0, EntryMapper::mapChapter).awaitAsList()
}

private suspend fun Database.loadCategoryIds(profileId: Long, entryId: Long): List<Long> {
    return categoriesQueries.getCategoriesByEntryId(profileId, entryId) { id, _, _, _ -> id }.awaitAsList()
}

private suspend fun Database.loadTracks(profileId: Long, entryId: Long): List<EntryTrack> {
    return entry_syncQueries.getTracksByEntryId(profileId, entryId, TrackMapper::mapTrack).awaitAsList()
}

private suspend fun Database.updateEntry(profileId: Long, entry: Entry) {
    entriesQueries.update(
        source = entry.source,
        url = entry.url,
        title = entry.title,
        displayName = entry.displayName,
        artist = entry.artist,
        author = entry.author,
        description = entry.description,
        genre = entry.genre?.let(StringListColumnAdapter::encode),
        status = entry.status.value.toLong(),
        thumbnailUrl = entry.thumbnailUrl,
        favorite = entry.favorite,
        lastUpdate = entry.lastUpdate,
        nextUpdate = entry.nextUpdate,
        initialized = entry.initialized,
        viewer = entry.viewerFlags,
        chapterFlags = entry.chapterFlags,
        coverLastModified = entry.coverLastModified,
        dateAdded = entry.dateAdded,
        updateStrategy = UpdateStrategyColumnAdapter.encode(entry.updateStrategy),
        calculateInterval = entry.fetchInterval.toLong(),
        version = entry.version,
        isSyncing = entry.isSyncing,
        notes = entry.notes,
        memo = MemoColumnAdapter.encode(entry.memo),
        type = entry.type.name.lowercase(),
        entryId = entry.id,
        profileId = profileId,
    )
}

private suspend fun Database.updateChild(child: EntryChapter) {
    chaptersQueries.update(
        entryId = child.entryId,
        url = child.url,
        name = child.name,
        scanlator = child.scanlator,
        read = child.read,
        bookmark = child.bookmark,
        chapterNumber = child.chapterNumber,
        sourceOrder = child.sourceOrder,
        dateFetch = child.dateFetch,
        dateUpload = child.dateUpload,
        version = child.version,
        isSyncing = child.isSyncing,
        memo = MemoColumnAdapter.encode(child.memo),
        chapterId = child.id,
    )
}

private suspend fun Database.insertTrack(profileId: Long, track: EntryTrack) {
    entry_syncQueries.insert(
        profileId = profileId,
        entryId = track.entryId,
        syncId = track.trackerId,
        remoteId = track.remoteId,
        libraryId = track.libraryId,
        title = track.title,
        lastChapterRead = track.progress,
        totalChapters = track.total,
        status = track.status,
        score = track.score,
        remoteUrl = track.remoteUrl,
        startDate = track.startDate,
        finishDate = track.finishDate,
        private = track.private,
    )
}
