package mihon.entry.interactions

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import mihon.entry.interactions.host.EntryMergeConsequenceRequest
import mihon.entry.interactions.host.EntryMergeHost
import mihon.entry.interactions.host.EntryMergeHostExpectation
import mihon.entry.interactions.host.EntryMergeHostExpectedEntry
import mihon.entry.interactions.host.EntryMergeHostMemberKey
import mihon.entry.interactions.host.EntryMergeHostPreparation
import mihon.entry.interactions.host.EntryMergeHostTransition
import mihon.entry.interactions.host.EntryMergeHostTransitionResult
import mihon.entry.interactions.host.EntryMergeMembershipSnapshot
import mihon.feature.graph.FeatureGraphEvaluation
import tachiyomi.domain.entry.model.Entry
import java.util.UUID

internal class EntryMergeWorkflowCoordinator(
    private val evaluation: FeatureGraphEvaluation,
    private val host: EntryMergeHost,
    private val durableConsequences: EntryMergeDurablePreparationGateway,
    private val consequences: EntryMergeConsequenceDelivery,
) : EntryMergeFeature {
    private val applicableTypes = evaluation.mergeTypes(
        ENTRY_MERGE_BASE_INTEGRATION_ID,
        EntryMergeBaseBehavior.WORKFLOW_COORDINATION.id,
    )
    override suspend fun prepare(intent: EntryMergePrepareIntent): EntryMergePreparationResult {
        if (intent.selectedEntries.isEmpty()) return rejected(EntryMergeRejection.EMPTY_SELECTION)
        val selectedTypes = intent.selectedEntries.mapTo(mutableSetOf(), Entry::type)
        val homogeneousType = selectedTypes.size == 1
        val homogeneousProfile = intent.selectedEntries.map(Entry::profileId).distinct().size == 1
        evaluation.requireMergeSelectionContext(
            selectedTypes.filterTo(mutableSetOf()) { it in applicableTypes },
            homogeneousType,
            homogeneousProfile,
        )
        if (!homogeneousType) {
            return rejected(EntryMergeRejection.MIXED_ENTRY_TYPES)
        }
        if (!homogeneousProfile) {
            return rejected(EntryMergeRejection.MIXED_PROFILES)
        }

        val type = intent.selectedEntries.first().type
        val profileId = intent.selectedEntries.first().profileId
        check(type in applicableTypes) { "Entry type $type was not composed into the Merge feature" }
        val profile = host.profile(profileId)
        val resolvedSelected = buildList {
            intent.selectedEntries.forEach { selected ->
                val resolved = if (selected.id > 0L) {
                    val authoritative = profile.entries(listOf(selected.id)).singleOrNull() ?: return@buildList
                    if (contentIdentity(authoritative) != contentIdentity(selected)) return@buildList
                    authoritative
                } else {
                    profile.resolveEntryIdentity(selected) ?: selected
                }
                add(resolved)
            }
        }
        val authoritativeSelectionPresent = resolvedSelected.size == intent.selectedEntries.size
        val typeStable = authoritativeSelectionPresent && resolvedSelected.all { it.type == type }
        val profileStable = authoritativeSelectionPresent && resolvedSelected.all { it.profileId == profileId }
        evaluation.requireMergeAuthorityContext(type, authoritativeSelectionPresent, typeStable, profileStable)
        if (!authoritativeSelectionPresent) return rejected(EntryMergeRejection.ENTRY_NOT_IN_EDITOR)
        if (resolvedSelected.map(::contentIdentity).distinct().size != resolvedSelected.size) {
            return rejected(EntryMergeRejection.DUPLICATE_ENTRIES)
        }
        if (!typeStable) return rejected(EntryMergeRejection.MIXED_ENTRY_TYPES)
        if (!profileStable) return rejected(EntryMergeRejection.MIXED_PROFILES)

        val selectedMemberships = resolvedSelected.map { entry ->
            entry.takeIf { it.id > 0L }?.let { profile.membership(it.id) }
        }
        val existingGroups = selectedMemberships.filterNotNull().distinctBy { it.targetEntryId }
        if (existingGroups.size > 1) {
            evaluation.requireMergeMembershipContext(
                type,
                singleExistingGroup = false,
                completeExistingGroup = true,
                sufficientEditorMembers = true,
            )
            return rejected(EntryMergeRejection.MULTIPLE_EXISTING_GROUPS)
        }

        val existingGroup = existingGroups.singleOrNull()
        val existingMembers = existingGroup?.let { profile.entries(it.orderedEntryIds) }.orEmpty()
        if (existingGroup != null && existingMembers.map(Entry::id) != existingGroup.orderedEntryIds) {
            evaluation.requireMergeMembershipContext(
                type,
                singleExistingGroup = true,
                completeExistingGroup = false,
                sufficientEditorMembers = true,
            )
            return EntryMergePreparationResult.Rejected(EntryMergeRejection.ENTRY_NOT_IN_EDITOR)
        }

        val preparations = intent.preparations.associateBy { preparation -> contentIdentity(preparation.entry) }
        if (preparations.size != intent.preparations.size) {
            return rejected(EntryMergeRejection.DUPLICATE_ENTRIES)
        }
        val existingMemberIds = existingGroup?.orderedEntryIds.orEmpty().toSet()
        val orderedEntries = buildList {
            resolvedSelected.forEach { selected ->
                val entriesAtSelection = if (selected.id in existingMemberIds) existingMembers else listOf(selected)
                entriesAtSelection.forEach { entry ->
                    if (none { contentIdentity(it) == contentIdentity(entry) }) add(entry)
                }
            }
        }
        val sufficientEditorMembers = orderedEntries.size >= 2
        evaluation.requireMergeMembershipContext(
            type,
            singleExistingGroup = true,
            completeExistingGroup = true,
            sufficientEditorMembers = sufficientEditorMembers,
        )
        if (!sufficientEditorMembers) return rejected(EntryMergeRejection.TOO_FEW_ENTRIES)
        val selectedIdentities = resolvedSelected.mapTo(mutableSetOf(), ::contentIdentity)
        val sessionId = newEntryMergeSessionId()
        val expectedEntries = mutableListOf<EntryMergeHostExpectedEntry>()
        val hostPreparations = mutableListOf<EntryMergeHostPreparation>()
        val editorEntries = linkedMapOf<EntryMergeHostMemberKey, EntryMergeEditorEntry>()

        orderedEntries.forEach { entry ->
            val identity = contentIdentity(entry)
            val persisted = entry.id > 0L
            val preparation = preparations[identity]
            if ((!persisted || !entry.favorite) && identity in selectedIdentities && preparation == null) {
                return rejected(EntryMergeRejection.PREPARATION_MISSING)
            }
            val key = newEntryMergeMemberKey()
            val membership = if (persisted) profile.membership(entry.id) else null
            val reference = FeatureEntryMergeEditorEntryReference(sessionId, key)
            val existingMember = existingGroup?.orderedEntryIds?.contains(entry.id) == true
            val editorEntry = EntryMergeEditorEntry(
                reference = reference,
                entry = entry,
                origin = when {
                    existingMember -> EntryMergeEditorEntryOrigin.EXISTING_MEMBER
                    persisted -> EntryMergeEditorEntryOrigin.SELECTED
                    else -> EntryMergeEditorEntryOrigin.NEW_MEMBER
                },
                removable = existingMember,
                removableFromLibrary = persisted && entry.favorite,
            )
            editorEntries[key] = editorEntry
            expectedEntries += EntryMergeHostExpectedEntry(
                key = key,
                entry = entry,
                persistedEntryId = entry.id.takeIf { persisted },
                membership = membership,
            )
            preparation?.let {
                hostPreparations += EntryMergeHostPreparation(key, it.entry, it.categoryIds.distinct())
            }
        }

        val targetEntryId = existingGroup?.targetEntryId ?: resolvedSelected.first().id
        val target = editorEntries.values.firstOrNull { it.entry.id == targetEntryId }?.reference
            ?: editorEntries.values.first().reference
        val editReference = FeatureEntryMergeEditReference(
            sessionId = sessionId,
            profileId = profileId,
            type = type,
            expectation = EntryMergeHostExpectation(type, expectedEntries),
            preparations = hostPreparations,
            entries = editorEntries,
        )
        return EntryMergePreparationResult.Ready(
            EntryMergeEditorProjection(
                editReference = editReference,
                profileId = profileId,
                type = type,
                entries = editorEntries.values.toList(),
                target = target,
                targetLocked = false,
            ),
        )
    }

    override fun observeExisting(entry: Entry): Flow<EntryMergeEditorProjection?> {
        if (entry.id <= 0L) return kotlinx.coroutines.flow.flowOf(null)
        return host.profile(entry.profileId).observeMembership(entry.id).mapLatest { membership ->
            if (membership == null) {
                null
            } else {
                when (val result = prepare(EntryMergePrepareIntent(listOf(entry)))) {
                    is EntryMergePreparationResult.Ready -> result.editor
                    is EntryMergePreparationResult.Rejected -> null
                }
            }
        }
    }

    override suspend fun execute(intent: EntryMergeWorkflowIntent): EntryMergeExecutionResult {
        return when (intent) {
            is EntryMergeCommitIntent -> commit(intent)
            is EntryMergeDissolveIntent -> dissolve(intent)
            is EntryMergeRemoveEntriesIntent -> removeEntries(intent)
        }
    }

    private suspend fun commit(intent: EntryMergeCommitIntent): EntryMergeExecutionResult {
        val edit = intent.editReference as? FeatureEntryMergeEditReference
            ?: return rejectedExecution(EntryMergeRejection.UNRECOGNIZED_EDIT_REFERENCE)
        val target = intent.target.toKeyOrNull(edit)
            ?: return rejectedExecution(EntryMergeRejection.UNRECOGNIZED_EDIT_REFERENCE)
        val ordered = intent.orderedEntries.toKeysOrNull(edit)
            ?: return rejectedExecution(EntryMergeRejection.UNRECOGNIZED_EDIT_REFERENCE)
        val removed = intent.removedEntries.toKeysOrNull(edit)?.toSet()
            ?: return rejectedExecution(EntryMergeRejection.UNRECOGNIZED_EDIT_REFERENCE)
        val libraryRemoved = intent.libraryRemovalEntries.toKeysOrNull(edit)?.toSet()
            ?: return rejectedExecution(EntryMergeRejection.UNRECOGNIZED_EDIT_REFERENCE)
        if (ordered.distinct().size != ordered.size || ordered.toSet() != edit.entries.keys) {
            return rejectedExecution(EntryMergeRejection.INVALID_ORDER)
        }
        if (removed.any { it !in edit.entries } || libraryRemoved.any { it !in edit.entries }) {
            return rejectedExecution(EntryMergeRejection.ENTRY_NOT_IN_EDITOR)
        }
        if (removed.intersect(libraryRemoved).isNotEmpty()) {
            return rejectedExecution(EntryMergeRejection.INVALID_ORDER)
        }
        if (target in removed || target in libraryRemoved) {
            return rejectedExecution(EntryMergeRejection.TARGET_REMOVED)
        }
        if ((removed + libraryRemoved).any { edit.entries.getValue(it).removable.not() }) {
            return rejectedExecution(EntryMergeRejection.ENTRY_NOT_IN_EDITOR)
        }
        if (libraryRemoved.any { edit.entries.getValue(it).removableFromLibrary.not() }) {
            return rejectedExecution(EntryMergeRejection.ENTRY_NOT_IN_EDITOR)
        }

        val operationId = UUID.randomUUID().toString()
        val durablePreparations = edit.preparations.map { preparation ->
            EntryMergeDurablePreparation(
                target = EntryMergeConsequenceTarget.EditorMember(preparation.key),
                event = EntryMergeDurableEvent(
                    operationId = operationId,
                    entry = preparation.entry,
                    changes = setOf(EntryMergeDurableChange.ADDED_TO_LIBRARY),
                    downloadRemovalRequested = false,
                ),
            )
        } + libraryRemoved.map { key ->
            EntryMergeDurablePreparation(
                target = EntryMergeConsequenceTarget.EditorMember(key),
                event = EntryMergeDurableEvent(
                    operationId = operationId,
                    entry = edit.entries.getValue(key).entry,
                    changes = setOf(
                        EntryMergeDurableChange.REMOVED_FROM_LIBRARY,
                        EntryMergeDurableChange.REMOVED_FROM_GROUP,
                    ),
                    downloadRemovalRequested = false,
                ),
            )
        }
        val consequenceRequests = when (val result = durableConsequences.prepare(durablePreparations)) {
            is EntryMergeDurablePreparationResult.Prepared -> result.requests
            EntryMergeDurablePreparationResult.Failed -> {
                return EntryMergeExecutionResult.OperationalFailure(retryable = true)
            }
        }
        return apply(
            operationId = operationId,
            profileId = edit.profileId,
            transition = EntryMergeHostTransition.CommitEditor(
                operationId = operationId,
                profileId = edit.profileId,
                expected = edit.expectation,
                preparations = edit.preparations,
                target = target,
                orderedEntries = ordered,
                removedEntries = removed,
                libraryRemovalEntries = libraryRemoved,
                consequenceRequests = consequenceRequests,
            ),
            preparedConsequences = consequenceRequests,
        )
    }

    private suspend fun dissolve(intent: EntryMergeDissolveIntent): EntryMergeExecutionResult {
        val profile = host.profile(intent.subject.profileId)
        val expected = profile.membership(intent.subject.entryId)
            ?: return applied(intent.subject, EntryMergeFollowUp.COMPLETE)
        return changeExistingGroup(
            subject = intent.subject,
            expected = expected,
            replacementIds = emptyList(),
            visibleEntryId = intent.subject.entryId,
            libraryRemovalIds = emptySet(),
            removeDownloads = false,
        )
    }

    private suspend fun removeEntries(intent: EntryMergeRemoveEntriesIntent): EntryMergeExecutionResult {
        if (intent.entryIds.isEmpty()) return applied(intent.subject, EntryMergeFollowUp.COMPLETE)
        val profile = host.profile(intent.subject.profileId)
        val expected = profile.membership(intent.subject.entryId)
            ?: return applied(intent.subject, EntryMergeFollowUp.COMPLETE)
        if (!expected.orderedEntryIds.containsAll(intent.entryIds)) {
            return rejectedExecution(EntryMergeRejection.ENTRY_NOT_IN_EDITOR)
        }
        val replacementIds = expected.orderedEntryIds.filterNot(intent.entryIds::contains)
        return changeExistingGroup(
            subject = intent.subject,
            expected = expected,
            replacementIds = replacementIds,
            visibleEntryId = replacementIds.firstOrNull(),
            libraryRemovalIds = intent.entryIds.takeIf { intent.removeFromLibrary }.orEmpty(),
            removeDownloads = intent.removeDownloads || intent.removeFromLibrary,
        )
    }

    private suspend fun changeExistingGroup(
        subject: EntryMergeSubject,
        expected: EntryMergeMembershipSnapshot,
        replacementIds: List<Long>,
        visibleEntryId: Long?,
        libraryRemovalIds: Set<Long>,
        removeDownloads: Boolean,
    ): EntryMergeExecutionResult {
        val entries = host.profile(subject.profileId).entries(expected.orderedEntryIds)
        val completeOrderedMembership = entries.map(Entry::id) == expected.orderedEntryIds
        val memberTypes = entries.mapTo(mutableSetOf(), Entry::type)
        val homogeneousMembershipType = completeOrderedMembership && memberTypes.size == 1
        evaluation.requireMergeExistingGroupContext(
            memberTypes.filterTo(mutableSetOf()) { it in applicableTypes },
            completeOrderedMembership,
            homogeneousMembershipType,
        )
        if (!completeOrderedMembership || !homogeneousMembershipType) return EntryMergeExecutionResult.Conflict
        val type = memberTypes.single()
        check(type in applicableTypes) { "Entry type $type was not composed into the Merge feature" }
        val operationId = UUID.randomUUID().toString()
        val entriesById = entries.associateBy(Entry::id)
        val removedEntryIds = expected.orderedEntryIds.toSet() - replacementIds
        val durablePreparations = removedEntryIds.map { entryId ->
            EntryMergeDurablePreparation(
                target = EntryMergeConsequenceTarget.PersistedEntry(entryId),
                event = EntryMergeDurableEvent(
                    operationId = operationId,
                    entry = entriesById.getValue(entryId),
                    changes = buildSet {
                        add(EntryMergeDurableChange.REMOVED_FROM_GROUP)
                        if (entryId in libraryRemovalIds) {
                            add(EntryMergeDurableChange.REMOVED_FROM_LIBRARY)
                        }
                    },
                    downloadRemovalRequested = removeDownloads,
                ),
            )
        }
        val consequenceRequests = when (val result = durableConsequences.prepare(durablePreparations)) {
            is EntryMergeDurablePreparationResult.Prepared -> result.requests
            EntryMergeDurablePreparationResult.Failed -> {
                return EntryMergeExecutionResult.OperationalFailure(retryable = true)
            }
        }
        val replacementTarget = when {
            replacementIds.size < 2 -> null
            expected.targetEntryId in replacementIds -> expected.targetEntryId
            else -> replacementIds.first()
        }
        return apply(
            operationId = operationId,
            profileId = subject.profileId,
            transition = EntryMergeHostTransition.ChangeExistingGroup(
                operationId = operationId,
                profileId = subject.profileId,
                expected = expected,
                replacementTargetEntryId = replacementTarget,
                replacementOrderedEntryIds = replacementIds.takeIf { it.size > 1 }.orEmpty(),
                visibleEntryId = replacementTarget ?: visibleEntryId,
                libraryRemovalEntryIds = libraryRemovalIds,
                consequenceRequests = consequenceRequests,
            ),
            preparedConsequences = consequenceRequests,
        )
    }

    private suspend fun apply(
        operationId: String,
        profileId: Long,
        transition: EntryMergeHostTransition,
        preparedConsequences: List<EntryMergeConsequenceRequest>,
    ): EntryMergeExecutionResult {
        val result = try {
            host.profile(profileId).applyTransition(transition)
        } catch (error: CancellationException) {
            withContext(NonCancellable) {
                runCatching { durableConsequences.discard(preparedConsequences) }
            }
            throw error
        }
        if (result !is EntryMergeHostTransitionResult.Applied) {
            durableConsequences.discard(preparedConsequences)
        }
        return when (result) {
            is EntryMergeHostTransitionResult.Applied -> {
                val followUp = consequences.deliverOperation(operationId)
                applied(result.visibleEntryId?.let { EntryMergeSubject(profileId, it) }, followUp)
            }
            EntryMergeHostTransitionResult.Conflict -> EntryMergeExecutionResult.Conflict
            is EntryMergeHostTransitionResult.OperationalFailure -> {
                EntryMergeExecutionResult.OperationalFailure(result.retryable)
            }
        }
    }

    private fun EntryMergeEditorEntryReference.toKeyOrNull(
        edit: FeatureEntryMergeEditReference,
    ): EntryMergeHostMemberKey? = try {
        requireFeatureReference(edit).key
    } catch (_: UnrecognizedEntryMergeReferenceException) {
        null
    }

    private fun Collection<EntryMergeEditorEntryReference>.toKeysOrNull(
        edit: FeatureEntryMergeEditReference,
    ): List<EntryMergeHostMemberKey>? = map { it.toKeyOrNull(edit) ?: return null }

    private fun contentIdentity(entry: Entry): String =
        "${entry.profileId}:${entry.type}:${entry.source}:${entry.url}"

    private fun rejected(reason: EntryMergeRejection) = EntryMergePreparationResult.Rejected(reason)

    private fun rejectedExecution(reason: EntryMergeRejection) = EntryMergeExecutionResult.Rejected(reason)

    private fun applied(subject: EntryMergeSubject?, followUp: EntryMergeFollowUp) =
        EntryMergeExecutionResult.Applied(EntryMergeWorkflowOutcome(subject, followUp))
}
