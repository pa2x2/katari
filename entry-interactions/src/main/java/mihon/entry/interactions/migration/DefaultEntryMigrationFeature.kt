package mihon.entry.interactions.migration

import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import mihon.entry.interactions.download.EntryDownloadMaintenanceFeature
import mihon.entry.interactions.download.EntryDownloadMaintenanceInspection
import mihon.entry.interactions.durable.EntryDurableConsequences
import mihon.entry.interactions.merge.EntryMergeLibraryLifecycleFeature
import mihon.entry.interactions.merge.EntryMergeMigrationFeature
import mihon.entry.interactions.merge.EntryMergeMigrationReplacementIntent
import mihon.entry.interactions.migration.consequence.EntryMigrationConsequenceDelivery
import mihon.entry.interactions.migration.consequence.EntryMigrationDurableEvent
import mihon.entry.interactions.migration.host.EntryMigrationConsequenceRequest
import mihon.entry.interactions.migration.host.EntryMigrationExecutionHost
import mihon.entry.interactions.migration.host.EntryMigrationExecutionInspectionResult
import mihon.entry.interactions.migration.host.EntryMigrationHostInspectionResult
import mihon.entry.interactions.migration.host.EntryMigrationHostOperation
import mihon.entry.interactions.migration.host.EntryMigrationHostReplayResult
import mihon.entry.interactions.migration.host.EntryMigrationHostTransition
import mihon.entry.interactions.migration.host.EntryMigrationHostTransitionResult
import mihon.entry.interactions.migration.host.EntryMigrationPreparationHost
import mihon.entry.interactions.source.EntrySourceRefreshFailure
import mihon.entry.interactions.source.EntrySourceRefreshFeature
import mihon.entry.interactions.source.EntrySourceRefreshRequest
import mihon.entry.interactions.source.EntrySourceRefreshResult
import mihon.entry.interactions.tracking.EntryTrackingFeature
import mihon.entry.interactions.tracking.EntryTrackingMigrationPreparationResult
import mihon.entry.interactions.tracking.toDomainTrack
import mihon.entry.interactions.tracking.toTrackingRecord
import tachiyomi.domain.entry.model.Entry

internal class DefaultEntryMigrationFeature(
    private val types: EntryMigrationTypes,
    private val preparationHost: EntryMigrationPreparationHost,
    private val executionHost: EntryMigrationExecutionHost,
    private val sourceRefresh: EntrySourceRefreshFeature,
    private val mergeMigration: EntryMergeMigrationFeature,
    private val mergeLibraryLifecycle: () -> EntryMergeLibraryLifecycleFeature,
    private val downloads: () -> EntryDownloadMaintenanceFeature,
    private val tracking: () -> EntryTrackingFeature,
    private val durableConsequences: EntryDurableConsequences<EntryMigrationDurableEvent>,
    private val consequences: EntryMigrationConsequenceDelivery,
    private val clockMillis: () -> Long = System::currentTimeMillis,
) : EntryMigrationFeature {
    private val migrationTypes = types.migration
    override fun availability(entry: Entry): EntryMigrationAvailability {
        val rejection = entry.sourceRejection()
        return if (rejection == null) {
            EntryMigrationAvailability.Available
        } else {
            EntryMigrationAvailability.Unavailable(rejection)
        }
    }

    override fun prepareSelection(entries: List<Entry>): EntryMigrationSelectionResult {
        if (entries.isEmpty()) return selectionRejected(EntryMigrationRejection.EMPTY_SELECTION)
        val singleProfile = entries.map(Entry::profileId).distinct().size == 1
        if (!singleProfile) {
            return selectionRejected(EntryMigrationRejection.MIXED_SELECTION_PROFILES)
        }
        entries.firstNotNullOfOrNull { entry -> entry.sourceRejection() }
            ?.let { return selectionRejected(it) }
        return EntryMigrationSelectionResult.Ready(
            entries.map { entry -> EntryMigrationSubject(entry.profileId, entry.id) },
        )
    }

    override suspend fun refreshTarget(intent: EntryMigrationTargetRefreshIntent): EntryMigrationTargetRefreshResult {
        validatePair(intent.source, intent.target)?.let {
            return EntryMigrationTargetRefreshResult.Rejected(it)
        }
        return refreshTarget(
            target = intent.target,
            fetchDetails = intent.fetchDetails,
            fetchChildren = intent.fetchChildren,
        )
    }

    override suspend fun reconcileOperation(
        intent: EntryMigrationOperationIntent,
    ): EntryMigrationOperationReconciliationResult {
        if (intent.source.profileId != intent.target.profileId) {
            return EntryMigrationOperationReconciliationResult.Conflict
        }
        return try {
            val profile = executionHost.profile(intent.source.profileId)
            when (
                val replay = profile.replay(
                    EntryMigrationHostOperation(
                        operationId = intent.key.value,
                        intentFingerprint = migrationFingerprint(intent.mode, intent.selectedOptions),
                        sourceEntryId = intent.source.entryId,
                        targetEntryId = intent.target.entryId,
                        mode = intent.mode,
                    ),
                )
            ) {
                is EntryMigrationHostReplayResult.Applied -> {
                    val followUp = if (replay.hasPendingConsequences) {
                        consequences.deliverOperation(intent.key.value)
                    } else {
                        EntryMigrationFollowUp.COMPLETE
                    }
                    EntryMigrationOperationReconciliationResult.Applied(
                        EntryMigrationOutcome(
                            source = intent.source,
                            target = intent.target,
                            mode = intent.mode,
                            followUp = followUp,
                        ),
                    )
                }
                EntryMigrationHostReplayResult.Conflict -> EntryMigrationOperationReconciliationResult.Conflict
                EntryMigrationHostReplayResult.NotApplied -> EntryMigrationOperationReconciliationResult.NotApplied
                is EntryMigrationHostReplayResult.OperationalFailure -> {
                    EntryMigrationOperationReconciliationResult.OperationalFailure(replay.retryable)
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            EntryMigrationOperationReconciliationResult.OperationalFailure(retryable = true)
        }
    }

    override suspend fun prepare(intent: EntryMigrationPrepareIntent): EntryMigrationPreparationResult {
        validatePair(intent.source, intent.target)?.let { return preparationRejected(it) }
        return try {
            when (
                val inspection = preparationHost.profile(intent.source.profileId)
                    .inspectPair(intent.source.id, intent.target.id)
            ) {
                is EntryMigrationHostInspectionResult.Ready -> prepare(inspection, intent)
                EntryMigrationHostInspectionResult.SourceMissing -> {
                    preparationRejected(EntryMigrationRejection.UNPERSISTED_ENTRY)
                }
                EntryMigrationHostInspectionResult.TargetMissing -> {
                    preparationRejected(EntryMigrationRejection.UNPERSISTED_ENTRY)
                }
                is EntryMigrationHostInspectionResult.OperationalFailure -> {
                    EntryMigrationPreparationResult.OperationalFailure(inspection.retryable)
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            EntryMigrationPreparationResult.OperationalFailure(retryable = true)
        }
    }

    override suspend fun execute(intent: EntryMigrationExecuteIntent): EntryMigrationExecutionResult {
        val reference = intent.reference as? FeatureEntryMigrationReference
            ?: return executionRejected(EntryMigrationRejection.UNRECOGNIZED_REFERENCE)
        if (!reference.availableOptions.containsAll(intent.selectedOptions)) {
            return executionRejected(EntryMigrationRejection.INVALID_OPTIONS)
        }
        return try {
            execute(reference, intent)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            EntryMigrationExecutionResult.OperationalFailure(retryable = true)
        }
    }

    private suspend fun prepare(
        inspection: EntryMigrationHostInspectionResult.Ready,
        intent: EntryMigrationPrepareIntent,
    ): EntryMigrationPreparationResult {
        val identityStable = inspection.source.sameMigrationIdentity(intent.source) &&
            inspection.target.sameMigrationIdentity(intent.target)
        if (!identityStable) {
            return preparationRejected(EntryMigrationRejection.ENTRY_IDENTITY_CHANGED)
        }
        validatePair(inspection.source, inspection.target)?.let { return preparationRejected(it) }
        val options = availableOptions(inspection)
        val reference = FeatureEntryMigrationReference(
            sessionId = intent.operationKey.operationId(),
            source = inspection.source,
            target = inspection.target,
            availableOptions = options,
        )
        return EntryMigrationPreparationResult.Ready(
            reference = reference,
            source = inspection.source.subject(),
            target = inspection.target.subject(),
            availableOptions = options,
        )
    }

    private suspend fun availableOptions(
        inspection: EntryMigrationHostInspectionResult.Ready,
    ): Set<EntryMigrationOption> {
        val hasDownloads =
            downloads().inspectEntry(inspection.source) == EntryDownloadMaintenanceInspection.HasDownloads
        return buildSet {
            if (inspection.source.type in types.childStateOption) {
                add(EntryMigrationOption.CHILD_STATE)
            }
            add(EntryMigrationOption.CATEGORIES)
            if (inspection.source.notes.isNotBlank()) add(EntryMigrationOption.NOTES)
            if (inspection.sourceHasCustomCover) add(EntryMigrationOption.CUSTOM_COVER)
            if (hasDownloads) add(EntryMigrationOption.REMOVE_SOURCE_DOWNLOADS)
        }
    }

    private suspend fun execute(
        reference: FeatureEntryMigrationReference,
        intent: EntryMigrationExecuteIntent,
    ): EntryMigrationExecutionResult {
        val profileId = reference.source.profileId
        val profile = executionHost.profile(profileId)
        val fingerprint = migrationFingerprint(intent.mode, intent.selectedOptions)
        when (
            val replay = profile.replay(
                EntryMigrationHostOperation(
                    operationId = reference.sessionId,
                    intentFingerprint = fingerprint,
                    sourceEntryId = reference.source.id,
                    targetEntryId = reference.target.id,
                    mode = intent.mode,
                ),
            )
        ) {
            is EntryMigrationHostReplayResult.Applied -> {
                val followUp = if (replay.hasPendingConsequences) {
                    consequences.deliverOperation(reference.sessionId)
                } else {
                    EntryMigrationFollowUp.COMPLETE
                }
                return applied(reference, intent.mode, followUp)
            }
            EntryMigrationHostReplayResult.Conflict -> return EntryMigrationExecutionResult.Conflict
            EntryMigrationHostReplayResult.NotApplied -> Unit
            is EntryMigrationHostReplayResult.OperationalFailure -> {
                return EntryMigrationExecutionResult.OperationalFailure(replay.retryable)
            }
        }
        val target = when (
            val inspection = preparationHost.profile(profileId)
                .inspectPair(reference.source.id, reference.target.id)
        ) {
            is EntryMigrationHostInspectionResult.Ready -> {
                val authorizationStable = inspection.source.sameMigrationAuthorization(reference.source) &&
                    inspection.target.sameMigrationAuthorization(reference.target)
                if (!authorizationStable) {
                    return EntryMigrationExecutionResult.Conflict
                }
                inspection.target
            }
            EntryMigrationHostInspectionResult.SourceMissing,
            EntryMigrationHostInspectionResult.TargetMissing,
            -> {
                return EntryMigrationExecutionResult.Conflict
            }
            is EntryMigrationHostInspectionResult.OperationalFailure -> {
                return EntryMigrationExecutionResult.OperationalFailure(inspection.retryable)
            }
        }

        // Progress moves chapter by chapter, so a chapter still to come in a later part of the list would lose its own
        when (refreshTarget(target, fetchDetails = true, fetchChildren = true, wholeChildList = true)) {
            EntryMigrationTargetRefreshResult.Refreshed -> Unit
            is EntryMigrationTargetRefreshResult.Rejected -> return EntryMigrationExecutionResult.Conflict
            EntryMigrationTargetRefreshResult.SourceUnavailable,
            EntryMigrationTargetRefreshResult.NoChildren,
            is EntryMigrationTargetRefreshResult.OperationalFailure,
            -> return EntryMigrationExecutionResult.OperationalFailure(retryable = true)
        }
        // The refresh can be what makes the target a merged entry, when the search that found it didn't say so
        val refreshedTarget = (
            preparationHost.profile(profileId).inspectPair(reference.source.id, reference.target.id)
                as? EntryMigrationHostInspectionResult.Ready
            )?.target ?: return EntryMigrationExecutionResult.Conflict
        // Between merged entries, each member the old source supplied moves its progress to its pair first
        if (reference.source.mergedBySource && refreshedTarget.mergedBySource) {
            val pairs = mergeMigration.pairLockedMembers(reference.source, refreshedTarget)
                .filter { validatePair(it.current, it.replacement) == null }
            pairs.forEach { pair ->
                val result = migrateMember(reference, intent, pair.current, pair.replacement)
                if (result !is EntryMigrationExecutionResult.Applied) return result
            }
        }
        val result = migrate(
            profileId = profileId,
            operationId = reference.sessionId,
            fingerprint = fingerprint,
            sourceId = reference.source.id,
            targetId = reference.target.id,
            mode = intent.mode,
            selectedOptions = intent.selectedOptions,
            authorized = { source, target ->
                source.sameMigrationAuthorization(reference.source) &&
                    target.sameMigrationAuthorization(reference.target)
            },
        )
        if (result is EntryMigrationExecutionResult.Applied && refreshedTarget.mergedBySource) {
            // The members the new merged entry's source supplies join the Library with it; any left out join on the
            // next refresh, which adds a Library merged entry's missing members
            try {
                mergeLibraryLifecycle().entryAddedToLibrary(refreshedTarget)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
            }
        }
        return result
    }

    private suspend fun migrateMember(
        reference: FeatureEntryMigrationReference,
        intent: EntryMigrationExecuteIntent,
        source: Entry,
        target: Entry,
    ): EntryMigrationExecutionResult {
        val operationId = "${reference.sessionId}:${source.id}"
        val profile = executionHost.profile(source.profileId)
        val fingerprint = migrationFingerprint(intent.mode, intent.selectedOptions)
        when (
            val replay = profile.replay(
                EntryMigrationHostOperation(operationId, fingerprint, source.id, target.id, intent.mode),
            )
        ) {
            is EntryMigrationHostReplayResult.Applied -> return EntryMigrationExecutionResult.Applied(
                EntryMigrationOutcome(source.subject(), target.subject(), intent.mode, EntryMigrationFollowUp.COMPLETE),
            )
            EntryMigrationHostReplayResult.Conflict -> return EntryMigrationExecutionResult.Conflict
            EntryMigrationHostReplayResult.NotApplied -> Unit
            is EntryMigrationHostReplayResult.OperationalFailure -> {
                return EntryMigrationExecutionResult.OperationalFailure(replay.retryable)
            }
        }
        val inspection = when (
            val result = preparationHost.profile(source.profileId).inspectPair(source.id, target.id)
        ) {
            is EntryMigrationHostInspectionResult.Ready -> result
            EntryMigrationHostInspectionResult.SourceMissing,
            EntryMigrationHostInspectionResult.TargetMissing,
            -> return EntryMigrationExecutionResult.Conflict
            is EntryMigrationHostInspectionResult.OperationalFailure -> {
                return EntryMigrationExecutionResult.OperationalFailure(result.retryable)
            }
        }
        when (refreshTarget(inspection.target, fetchDetails = true, fetchChildren = true)) {
            EntryMigrationTargetRefreshResult.Refreshed -> Unit
            is EntryMigrationTargetRefreshResult.Rejected -> return EntryMigrationExecutionResult.Conflict
            EntryMigrationTargetRefreshResult.SourceUnavailable,
            EntryMigrationTargetRefreshResult.NoChildren,
            is EntryMigrationTargetRefreshResult.OperationalFailure,
            -> return EntryMigrationExecutionResult.OperationalFailure(retryable = true)
        }
        return migrate(
            profileId = source.profileId,
            operationId = operationId,
            fingerprint = fingerprint,
            sourceId = source.id,
            targetId = target.id,
            mode = intent.mode,
            selectedOptions = intent.selectedOptions intersect availableOptions(inspection),
            authorized = { inspectedSource, inspectedTarget ->
                inspectedSource.sameMigrationAuthorization(inspection.source) &&
                    inspectedTarget.sameMigrationAuthorization(inspection.target)
            },
        )
    }

    /** Moves [sourceId]'s Library state to [targetId] in one transaction, once both are inspected as [authorized]. */
    private suspend fun migrate(
        profileId: Long,
        operationId: String,
        fingerprint: String,
        sourceId: Long,
        targetId: Long,
        mode: EntryMigrationMode,
        selectedOptions: Set<EntryMigrationOption>,
        authorized: (source: Entry, target: Entry) -> Boolean,
    ): EntryMigrationExecutionResult {
        val profile = executionHost.profile(profileId)
        val inspected = when (
            val result = profile.inspectExecution(sourceId, targetId)
        ) {
            is EntryMigrationExecutionInspectionResult.Ready -> result
            EntryMigrationExecutionInspectionResult.SourceMissing,
            EntryMigrationExecutionInspectionResult.TargetMissing,
            -> {
                return EntryMigrationExecutionResult.Conflict
            }
            is EntryMigrationExecutionInspectionResult.OperationalFailure -> {
                return EntryMigrationExecutionResult.OperationalFailure(result.retryable)
            }
        }
        if (!authorized(inspected.source, inspected.target)) {
            return EntryMigrationExecutionResult.Conflict
        }

        val preparedTracks = when (
            val result = tracking().prepareMigrationTracks(
                source = inspected.source,
                target = inspected.target,
                tracks = inspected.sourceTracks.map { it.toTrackingRecord() },
            )
        ) {
            is EntryTrackingMigrationPreparationResult.Prepared -> result.tracks.map { it.toDomainTrack() }
            is EntryTrackingMigrationPreparationResult.Failed -> {
                return EntryMigrationExecutionResult.OperationalFailure(retryable = true)
            }
        }

        val transferChildren = EntryMigrationOption.CHILD_STATE in selectedOptions
        val durableEvent = EntryMigrationDurableEvent(
            operationId = operationId,
            source = inspected.source,
            target = inspected.target,
            selectedOptions = selectedOptions,
            sourceChildren = inspected.sourceChildren,
            targetChildren = inspected.targetChildren,
        )
        val childUpdates = if (transferChildren) {
            prepareMigrationChildUpdates(
                sourceChildren = inspected.sourceChildren,
                childMatches = durableEvent.childMatches,
                transferConsumption = inspected.source.type in types.consumption,
                transferBookmarks = inspected.source.type in types.bookmark,
            )
        } else {
            emptyList()
        }
        val replace = mode == EntryMigrationMode.REPLACE
        val preparedConsequences = try {
            durableConsequences.prepare(durableEvent)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            return EntryMigrationExecutionResult.OperationalFailure(retryable = true)
        }
        val consequenceRequests = preparedConsequences.map { record ->
            EntryMigrationConsequenceRequest(
                participantId = record.participantId,
                schemaVersion = record.payload.schemaVersion,
                payload = record.payload.value,
            )
        }
        val targetUpdate = inspected.target.copy(
            favorite = true,
            libraryPinned = inspected.source.libraryPinned || inspected.target.libraryPinned,
            chapterFlags = inspected.source.chapterFlags,
            dateAdded = if (replace) inspected.source.dateAdded else clockMillis(),
            notes = if (EntryMigrationOption.NOTES in selectedOptions) {
                inspected.source.notes
            } else {
                inspected.target.notes
            },
        )
        val transition = EntryMigrationHostTransition(
            operationId = operationId,
            intentFingerprint = fingerprint,
            profileId = profileId,
            mode = mode,
            expectedSource = inspected.source,
            expectedTarget = inspected.target,
            expectedSourceCategoryIds = inspected.sourceCategoryIds
                .takeIf { EntryMigrationOption.CATEGORIES in selectedOptions },
            expectedTargetChildren = inspected.targetChildren.takeIf { transferChildren },
            sourceUpdate = inspected.source.copy(
                favorite = false,
                libraryPinned = false,
                dateAdded = 0L,
            ).takeIf { replace },
            targetUpdate = targetUpdate,
            targetCategoryIds = inspected.sourceCategoryIds
                .takeIf { EntryMigrationOption.CATEGORIES in selectedOptions },
            childUpdates = childUpdates,
            expectedSourceTracks = inspected.sourceTracks,
            preparedTracks = preparedTracks,
            consequenceRequests = consequenceRequests,
        )
        val mergeReplacement = if (replace) {
            suspend {
                mergeMigration.participateInReplacementTransaction(
                    EntryMergeMigrationReplacementIntent(inspected.source, inspected.target),
                )
            }
        } else {
            null
        }
        val result = try {
            profile.applyTransition(transition, mergeReplacement)
        } catch (error: CancellationException) {
            withContext(NonCancellable) {
                runCatching { durableConsequences.discard(preparedConsequences) }
            }
            throw error
        }
        if (result !is EntryMigrationHostTransitionResult.Applied) {
            durableConsequences.discard(preparedConsequences)
        }
        return when (result) {
            is EntryMigrationHostTransitionResult.Applied -> {
                val followUp = if (result.hasPendingConsequences) {
                    consequences.deliverOperation(operationId)
                } else {
                    EntryMigrationFollowUp.COMPLETE
                }
                EntryMigrationExecutionResult.Applied(
                    EntryMigrationOutcome(inspected.source.subject(), inspected.target.subject(), mode, followUp),
                )
            }
            EntryMigrationHostTransitionResult.Conflict -> EntryMigrationExecutionResult.Conflict
            is EntryMigrationHostTransitionResult.OperationalFailure -> {
                EntryMigrationExecutionResult.OperationalFailure(result.retryable)
            }
        }
    }

    private fun applied(
        reference: FeatureEntryMigrationReference,
        mode: EntryMigrationMode,
        followUp: EntryMigrationFollowUp,
    ): EntryMigrationExecutionResult.Applied {
        return EntryMigrationExecutionResult.Applied(
            EntryMigrationOutcome(
                source = reference.source.subject(),
                target = reference.target.subject(),
                mode = mode,
                followUp = followUp,
            ),
        )
    }

    private fun validatePair(source: Entry, target: Entry): EntryMigrationRejection? {
        source.sourceRejection()?.let { return it }
        if (target.id <= 0L || target.profileId <= 0L) return EntryMigrationRejection.UNPERSISTED_ENTRY
        if (source.profileId != target.profileId) return EntryMigrationRejection.SOURCE_TARGET_PROFILE_MISMATCH
        if (source.type != target.type) return EntryMigrationRejection.SOURCE_TARGET_TYPE_MISMATCH
        if (target.type !in migrationTypes) return EntryMigrationRejection.UNSUPPORTED_TARGET_TYPE
        if (source.id == target.id) return EntryMigrationRejection.SAME_ENTRY
        return null
    }

    private suspend fun refreshTarget(
        target: Entry,
        fetchDetails: Boolean,
        fetchChildren: Boolean,
        wholeChildList: Boolean = false,
    ): EntryMigrationTargetRefreshResult {
        return when (
            val result = sourceRefresh.refresh(
                EntrySourceRefreshRequest(
                    entry = target,
                    fetchDetails = fetchDetails,
                    fetchChildren = fetchChildren,
                    manual = false,
                    wholeChildList = wholeChildList,
                ),
            )
        ) {
            is EntrySourceRefreshResult.Refreshed -> EntryMigrationTargetRefreshResult.Refreshed
            is EntrySourceRefreshResult.SourceUnavailable -> EntryMigrationTargetRefreshResult.SourceUnavailable
            is EntrySourceRefreshResult.Failed -> when (val reason = result.reason) {
                EntrySourceRefreshFailure.NoChildren -> EntryMigrationTargetRefreshResult.NoChildren
                is EntrySourceRefreshFailure.Operation -> {
                    EntryMigrationTargetRefreshResult.OperationalFailure(reason.error)
                }
            }
        }
    }

    private fun Entry.sourceRejection(): EntryMigrationRejection? {
        if (!isPersisted()) return EntryMigrationRejection.UNPERSISTED_ENTRY
        if (!favorite) return EntryMigrationRejection.SOURCE_NOT_IN_LIBRARY
        if (type !in migrationTypes) return EntryMigrationRejection.UNSUPPORTED_SOURCE_TYPE
        return null
    }

    private fun Entry.isPersisted(): Boolean = id > 0L && profileId > 0L

    private fun Entry.subject() = EntryMigrationSubject(profileId, id)

    private fun migrationFingerprint(
        mode: EntryMigrationMode,
        selectedOptions: Set<EntryMigrationOption>,
    ): String {
        return buildString {
            append(mode.name)
            append(':')
            append(selectedOptions.map(EntryMigrationOption::name).sorted().joinToString(","))
        }
    }

    private fun selectionRejected(reason: EntryMigrationRejection) = EntryMigrationSelectionResult.Rejected(reason)

    private fun preparationRejected(reason: EntryMigrationRejection) = EntryMigrationPreparationResult.Rejected(reason)

    private fun executionRejected(reason: EntryMigrationRejection) = EntryMigrationExecutionResult.Rejected(reason)
}

/** The types each Migration capability applies to, read from the providers the type modules contribute. */
internal class EntryMigrationTypes(
    val migration: Set<EntryType>,
    val consumption: Set<EntryType>,
    val bookmark: Set<EntryType>,
    val childStateOption: Set<EntryType>,
)
