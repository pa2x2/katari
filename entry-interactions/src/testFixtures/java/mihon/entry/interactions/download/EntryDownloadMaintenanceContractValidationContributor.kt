package mihon.entry.interactions

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import mihon.entry.interactions.validation.contractExpectation
import mihon.entry.interactions.validation.productionSubjectEvaluation
import mihon.entry.interactions.validation.verifyFeatureContract
import mihon.feature.graph.validation.FeatureContractReference
import mihon.feature.graph.validation.FeatureContractVerifier
import mihon.feature.graph.validation.FeatureExecutionContractExecutionInput
import mihon.feature.graph.validation.FeatureExecutionContractReference
import mihon.feature.graph.validation.FeatureExecutionContractVerifier
import mihon.feature.graph.validation.FeatureValidationContributionSink
import mihon.feature.graph.validation.FeatureValidationContributor
import tachiyomi.domain.entry.model.Entry

class EntryDownloadMaintenanceContractValidationContributor : FeatureValidationContributor {
    override val owner = EntryDownloadMaintenanceFeatureContributor.owner

    override fun contributeTo(sink: FeatureValidationContributionSink) {
        sink.add(
            FeatureContractVerifier(
                FeatureContractReference(
                    ENTRY_DOWNLOAD_MAINTENANCE_FEATURE_ID,
                    EntryDownloadMaintenanceBehaviorContract,
                ),
            ) { input ->
                verifyFeatureContract {
                    val provider = input.provider(EntryDownloadCapability.definition)
                    val evaluation = productionSubjectEvaluation(
                        EntryDownloadCapability.bind(provider),
                        EntryDownloadMaintenanceFeatureContributor,
                    )
                    val entry = Entry.create().copy(id = 75L, type = provider.type)
                    var cacheInvalidated = false
                    val interaction = recordingDownloadInteraction()
                    every { interaction.invalidateCaches() } answers { cacheInvalidated = true }
                    val ownership = mockk<EntryMergeDownloadOwnershipProjection> {
                        coEvery { resolveDownloadOwners(any()) } returns EntryMergeDownloadOwners(
                            profileId = entry.profileId,
                            visibleEntryId = entry.id,
                            orderedOwners = listOf(entry),
                        )
                    }
                    val feature = DefaultEntryDownloadMaintenanceFeature(evaluation, interaction, ownership)

                    contractExpectation(
                        feature.invalidateCaches() == EntryDownloadMaintenanceResult.Performed,
                        "Download Maintenance must accept a participating provider",
                    )
                    contractExpectation(
                        cacheInvalidated,
                        "Download Maintenance must dispatch cache invalidation through its shared boundary",
                    )
                }
            },
        )
        sink.add(
            FeatureExecutionContractVerifier(
                FeatureExecutionContractReference(
                    ENTRY_DOWNLOAD_LIBRARY_REMOVAL_PARTICIPANT.id,
                    EntryDownloadLibraryRemovalBehaviorContract,
                ),
                verification = ::verifyLibraryRemovalInspection,
            ),
        )
        sink.add(
            FeatureExecutionContractVerifier(
                FeatureExecutionContractReference(
                    ENTRY_DOWNLOAD_MIGRATION_OPTION_PARTICIPANT.id,
                    EntryDownloadMigrationOptionBehaviorContract,
                ),
            ) { input ->
                verifyFeatureContract {
                    val type = input.provider(EntryDownloadCapability.definition).type
                    val source = Entry.create().copy(id = 79L, type = type)
                    val feature = mockk<EntryDownloadMaintenanceFeature> {
                        coEvery { inspectEntry(source) } returns EntryDownloadMaintenanceInspection.HasDownloads
                    }
                    val options = linkedSetOf<EntryMigrationOption>()
                    entryDownloadMigrationOptionBinding { feature }.handler.execute(
                        EntryMigrationOptionDiscoveryEvent(source, options::add),
                    )
                    contractExpectation(
                        EntryMigrationOption.REMOVE_SOURCE_DOWNLOADS in options,
                        "Download Maintenance must contribute its Migration option when downloads exist",
                    )
                }
            },
        )
        sink.add(
            FeatureExecutionContractVerifier(
                FeatureExecutionContractReference(
                    ENTRY_DOWNLOAD_MERGE_PARTICIPANT.id,
                    EntryDownloadMergeDurableBehaviorContract,
                ),
            ) { input ->
                verifyFeatureContract {
                    val type = input.provider(EntryDownloadCapability.definition).type
                    val entry = Entry.create().copy(id = 82L, type = type)
                    val plan = EntryDownloadRemovalPlan(listOf(entry))
                    val feature = mockk<EntryDownloadMaintenanceFeature> {
                        coEvery { prepareRemoval(entry) } returns EntryDownloadRemovalPreparation.Prepared(plan)
                        coEvery { applyRemoval(plan) } returns EntryDownloadMaintenanceResult.Performed
                    }
                    val binding = entryDownloadMergeBinding { feature }
                    val prepared = binding.preparer.prepare(
                        EntryMergeDurableEvent(
                            operationId = "contract",
                            entry = entry,
                            changes = setOf(EntryMergeDurableChange.REMOVED_FROM_GROUP),
                            downloadRemovalRequested = true,
                        ),
                    )
                    contractExpectation(prepared != null, "Download Maintenance must prepare durable Merge removal")
                    binding.deliveryHandler.deliver(requireNotNull(prepared))
                    coVerify(exactly = 1) { feature.applyRemoval(plan) }
                }
            },
        )
        sink.add(
            FeatureExecutionContractVerifier(
                FeatureExecutionContractReference(
                    ENTRY_DOWNLOAD_MIGRATION_PARTICIPANT.id,
                    EntryDownloadMigrationDurableBehaviorContract,
                ),
            ) { input ->
                verifyFeatureContract {
                    val type = input.provider(EntryDownloadCapability.definition).type
                    val source = Entry.create().copy(id = 80L, type = type)
                    val target = source.copy(id = 81L)
                    val plan = EntryDownloadRemovalPlan(listOf(source))
                    val feature = mockk<EntryDownloadMaintenanceFeature> {
                        coEvery { prepareRemoval(source) } returns EntryDownloadRemovalPreparation.Prepared(plan)
                        coEvery { applyRemoval(plan) } returns EntryDownloadMaintenanceResult.Performed
                    }
                    val binding = entryDownloadMigrationBinding { feature }
                    val prepared = binding.preparer.prepare(
                        EntryMigrationDurableEvent(
                            "contract",
                            source,
                            target,
                            setOf(EntryMigrationOption.REMOVE_SOURCE_DOWNLOADS),
                            emptyList(),
                            emptyList(),
                        ),
                    )
                    contractExpectation(prepared != null, "Download Maintenance must prepare durable removal")
                    binding.deliveryHandler.deliver(requireNotNull(prepared))
                    coVerify(exactly = 1) { feature.applyRemoval(plan) }
                }
            },
        )
        sink.add(
            FeatureExecutionContractVerifier(
                FeatureExecutionContractReference(
                    ENTRY_DOWNLOAD_METADATA_CHANGE_PARTICIPANT.id,
                    EntryDownloadMetadataChangeBehaviorContract,
                ),
                verification = ::verifyMetadataChange,
            ),
        )
        listOf(
            ENTRY_DOWNLOAD_DESTRUCTIVE_REMOVAL_PREPARATION_PARTICIPANT,
            ENTRY_DOWNLOAD_DESTRUCTIVE_REMOVAL_PARTICIPANT,
        ).forEach { participant ->
            sink.add(
                FeatureExecutionContractVerifier(
                    FeatureExecutionContractReference(
                        participant.id,
                        EntryDownloadDestructiveRemovalBehaviorContract,
                    ),
                    verification = ::verifyRemovalLifecycle,
                ),
            )
        }
        listOf(
            ENTRY_DOWNLOAD_PROFILE_MOVE_PREPARATION_PARTICIPANT,
            ENTRY_DOWNLOAD_PROFILE_MOVE_PARTICIPANT,
        ).forEach { participant ->
            sink.add(
                FeatureExecutionContractVerifier(
                    FeatureExecutionContractReference(participant.id, EntryDownloadProfileMoveBehaviorContract),
                    verification = ::verifyRemovalLifecycle,
                ),
            )
        }
    }

    private suspend fun verifyLibraryRemovalInspection(
        input: FeatureExecutionContractExecutionInput,
    ) = verifyFeatureContract {
        val provider = input.provider(EntryDownloadCapability.definition)
        val entry = Entry.create().copy(id = 76L, type = provider.type)
        val interaction = recordingDownloadInteraction()
        every { interaction.hasDownloads(entry) } returns true
        val ownership = mockk<EntryMergeDownloadOwnershipProjection> {
            coEvery { resolveDownloadOwners(any()) } returns EntryMergeDownloadOwners(
                profileId = entry.profileId,
                visibleEntryId = entry.id,
                orderedOwners = listOf(entry),
            )
        }
        val feature = DefaultEntryDownloadMaintenanceFeature(
            productionSubjectEvaluation(
                EntryDownloadCapability.bind(provider),
                EntryDownloadMaintenanceFeatureContributor,
            ),
            interaction,
            ownership,
        )

        contractExpectation(
            feature.inspectEntry(entry) == EntryDownloadMaintenanceInspection.HasDownloads,
            "Library removal must report a Download follow-up only when downloads exist",
        )
    }

    private suspend fun verifyMetadataChange(
        input: FeatureExecutionContractExecutionInput,
    ) = verifyFeatureContract {
        val provider = input.provider(EntryDownloadCapability.definition)
        val entry = Entry.create().copy(id = 77L, type = provider.type, title = "Old")
        val interaction = recordingDownloadInteraction()
        val feature = DefaultEntryDownloadMaintenanceFeature(
            productionSubjectEvaluation(
                EntryDownloadCapability.bind(provider),
                EntryDownloadMaintenanceFeatureContributor,
            ),
            interaction,
            mockk(relaxed = true),
        )

        feature.renameEntry(entry, "New")

        coVerify(exactly = 1) { interaction.renameEntry(entry, "New") }
    }

    private suspend fun verifyRemovalLifecycle(
        input: FeatureExecutionContractExecutionInput,
    ) = verifyFeatureContract {
        val provider = input.provider(EntryDownloadCapability.definition)
        val entry = Entry.create().copy(id = 78L, type = provider.type)
        val interaction = recordingDownloadInteraction()
        every { interaction.hasDownloads(entry) } returnsMany listOf(true, false)
        coEvery { interaction.deleteEntryDownloads(entry) } returns true
        val ownership = mockk<EntryMergeDownloadOwnershipProjection> {
            coEvery { resolveDownloadOwners(any()) } returns EntryMergeDownloadOwners(
                profileId = entry.profileId,
                visibleEntryId = entry.id,
                orderedOwners = listOf(entry),
            )
        }
        val feature = DefaultEntryDownloadMaintenanceFeature(
            productionSubjectEvaluation(
                EntryDownloadCapability.bind(provider),
                EntryDownloadMaintenanceFeatureContributor,
            ),
            interaction,
            ownership,
        )
        val preparation = feature.prepareRemoval(entry)
        contractExpectation(
            preparation is EntryDownloadRemovalPreparation.Prepared &&
                feature.applyRemoval(preparation.plan) == EntryDownloadMaintenanceResult.Performed,
            "Lifecycle removal must capture and apply concrete Download ownership",
        )
    }
}
