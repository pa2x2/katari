package mihon.entry.interactions

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import mihon.entry.interactions.host.EntryMigrationExecutionHost
import mihon.entry.interactions.host.EntryMigrationExecutionInspectionResult
import mihon.entry.interactions.host.EntryMigrationHostInspectionResult
import mihon.entry.interactions.host.EntryMigrationHostOperation
import mihon.entry.interactions.host.EntryMigrationHostReplayResult
import mihon.entry.interactions.host.EntryMigrationHostTransition
import mihon.entry.interactions.host.EntryMigrationHostTransitionResult
import mihon.entry.interactions.host.EntryMigrationPreparationHost
import mihon.feature.graph.FeatureGraphEvaluation
import tachiyomi.domain.entry.model.Entry

internal class DefaultEntryMigrationFeature(
    private val evaluation: FeatureGraphEvaluation,
    private val preparationHost: EntryMigrationPreparationHost,
    private val executionHost: EntryMigrationExecutionHost,
    private val sourceRefresh: EntrySourceRefreshFeature,
    private val mergeMigration: EntryMergeMigrationFeature,
    private val optionDiscovery: EntryMigrationOptionDiscovery,
    private val transitionPreparation: EntryMigrationTransitionPreparation,
    private val durableConsequences: EntryMigrationDurableConsequences,
    private val consequences: EntryMigrationConsequenceDelivery,
    private val clockMillis: () -> Long = System::currentTimeMillis,
) : EntryMigrationFeature {
    private val migrationTypes = evaluation.applicableProviderTypes<EntryMigrationProvider>(
        feature = ENTRY_MIGRATION_FEATURE_ID,
        integration = ENTRY_MIGRATION_BASE_INTEGRATION_ID,
        behaviorProjection = EntryMigrationBaseBehavior.PROVIDER_DISPATCH.id,
    )
    private val consumptionTypes = evaluation.applicableProviderTypes<EntryConsumptionProcessor>(
        feature = ENTRY_MIGRATION_FEATURE_ID,
        integration = ENTRY_MIGRATION_CONSUMPTION_INTEGRATION_ID,
        behaviorProjection = EntryMigrationConsumptionBehavior.TRANSFER.id,
    )
    private val bookmarkTypes = evaluation.applicableProviderTypes<EntryBookmarkProcessor>(
        feature = ENTRY_MIGRATION_FEATURE_ID,
        integration = ENTRY_MIGRATION_BOOKMARK_INTEGRATION_ID,
        behaviorProjection = EntryMigrationBookmarkBehavior.TRANSFER.id,
    )
    private val childStateOptionTypes = evaluation.applicableProviderTypes<EntryMigrationProvider>(
        feature = ENTRY_MIGRATION_FEATURE_ID,
        integration = ENTRY_MIGRATION_CHILD_STATE_OPTION_INTEGRATION_ID,
        behaviorProjection = EntryMigrationChildStateOptionBehavior.id,
    )
    override fun availability(entry: Entry): EntryMigrationAvailability {
        val rejection = entry.sourceRejection()
        if (entry.type in migrationTypes) {
            evaluation.requireMigrationSourceContext(
                type = entry.type,
                persisted = entry.isPersisted(),
                inLibrary = entry.favorite,
            )
        }
        return if (rejection == null) {
            EntryMigrationAvailability.Available
        } else {
            EntryMigrationAvailability.Unavailable(rejection)
        }
    }

    override fun prepareSelection(entries: List<Entry>): EntryMigrationSelectionResult {
        if (entries.isEmpty()) return selectionRejected(EntryMigrationRejection.EMPTY_SELECTION)
        val singleProfile = entries.map(Entry::profileId).distinct().size == 1
        entries
            .filter { it.type in migrationTypes }
            .forEach { entry ->
                evaluation.requireMigrationSourceContext(
                    type = entry.type,
                    persisted = entry.isPersisted(),
                    inLibrary = entry.favorite,
                )
                evaluation.requireMigrationSelectionContext(
                    type = entry.type,
                    persisted = entry.isPersisted(),
                    inLibrary = entry.favorite,
                    singleProfile = singleProfile,
                )
            }
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
        requirePairContext(intent.source, intent.target)
        validatePair(intent.source, intent.target)?.let {
            return EntryMigrationTargetRefreshResult.Rejected(it)
        }
        return refreshTarget(
            target = intent.target,
            fetchDetails = intent.fetchDetails,
            fetchChildren = intent.fetchChildren,
        )
    }

    override suspend fun prepare(intent: EntryMigrationPrepareIntent): EntryMigrationPreparationResult {
        requirePairContext(intent.source, intent.target)
        validatePair(intent.source, intent.target)?.let { return preparationRejected(it) }
        return try {
            when (
                val inspection = preparationHost.profile(intent.source.profileId)
                    .inspectPair(intent.source.id, intent.target.id)
            ) {
                is EntryMigrationHostInspectionResult.Ready -> prepare(inspection, intent)
                EntryMigrationHostInspectionResult.SourceMissing -> {
                    evaluation.requireMigrationInspectionContext(
                        sourceType = intent.source.type,
                        pairPresent = false,
                        identityStable = false,
                    )
                    preparationRejected(EntryMigrationRejection.UNPERSISTED_ENTRY)
                }
                EntryMigrationHostInspectionResult.TargetMissing -> {
                    evaluation.requireMigrationInspectionContext(
                        sourceType = intent.source.type,
                        pairPresent = false,
                        identityStable = false,
                    )
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
        evaluation.requireMigrationInspectionContext(
            sourceType = intent.source.type,
            pairPresent = true,
            identityStable = identityStable,
        )
        if (!identityStable) {
            return preparationRejected(EntryMigrationRejection.ENTRY_IDENTITY_CHANGED)
        }
        requirePairContext(inspection.source, inspection.target)
        validatePair(inspection.source, inspection.target)?.let { return preparationRejected(it) }
        val hasNotes = inspection.source.notes.isNotBlank()
        val hasCustomCover = inspection.sourceHasCustomCover
        val contributedOptions = optionDiscovery.discover(inspection.source)
        evaluation.requireMigrationOptionContext(
            inspection.source.type,
            EntryMigrationContextualOption.NOTES,
            hasNotes,
        )
        evaluation.requireMigrationOptionContext(
            inspection.source.type,
            EntryMigrationContextualOption.CUSTOM_COVER,
            hasCustomCover,
        )
        val options = buildSet {
            if (inspection.source.type in childStateOptionTypes) {
                add(EntryMigrationOption.CHILD_STATE)
            }
            add(EntryMigrationOption.CATEGORIES)
            if (hasNotes) add(EntryMigrationOption.NOTES)
            if (hasCustomCover) add(EntryMigrationOption.CUSTOM_COVER)
            addAll(contributedOptions)
        }
        val reference = FeatureEntryMigrationReference(
            sessionId = newEntryMigrationSessionId(),
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

    private suspend fun execute(
        reference: FeatureEntryMigrationReference,
        intent: EntryMigrationExecuteIntent,
    ): EntryMigrationExecutionResult {
        val profileId = reference.source.profileId
        val profile = executionHost.profile(profileId)
        val fingerprint = intent.fingerprint()
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
                evaluation.requireMigrationExecutionContext(
                    type = reference.source.type,
                    pairPresent = true,
                    authorizationStable = authorizationStable,
                )
                if (!authorizationStable) {
                    return EntryMigrationExecutionResult.Conflict
                }
                inspection.target
            }
            EntryMigrationHostInspectionResult.SourceMissing,
            EntryMigrationHostInspectionResult.TargetMissing,
            -> {
                evaluation.requireMigrationExecutionContext(
                    type = reference.source.type,
                    pairPresent = false,
                    authorizationStable = false,
                )
                return EntryMigrationExecutionResult.Conflict
            }
            is EntryMigrationHostInspectionResult.OperationalFailure -> {
                return EntryMigrationExecutionResult.OperationalFailure(inspection.retryable)
            }
        }

        when (refreshTarget(target, fetchDetails = true, fetchChildren = true)) {
            EntryMigrationTargetRefreshResult.Refreshed -> Unit
            is EntryMigrationTargetRefreshResult.Rejected -> return EntryMigrationExecutionResult.Conflict
            EntryMigrationTargetRefreshResult.SourceUnavailable,
            EntryMigrationTargetRefreshResult.NoChildren,
            is EntryMigrationTargetRefreshResult.OperationalFailure,
            -> return EntryMigrationExecutionResult.OperationalFailure(retryable = true)
        }
        val inspected = when (
            val result = profile.inspectExecution(reference.source.id, reference.target.id)
        ) {
            is EntryMigrationExecutionInspectionResult.Ready -> result
            EntryMigrationExecutionInspectionResult.SourceMissing,
            EntryMigrationExecutionInspectionResult.TargetMissing,
            -> {
                evaluation.requireMigrationExecutionContext(
                    type = reference.source.type,
                    pairPresent = false,
                    authorizationStable = false,
                )
                return EntryMigrationExecutionResult.Conflict
            }
            is EntryMigrationExecutionInspectionResult.OperationalFailure -> {
                return EntryMigrationExecutionResult.OperationalFailure(result.retryable)
            }
        }
        val authorizationStable = inspected.source.sameMigrationAuthorization(reference.source) &&
            inspected.target.sameMigrationAuthorization(reference.target)
        evaluation.requireMigrationExecutionContext(
            type = reference.source.type,
            pairPresent = true,
            authorizationStable = authorizationStable,
        )
        if (!authorizationStable) {
            return EntryMigrationExecutionResult.Conflict
        }

        val preparedTracks = when (
            val result = transitionPreparation.prepare(
                source = inspected.source,
                target = inspected.target,
                sourceTracks = inspected.sourceTracks,
            )
        ) {
            is EntryMigrationTransitionPreparationResult.Prepared -> result.tracks
            EntryMigrationTransitionPreparationResult.Failed -> {
                return EntryMigrationExecutionResult.OperationalFailure(retryable = true)
            }
        }

        val transferChildren = EntryMigrationOption.CHILD_STATE in intent.selectedOptions
        val childUpdates = if (transferChildren) {
            prepareMigrationChildUpdates(
                sourceChildren = inspected.sourceChildren,
                targetChildren = inspected.targetChildren,
                transferConsumption = inspected.source.type in consumptionTypes,
                transferBookmarks = inspected.source.type in bookmarkTypes,
            )
        } else {
            emptyList()
        }
        val replace = intent.mode == EntryMigrationMode.REPLACE
        val preparedConsequences = durableConsequences.prepare(
            EntryMigrationDurableEvent(
                operationId = reference.sessionId,
                source = inspected.source,
                target = inspected.target,
                selectedOptions = intent.selectedOptions,
                sourceChildren = inspected.sourceChildren,
                targetChildren = inspected.targetChildren,
            ),
        )
        if (!preparedConsequences.isSuccessful) {
            durableConsequences.discard(preparedConsequences.envelopes)
            return EntryMigrationExecutionResult.OperationalFailure(retryable = true)
        }
        val consequenceRequests = preparedConsequences.envelopes.map { envelope ->
            mihon.entry.interactions.host.EntryMigrationConsequenceRequest(
                participantId = envelope.participant.value,
                schemaVersion = envelope.schemaVersion,
                payload = envelope.payload,
            )
        }
        val targetUpdate = inspected.target.copy(
            favorite = true,
            chapterFlags = inspected.source.chapterFlags,
            dateAdded = if (replace) inspected.source.dateAdded else clockMillis(),
            notes = if (EntryMigrationOption.NOTES in intent.selectedOptions) {
                inspected.source.notes
            } else {
                inspected.target.notes
            },
        )
        val transition = EntryMigrationHostTransition(
            operationId = reference.sessionId,
            intentFingerprint = fingerprint,
            profileId = profileId,
            mode = intent.mode,
            expectedSource = inspected.source,
            expectedTarget = inspected.target,
            expectedSourceCategoryIds = inspected.sourceCategoryIds
                .takeIf { EntryMigrationOption.CATEGORIES in intent.selectedOptions },
            expectedTargetChildren = inspected.targetChildren.takeIf { transferChildren },
            sourceUpdate = inspected.source.copy(favorite = false, dateAdded = 0L).takeIf { replace },
            targetUpdate = targetUpdate,
            targetCategoryIds = inspected.sourceCategoryIds
                .takeIf { EntryMigrationOption.CATEGORIES in intent.selectedOptions },
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
                runCatching { durableConsequences.discard(preparedConsequences.envelopes) }
            }
            throw error
        }
        if (result !is EntryMigrationHostTransitionResult.Applied) {
            durableConsequences.discard(preparedConsequences.envelopes)
        }
        return when (result) {
            is EntryMigrationHostTransitionResult.Applied -> {
                val followUp = if (result.hasPendingConsequences) {
                    consequences.deliverOperation(reference.sessionId)
                } else {
                    EntryMigrationFollowUp.COMPLETE
                }
                applied(reference, intent.mode, followUp)
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
    ): EntryMigrationTargetRefreshResult {
        return when (
            val result = sourceRefresh.refresh(
                EntrySourceRefreshRequest(
                    entry = target,
                    fetchDetails = fetchDetails,
                    fetchChildren = fetchChildren,
                    manual = false,
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

    private fun requirePairContext(source: Entry, target: Entry) {
        if (source.type !in migrationTypes) return
        evaluation.requireMigrationPairContext(
            sourceType = source.type,
            sourcePersisted = source.isPersisted(),
            sourceInLibrary = source.favorite,
            targetPersisted = target.isPersisted(),
            sameProfile = source.profileId == target.profileId,
            sameType = source.type == target.type,
            distinctEntry = source.id != target.id,
        )
    }

    private fun Entry.sourceRejection(): EntryMigrationRejection? {
        if (!isPersisted()) return EntryMigrationRejection.UNPERSISTED_ENTRY
        if (!favorite) return EntryMigrationRejection.SOURCE_NOT_IN_LIBRARY
        if (type !in migrationTypes) return EntryMigrationRejection.UNSUPPORTED_SOURCE_TYPE
        return null
    }

    private fun Entry.isPersisted(): Boolean = id > 0L && profileId > 0L

    private fun Entry.subject() = EntryMigrationSubject(profileId, id)

    private fun EntryMigrationExecuteIntent.fingerprint(): String {
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
