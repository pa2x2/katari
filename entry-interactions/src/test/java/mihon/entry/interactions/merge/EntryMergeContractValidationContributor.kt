package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import io.mockk.coEvery
import io.mockk.mockk
import mihon.entry.interactions.host.EntryMergeMembershipSnapshot
import mihon.entry.interactions.validation.contractExpectation
import mihon.entry.interactions.validation.productionSubjectEvaluation
import mihon.entry.interactions.validation.verifyFeatureContract
import mihon.feature.graph.FeatureContractScenarioId
import mihon.feature.graph.contextEvidence
import mihon.feature.graph.validation.FeatureContractExecutionInput
import mihon.feature.graph.validation.FeatureContractReference
import mihon.feature.graph.validation.FeatureContractScenario
import mihon.feature.graph.validation.FeatureContractVerifier
import mihon.feature.graph.validation.FeatureExecutionContractExecutionInput
import mihon.feature.graph.validation.FeatureExecutionContractReference
import mihon.feature.graph.validation.FeatureExecutionContractVerifier
import mihon.feature.graph.validation.FeatureValidationContributionSink
import mihon.feature.graph.validation.FeatureValidationContributor
import tachiyomi.domain.entry.model.Entry

class EntryMergeContractValidationContributor : FeatureValidationContributor {
    override val owner = EntryMergeFeatureContributor.owner

    override fun contributeTo(sink: FeatureValidationContributionSink) {
        sink.addEntryBackupParticipationContractForSubject(
            ENTRY_MERGE_BACKUP_SNAPSHOT_PARTICIPANT,
            EntryMergeBehaviorContract.WORKFLOW,
            EntryMergeBackupMember.serializer(),
            ::backupMemberExample,
        )
        sink.addEntryBackupParticipationContractForSubject(
            ENTRY_MERGE_BACKUP_RESTORE_PARTICIPANT,
            EntryMergeBehaviorContract.WORKFLOW,
            EntryMergeBackupMember.serializer(),
            ::backupMemberExample,
        )
        sink.addEntryBackupParticipationContractForSubject(
            ENTRY_MERGE_BACKUP_FINALIZE_PARTICIPANT,
            EntryMergeBehaviorContract.WORKFLOW,
            EntryMergeBackupMember.serializer(),
            ::backupMemberExample,
        )
        contracts.forEach { item ->
            val reference = FeatureContractReference(ENTRY_MERGE_FEATURE_ID, item.contract)
            sink.add(FeatureContractVerifier(reference) { input -> verifyMerge(input, item.contract) })
            item.scenario?.let { scenario ->
                sink.add(
                    FeatureContractScenario(
                        FeatureContractScenarioId("${item.integration.value}.applicable"),
                        reference,
                        item.integration,
                    ) { scenario() },
                )
            }
        }
        sink.add(
            FeatureExecutionContractVerifier(
                FeatureExecutionContractReference(
                    ENTRY_MERGE_LIBRARY_REMOVAL_PARTICIPANT.id,
                    EntryMergeBehaviorContract.LIBRARY_REMOVAL_PARTICIPATION,
                ),
                verification = ::verifyLibraryRemovalParticipation,
            ),
        )
        sink.add(
            FeatureExecutionContractVerifier(
                FeatureExecutionContractReference(
                    ENTRY_MERGE_DESTRUCTIVE_REMOVAL_PARTICIPANT.id,
                    EntryMergeBehaviorContract.DESTRUCTIVE_REMOVAL_PARTICIPATION,
                ),
                verification = ::verifyLibraryRemovalParticipation,
            ),
        )
        listOf(
            ENTRY_MERGE_PROFILE_MOVE_PREPARATION_PARTICIPANT,
            ENTRY_MERGE_PROFILE_MOVE_DESTINATION_PARTICIPANT,
            ENTRY_MERGE_PROFILE_MOVING_PARTICIPANT,
            ENTRY_MERGE_PROFILE_STATE_MOVED_PARTICIPANT,
        ).forEach { participant ->
            sink.add(
                FeatureExecutionContractVerifier(
                    FeatureExecutionContractReference(
                        participant.id,
                        EntryMergeBehaviorContract.PROFILE_MOVE_PARTICIPATION,
                    ),
                    verification = ::verifyProfileMoveParticipation,
                ),
            )
        }
    }

    private suspend fun verifyLibraryRemovalParticipation(
        input: FeatureExecutionContractExecutionInput,
    ) = verifyFeatureContract {
        val type = EntryType.entries.single { it.toContentTypeId() == input.subject.contentType }
        val entries = listOf(entry(1L, type), entry(2L, type))
        val membership = EntryMergeMembershipSnapshot(7L, 1L, entries.map(Entry::id))
        val host = RecordingEntryMergeHost(entries, listOf(membership))

        val result = EntryMergeLibraryLifecycleCoordinator(host).entriesRemovedFromLibrary(listOf(entries.last()))

        contractExpectation(
            result == EntryMergeLibraryRemovalResult(changedGroupCount = 1, unresolvedGroupCount = 0),
            "Library removal must update Merge membership transactionally",
        )
    }

    private suspend fun verifyProfileMoveParticipation(
        input: FeatureExecutionContractExecutionInput,
    ) = verifyFeatureContract {
        val type = EntryType.entries.single { it.toContentTypeId() == input.subject.contentType }
        val entries = listOf(entry(1L, type), entry(2L, type))
        val host = RecordingEntryMergeHost(
            entries,
            listOf(EntryMergeMembershipSnapshot(7L, 1L, entries.map(Entry::id))),
        )
        val feature = EntryMergeProfileMoveCoordinator(host)
        val prepared = feature.prepare(7L, listOf(1L)) as EntryMergeProfileMovePreparationResult.Ready
        val inspected = feature.inspectDestination(prepared.reference, 9L, emptyList())
            as EntryMergeProfileMoveDestinationResult.Ready
        val intent = EntryMergeProfileMoveIntent(
            inspected.reference,
            9L,
            mapOf(1L to 1L, 2L to 2L),
            emptySet(),
        )
        contractExpectation(
            feature.begin(intent) == EntryMergeProfileMoveExecutionResult.Applied &&
                feature.complete(intent) == EntryMergeProfileMoveExecutionResult.Applied,
            "Merge must preserve complete groups across Profile movement",
        )
    }

    private suspend fun verifyMerge(
        input: FeatureContractExecutionInput,
        contract: EntryMergeBehaviorContract,
    ) = verifyFeatureContract {
        val type = EntryType.entries.single { it.toContentTypeId() == input.subject.contentType }
        val entries = listOf(entry(1L, type), entry(2L, type))
        val membership = EntryMergeMembershipSnapshot(7L, 1L, entries.map(Entry::id))
        when (contract) {
            EntryMergeBehaviorContract.DOWNLOAD_OWNERSHIP -> {
                input.provider(EntryDownloadCapability.definition)
                val owners = EntryMergeDownloadOwnershipCoordinator(
                    RecordingEntryMergeHost(entries, listOf(membership)),
                )
                    .resolveDownloadOwners(EntryMergeSubject(7L, 2L))
                contractExpectation(owners.orderedOwners == entries, "Merge must project ordered download owners")
            }
            EntryMergeBehaviorContract.MIGRATION_REPLACEMENT -> {
                input.provider(EntryMigrationCapability.definition)
                val host = RecordingEntryMergeHost(entries, listOf(membership))
                val result = EntryMergeMigrationCoordinator(host).participateInReplacementTransaction(
                    EntryMergeMigrationReplacementIntent(entries.first(), entry(3L, type)),
                )
                contractExpectation(
                    result == EntryMergeMigrationReplacementResult.Applied,
                    "Merge must participate in migration replacement",
                )
            }
            EntryMergeBehaviorContract.EXISTING_GROUP -> {
                val host = RecordingEntryMergeHost(entries, listOf(membership))
                val result = workflow(type, host).execute(
                    EntryMergeRemoveEntriesIntent(EntryMergeSubject(7L, 1L), setOf(2L), false, false),
                )
                contractExpectation(result is EntryMergeExecutionResult.Applied, "Merge must mutate an existing group")
            }
            else -> {
                val ready = workflow(type, RecordingEntryMergeHost(entries))
                    .prepare(EntryMergePrepareIntent(entries))
                contractExpectation(
                    ready is EntryMergePreparationResult.Ready,
                    "Merge must prepare its shared workflow",
                )
            }
        }
    }

    private fun workflow(
        type: EntryType,
        host: RecordingEntryMergeHost,
    ): EntryMergeFeature {
        val evaluation = productionSubjectEvaluation(
            type = type,
            feature = EntryMergeFeatureContributor,
            additionalContributors = listOf(
                EntryTrackingMergeContributor,
                EntryMergeCustomCoverContributor,
                EntryDownloadMergeContributor,
            ),
        )
        val durable = mockk<EntryMergeDurableConsequences> {
            coEvery { prepare(any()) } returns EntryMergeDurablePreparationResult.Prepared(emptyList())
        }
        return EntryMergeWorkflowCoordinator(
            evaluation,
            host,
            durable,
            EntryMergeConsequenceDelivery(host, durable),
        )
    }

    private fun entry(id: Long, type: EntryType) = Entry.create().copy(
        id = id,
        profileId = 7L,
        source = 10L + id,
        url = "/$id",
        title = "Entry $id",
        favorite = true,
        type = type,
    )

    private data class Contract(
        val integration: mihon.feature.graph.FeatureIntegrationId,
        val contract: EntryMergeBehaviorContract,
        val scenario: (() -> List<mihon.feature.graph.ContextEvidence<*>>)? = null,
    )

    private companion object {
        val contracts = listOf(
            Contract(ENTRY_MERGE_BASE_INTEGRATION_ID, EntryMergeBehaviorContract.WORKFLOW),
            Contract(ENTRY_MERGE_DOWNLOAD_INTEGRATION_ID, EntryMergeBehaviorContract.DOWNLOAD_OWNERSHIP),
            Contract(ENTRY_MERGE_MIGRATION_INTEGRATION_ID, EntryMergeBehaviorContract.MIGRATION_REPLACEMENT),
            Contract(ENTRY_MERGE_SELECTION_CONTEXT_INTEGRATION, EntryMergeBehaviorContract.PREPARATION_SELECTION) {
                listOf(
                    contextEvidence(ENTRY_MERGE_HOMOGENEOUS_TYPE_CONTEXT, true),
                    contextEvidence(ENTRY_MERGE_HOMOGENEOUS_PROFILE_CONTEXT, true),
                )
            },
            Contract(ENTRY_MERGE_AUTHORITY_CONTEXT_INTEGRATION, EntryMergeBehaviorContract.PREPARATION_AUTHORITY) {
                listOf(
                    contextEvidence(ENTRY_MERGE_AUTHORITATIVE_SELECTION_CONTEXT, true),
                    contextEvidence(ENTRY_MERGE_AUTHORITATIVE_TYPE_CONTEXT, true),
                    contextEvidence(ENTRY_MERGE_AUTHORITATIVE_PROFILE_CONTEXT, true),
                )
            },
            Contract(ENTRY_MERGE_MEMBERSHIP_CONTEXT_INTEGRATION, EntryMergeBehaviorContract.PREPARATION_MEMBERSHIP) {
                listOf(
                    contextEvidence(ENTRY_MERGE_SINGLE_EXISTING_GROUP_CONTEXT, true),
                    contextEvidence(ENTRY_MERGE_COMPLETE_EXISTING_GROUP_CONTEXT, true),
                    contextEvidence(ENTRY_MERGE_SUFFICIENT_EDITOR_MEMBERS_CONTEXT, true),
                )
            },
            Contract(ENTRY_MERGE_EXISTING_GROUP_CONTEXT_INTEGRATION, EntryMergeBehaviorContract.EXISTING_GROUP) {
                listOf(
                    contextEvidence(ENTRY_MERGE_COMPLETE_ORDERED_MEMBERSHIP_CONTEXT, true),
                    contextEvidence(ENTRY_MERGE_HOMOGENEOUS_MEMBERSHIP_TYPE_CONTEXT, true),
                )
            },
        )
    }
}

private fun backupMemberExample(contentType: mihon.feature.graph.ContentTypeId) = EntryMergeBackupMember(
    target = EntryMergeBackupIdentity(
        1,
        "/target",
        EntryType.entries.single { it.toContentTypeId() == contentType },
    ),
    position = 0,
)
