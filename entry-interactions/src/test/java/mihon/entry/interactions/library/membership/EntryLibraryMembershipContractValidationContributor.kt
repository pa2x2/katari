package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import mihon.entry.interactions.host.EntryMigrationCustomCoverHost
import mihon.entry.interactions.host.EntryMigrationCustomCoverPayload
import mihon.entry.interactions.validation.contractExpectation
import mihon.entry.interactions.validation.verifyFeatureContract
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureExecutionHandler
import mihon.feature.graph.FeatureExecutionParticipantBinding
import mihon.feature.graph.FeatureExecutionParticipantDefinition
import mihon.feature.graph.FeatureExecutionParticipantId
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.validation.FeatureContractReference
import mihon.feature.graph.validation.FeatureContractVerifier
import mihon.feature.graph.validation.FeatureExecutionContractReference
import mihon.feature.graph.validation.FeatureExecutionContractVerifier
import mihon.feature.graph.validation.FeatureValidationContributionSink
import mihon.feature.graph.validation.FeatureValidationContributor
import tachiyomi.domain.entry.model.Entry

class EntryLibraryMembershipContractValidationContributor : FeatureValidationContributor {
    override val owner = EntryLibraryMembershipFeatureContributor.owner

    override fun contributeTo(sink: FeatureValidationContributionSink) {
        sink.add(
            FeatureContractVerifier(
                FeatureContractReference(
                    ENTRY_LIBRARY_MEMBERSHIP_FEATURE_ID,
                    EntryLibraryMembershipBehaviorContract,
                ),
            ) { input ->
                verifyFeatureContract {
                    val type = EntryType.entries.single { it.toContentTypeId() == input.subject.contentType }
                    val entry = Entry.create().copy(id = 91L, type = type, favorite = false)
                    val host = RecordingLibraryMembershipHost(entry.copy(favorite = true))
                    val feature = membershipFeature(type, host)

                    val result = feature.add(EntryLibraryAddRequest(entry))

                    contractExpectation(
                        result is EntryLibraryAddResult.Added,
                        "Library Membership must commit addition",
                    )
                    contractExpectation(
                        host.addedCategoryIds == emptyList<Long>(),
                        "Library Membership must resolve the automatic default category before persistence",
                    )
                }
            },
        )
    }

    private fun membershipFeature(
        type: EntryType,
        host: EntryLibraryMembershipHost,
    ): EntryLibraryMembershipFeature {
        val plugin = object : EntryInteractionPlugin {
            override val type = type
            override val owner = ContributionOwner("library-membership-contract.${type.name.lowercase()}")
            override val providerBindings = emptyList<EntryInteractionProviderBinding<*>>()
        }
        val participants = ContractExecutionParticipantContributor
        val bindings = participants.definitions.map { definition ->
            FeatureExecutionParticipantBinding(definition, FeatureExecutionHandler { })
        }
        val composition = createEntryInteractionComposition(
            plugins = listOf(plugin),
            featureContributors = listOf(EntryLibraryMembershipFeatureContributor, participants),
            executionBindings = bindings,
        )
        return EntryLibraryMembershipCoordinator(
            host = host,
            mergeCandidates = mockk { coEvery { candidates(any()) } returns emptyList() },
            executions = composition.featureExecutions,
        )
    }
}

class EntryLibraryCustomCoverContractValidationContributor : FeatureValidationContributor {
    override val owner = EntryLibraryCustomCoverContributor.owner

    override fun contributeTo(sink: FeatureValidationContributionSink) {
        sink.add(
            FeatureExecutionContractVerifier(
                FeatureExecutionContractReference(
                    ENTRY_LIBRARY_CUSTOM_COVER_REMOVAL_PARTICIPANT.id,
                    EntryLibraryCustomCoverBehaviorContract,
                ),
            ) {
                verifyFeatureContract {
                    val cleaned = mutableListOf<Long>()
                    val host = EntryLibraryCustomCoverHost { entry -> cleaned += entry.id }
                    host.cleanupAfterLibraryRemoval(Entry.create().copy(id = 92L))
                    contractExpectation(
                        cleaned == listOf(92L),
                        "Library removal must invoke the host-owned custom-cover cleanup",
                    )
                }
            },
        )
        sink.add(
            FeatureExecutionContractVerifier(
                FeatureExecutionContractReference(
                    ENTRY_MERGE_CUSTOM_COVER_PARTICIPANT.id,
                    EntryMergeCustomCoverDurableBehaviorContract,
                ),
            ) { input ->
                verifyFeatureContract {
                    val type = EntryType.entries.single { it.toContentTypeId() == input.subject.contentType }
                    val entry = Entry.create().copy(id = 96L, profileId = 7L, type = type)
                    val cleaned = mutableListOf<Long>()
                    val binding = entryMergeCustomCoverBinding(
                        mergeHost = RecordingEntryMergeHost(listOf(entry)),
                        cleanup = { cleaned += it.id },
                    )
                    val prepared = binding.preparer.prepare(
                        EntryMergeDurableEvent(
                            operationId = "contract",
                            entry = entry,
                            changes = setOf(EntryMergeDurableChange.REMOVED_FROM_LIBRARY),
                            downloadRemovalRequested = false,
                        ),
                    )
                    contractExpectation(prepared != null, "Custom covers must prepare durable Merge cleanup")
                    binding.deliveryHandler.deliver(requireNotNull(prepared))
                    contractExpectation(cleaned == listOf(96L), "Merge must deliver custom-cover cleanup")
                }
            },
        )
        sink.add(
            FeatureExecutionContractVerifier(
                FeatureExecutionContractReference(
                    ENTRY_MIGRATION_CUSTOM_COVER_PARTICIPANT.id,
                    EntryMigrationCustomCoverDurableBehaviorContract,
                ),
            ) { input ->
                verifyFeatureContract {
                    val type = EntryType.entries.single { it.toContentTypeId() == input.subject.contentType }
                    val source = Entry.create().copy(id = 95L, type = type)
                    val target = source.copy(id = 96L)
                    val payload = EntryMigrationCustomCoverPayload("contract-stage", target.id)
                    val host = mockk<EntryMigrationCustomCoverHost>(relaxed = true) {
                        coEvery { stage("contract", source, target) } returns payload
                    }
                    val binding = entryMigrationCustomCoverBinding(host)
                    val prepared = binding.preparer.prepare(
                        EntryMigrationDurableEvent(
                            "contract",
                            source,
                            target,
                            setOf(EntryMigrationOption.CUSTOM_COVER),
                            emptyList(),
                            emptyList(),
                        ),
                    )
                    contractExpectation(prepared != null, "Custom covers must prepare durable Migration state")
                    binding.deliveryHandler.deliver(requireNotNull(prepared))
                    binding.discardHandler.discard(prepared)
                    coVerify(exactly = 1) { host.promote(payload) }
                    coVerify(exactly = 1) { host.discard(payload) }
                }
            },
        )
        sink.add(
            FeatureExecutionContractVerifier(
                FeatureExecutionContractReference(
                    ENTRY_CUSTOM_COVER_DESTRUCTIVE_REMOVAL_PARTICIPANT.id,
                    EntryDestructiveRemovalCustomCoverBehaviorContract,
                ),
            ) {
                verifyFeatureContract {
                    val cleaned = mutableListOf<Long>()
                    val host = EntryDestructiveRemovalCustomCoverHost { entry -> cleaned += entry.id }
                    host.removeCustomCover(Entry.create().copy(id = 93L))
                    contractExpectation(
                        cleaned == listOf(93L),
                        "Destructive removal must invoke custom-cover cleanup",
                    )
                }
            },
        )
        sink.add(
            FeatureExecutionContractVerifier(
                FeatureExecutionContractReference(
                    ENTRY_PROFILE_MOVE_CUSTOM_COVER_PARTICIPANT.id,
                    EntryProfileMoveCustomCoverBehaviorContract,
                ),
            ) {
                verifyFeatureContract {
                    val cleaned = mutableListOf<Long>()
                    val host = EntryProfileMoveCustomCoverHost { entries -> cleaned += entries.map(Entry::id) }
                    host.removeCustomCovers(listOf(Entry.create().copy(id = 94L)))
                    contractExpectation(cleaned == listOf(94L), "Profile movement must clean removed custom covers")
                }
            },
        )
    }
}

private class RecordingLibraryMembershipHost(
    private val addedEntry: Entry,
) : EntryLibraryMembershipHost {
    var addedCategoryIds: List<Long>? = null

    override suspend fun prepareAddition(entry: Entry) = EntryLibraryMembershipPreparation(
        categories = emptyList(),
        defaultCategoryId = 0L,
        selectedCategoryIds = emptySet(),
        defaultChildFlags = 7L,
    )

    override suspend fun add(
        entry: Entry,
        categoryIds: List<Long>,
        defaultChildFlags: Long,
    ): EntryLibraryMembershipCommit {
        addedCategoryIds = categoryIds
        return EntryLibraryMembershipCommit.Applied(listOf(addedEntry))
    }

    override suspend fun remove(
        entries: List<Entry>,
        beforeCommit: suspend (persistedEntries: List<Entry>) -> Unit,
    ): EntryLibraryMembershipCommit = EntryLibraryMembershipCommit.NoChange
}

private object ContractExecutionParticipantContributor : FeatureGraphContributor {
    override val owner = ContributionOwner("library-membership-contract-participants")
    private object Contract : FeatureBehaviorContract {
        override val id = FeatureArtifactId("library-membership-contract-participant")
    }

    val definitions = listOf(
        FeatureExecutionParticipantDefinition(
            id = FeatureExecutionParticipantId("library-membership-contract.added"),
            owner = owner,
            point = ENTRY_LIBRARY_ADDED_EXECUTION_POINT,
            behavioralContracts = listOf(Contract),
        ),
        FeatureExecutionParticipantDefinition(
            id = FeatureExecutionParticipantId("library-membership-contract.removing"),
            owner = owner,
            point = ENTRY_LIBRARY_REMOVING_EXECUTION_POINT,
            behavioralContracts = listOf(Contract),
        ),
        FeatureExecutionParticipantDefinition(
            id = FeatureExecutionParticipantId("library-membership-contract.removed"),
            owner = owner,
            point = ENTRY_LIBRARY_REMOVED_EXECUTION_POINT,
            behavioralContracts = listOf(Contract),
        ),
    )

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        definitions.forEach(sink::add)
    }
}
