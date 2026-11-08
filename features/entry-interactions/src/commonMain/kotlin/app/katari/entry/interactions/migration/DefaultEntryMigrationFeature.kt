package app.katari.entry.interactions.migration

import app.katari.entry.interactions.download.EntryDownloadMaintenanceFeature
import app.katari.entry.interactions.download.EntryDownloadMaintenanceInspection
import app.katari.entry.interactions.durable.EntryDurableConsequences
import app.katari.entry.interactions.merge.EntryMergeLibraryLifecycleFeature
import app.katari.entry.interactions.merge.EntryMergeMigrationFeature
import app.katari.entry.interactions.merge.EntryMergeMigrationReplacementIntent
import app.katari.entry.interactions.migration.consequence.EntryMigrationConsequenceDelivery
import app.katari.entry.interactions.migration.consequence.EntryMigrationDurableEvent
import app.katari.entry.interactions.migration.host.EntryMigrationConsequenceRequest
import app.katari.entry.interactions.migration.host.EntryMigrationExecutionHost
import app.katari.entry.interactions.migration.host.EntryMigrationExecutionInspectionResult
import app.katari.entry.interactions.migration.host.EntryMigrationHostInspectionResult
import app.katari.entry.interactions.migration.host.EntryMigrationHostOperation
import app.katari.entry.interactions.migration.host.EntryMigrationHostReplayResult
import app.katari.entry.interactions.migration.host.EntryMigrationHostTransition
import app.katari.entry.interactions.migration.host.EntryMigrationHostTransitionResult
import app.katari.entry.interactions.migration.host.EntryMigrationPreparationHost
import app.katari.entry.interactions.source.EntrySourceRefreshFailure
import app.katari.entry.interactions.source.EntrySourceRefreshFeature
import app.katari.entry.interactions.source.EntrySourceRefreshRequest
import app.katari.entry.interactions.source.EntrySourceRefreshResult
import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import tachiyomi.domain.entry.migration.EntryMigrationFingerprint
import tachiyomi.domain.entry.migration.MigrationChildStateTransfer
import tachiyomi.domain.entry.migration.MigrationLibraryFields
import tachiyomi.domain.entry.migration.migratedLibraryFields
import tachiyomi.domain.entry.model.Entry

internal class DefaultEntryMigrationFeature(
    private val types: EntryMigrationTypes,
    private val preparationHost: EntryMigrationPreparationHost,
    private val executionHost: EntryMigrationExecutionHost,
    private val sourceRefresh: EntrySourceRefreshFeature,
    private val mergeMigration: EntryMergeMigrationFeature,
    private val mergeLibraryLifecycle: () -> EntryMergeLibraryLifecycleFeature,
    private val downloads: () -> EntryDownloadMaintenanceFeature,
    private val durableConsequences: EntryDurableConsequences<EntryMigrationDurableEvent>,
    private val consequences: EntryMigrationConsequenceDelivery,
    private val clockMillis: () -> Long = System::currentTimeMillis,
) : EntryMigrationFeature {
    private val migrationTypes = types.migration
    override fun childStateTransfer(type: EntryType) = MigrationChildStateTransfer(
        read = type in types.consumption,
        bookmarks = type in types.bookmark,
    )

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
        val members = if (reference.source.mergedBySource) {
            mergeMigration.lockedMembers(reference.source)
        } else {
            emptyList()
        }
        val pairing = when {
            members.isEmpty() -> EntryMigrationChildPairing.ByNumber
            refreshedTarget.mergedBySource -> {
                // Between merged entries, each member the old source supplied moves its progress to its pair first
                val pairs = mergeMigration.pairLockedMembers(reference.source, refreshedTarget)
                    .filter { validatePair(it.current, it.replacement) == null }
                pairs.forEach { pair ->
                    val result = migrateMember(
                        reference,
                        intent,
                        pair.current,
                        pair.replacement,
                        EntryMigrationChildPairing.ByNumber,
                    )
                    if (result !is EntryMigrationExecutionResult.Applied) return result
                }
                EntryMigrationChildPairing.ByNumber
            }
            else -> {
                // Onto an entry that lists every season at once, each season the old source supplied takes the
                // stretch after the root's chapters and the seasons before it
                var offset = childCount(reference.source, refreshedTarget)
                    ?: return EntryMigrationExecutionResult.OperationalFailure(retryable = true)
                members.forEach { member ->
                    val count = childCount(member, refreshedTarget)
                        ?: return EntryMigrationExecutionResult.OperationalFailure(retryable = true)
                    if (validatePair(member, refreshedTarget) == null) {
                        val result = migrateMember(
                            reference,
                            intent,
                            member,
                            refreshedTarget,
                            EntryMigrationChildPairing.ByPosition(offset),
                        )
                        if (result !is EntryMigrationExecutionResult.Applied) return result
                    }
                    offset += count
                }
                EntryMigrationChildPairing.ByPosition(offset = 0)
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
            pairing = pairing,
            chapterStateOnly = false,
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

    /**
     * Migrates [source], a member the old merged entry's source supplied, ahead of the merged entry itself. A member
     * landing on [reference]'s own target only brings its chapters' state and leaves the target's Library state to
     * the merged entry, whose migration has yet to find it as it was.
     */
    private suspend fun migrateMember(
        reference: FeatureEntryMigrationReference,
        intent: EntryMigrationExecuteIntent,
        source: Entry,
        target: Entry,
        pairing: EntryMigrationChildPairing,
    ): EntryMigrationExecutionResult {
        val operationId = "${reference.sessionId}:${source.id}"
        val profile = executionHost.profile(source.profileId)
        val ownTarget = target.id != reference.target.id
        val memberOptions = if (ownTarget) intent.selectedOptions else intent.selectedOptions intersect CHAPTER_OPTIONS
        val fingerprint = migrationFingerprint(intent.mode, memberOptions)
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
        if (ownTarget) {
            when (refreshTarget(inspection.target, fetchDetails = true, fetchChildren = true)) {
                EntryMigrationTargetRefreshResult.Refreshed -> Unit
                is EntryMigrationTargetRefreshResult.Rejected -> return EntryMigrationExecutionResult.Conflict
                EntryMigrationTargetRefreshResult.SourceUnavailable,
                EntryMigrationTargetRefreshResult.NoChildren,
                is EntryMigrationTargetRefreshResult.OperationalFailure,
                -> return EntryMigrationExecutionResult.OperationalFailure(retryable = true)
            }
        }
        return migrate(
            profileId = source.profileId,
            operationId = operationId,
            fingerprint = fingerprint,
            sourceId = source.id,
            targetId = target.id,
            mode = intent.mode,
            selectedOptions = memberOptions intersect availableOptions(inspection),
            pairing = pairing,
            chapterStateOnly = !ownTarget,
            authorized = { inspectedSource, inspectedTarget ->
                inspectedSource.sameMigrationAuthorization(inspection.source) &&
                    inspectedTarget.sameMigrationAuthorization(inspection.target)
            },
        )
    }

    /**
     * Moves [sourceId]'s Library state to [targetId] in one transaction, once both are inspected as [authorized]. With
     * [chapterStateOnly], the target keeps its own Library fields and choices, and only takes chapter state.
     */
    private suspend fun migrate(
        profileId: Long,
        operationId: String,
        fingerprint: String,
        sourceId: Long,
        targetId: Long,
        mode: EntryMigrationMode,
        selectedOptions: Set<EntryMigrationOption>,
        pairing: EntryMigrationChildPairing,
        chapterStateOnly: Boolean,
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

        val transferChildren = EntryMigrationOption.CHILD_STATE in selectedOptions
        val childPairs = pairing.pair(inspected.sourceChildren, inspected.targetChildren)
        val durableEvent = EntryMigrationDurableEvent(
            operationId = operationId,
            source = inspected.source,
            target = inspected.target,
            selectedOptions = selectedOptions,
            childMatches = childPairs.matches,
            chapterStateOnly = chapterStateOnly,
        )
        val childUpdates = if (transferChildren) {
            prepareMigrationChildUpdates(
                sourceChildren = inspected.sourceChildren,
                childPairs = childPairs,
                transfer = childStateTransfer(inspected.source.type),
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
        val targetUpdate = if (!chapterStateOnly) {
            val libraryFields = migratedLibraryFields(
                source = inspected.source.libraryFields(),
                target = inspected.target.libraryFields(),
                replace = replace,
                carryNotes = EntryMigrationOption.NOTES in selectedOptions,
                now = clockMillis(),
            )
            inspected.target.withLibraryFields(libraryFields)
        } else {
            inspected.target
        }
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
            sourceUpdate = inspected.source.withLibraryFields(inspected.source.libraryFields().replaced())
                .takeIf { replace },
            targetUpdate = targetUpdate,
            targetCategoryIds = inspected.sourceCategoryIds
                .takeIf { EntryMigrationOption.CATEGORIES in selectedOptions },
            childUpdates = childUpdates,
            historyChapterMoves = durableEvent.childMatches
                .mapNotNull { match -> match.source?.let { source -> source.id to match.target.id } }
                .distinctBy { (sourceId) -> sourceId }
                .toMap()
                .takeIf { replace },
            childOrder = childPairs.order,
            chapterStateOnly = chapterStateOnly,
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

    private suspend fun childCount(entry: Entry, target: Entry): Int? {
        val inspection = executionHost.profile(entry.profileId).inspectExecution(entry.id, target.id)
        return (inspection as? EntryMigrationExecutionInspectionResult.Ready)?.sourceChildren?.size
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

    private fun Entry.libraryFields() = MigrationLibraryFields(favorite, libraryPinned, chapterFlags, dateAdded, notes)

    private fun Entry.withLibraryFields(fields: MigrationLibraryFields) = copy(
        favorite = fields.favorite,
        libraryPinned = fields.libraryPinned,
        chapterFlags = fields.chapterFlags,
        dateAdded = fields.dateAdded,
        notes = fields.notes,
    )

    private fun Entry.isPersisted(): Boolean = id > 0L && profileId > 0L

    private fun Entry.subject() = EntryMigrationSubject(profileId, id)

    private fun migrationFingerprint(
        mode: EntryMigrationMode,
        selectedOptions: Set<EntryMigrationOption>,
    ): String = EntryMigrationFingerprint.of(mode.name, selectedOptions)

    private fun selectionRejected(reason: EntryMigrationRejection) = EntryMigrationSelectionResult.Rejected(reason)

    private fun preparationRejected(reason: EntryMigrationRejection) = EntryMigrationPreparationResult.Rejected(reason)

    private fun executionRejected(reason: EntryMigrationRejection) = EntryMigrationExecutionResult.Rejected(reason)
}

/** The options a member folding into its merged entry's target takes: the rest are the merged entry's to carry. */
private val CHAPTER_OPTIONS = setOf(EntryMigrationOption.CHILD_STATE, EntryMigrationOption.REMOVE_SOURCE_DOWNLOADS)

/** The types each Migration capability applies to, read from the providers the type modules contribute. */
internal class EntryMigrationTypes(
    val migration: Set<EntryType>,
    val consumption: Set<EntryType>,
    val bookmark: Set<EntryType>,
    val childStateOption: Set<EntryType>,
)
