package mihon.entry.interactions

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import mihon.entry.interactions.validation.contractExpectation
import mihon.entry.interactions.validation.productionSubjectEvaluation
import mihon.entry.interactions.validation.verifyFeatureContract
import mihon.feature.graph.FeatureContractScenarioId
import mihon.feature.graph.contextEvidence
import mihon.feature.graph.validation.FeatureContractReference
import mihon.feature.graph.validation.FeatureContractVerifier
import mihon.feature.graph.validation.FeatureExecutionContractReference
import mihon.feature.graph.validation.FeatureExecutionContractScenario
import mihon.feature.graph.validation.FeatureExecutionContractVerifier
import mihon.feature.graph.validation.FeatureValidationContributionSink
import mihon.feature.graph.validation.FeatureValidationContributor
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.repository.EntryProgressRepository

class EntryProgressContractValidationContributor : FeatureValidationContributor {
    override val owner = EntryProgressFeatureContributor.owner

    override fun contributeTo(sink: FeatureValidationContributionSink) {
        sink.addEntryBackupParticipationContract(
            ENTRY_PROGRESS_BACKUP_SNAPSHOT_PARTICIPANT,
            EntryProgressBehaviorContract,
            EntryProgressSnapshot.serializer(),
            EntryProgressSnapshot(),
        )
        sink.add(
            FeatureExecutionContractVerifier(
                FeatureExecutionContractReference(
                    ENTRY_PROGRESS_MIGRATION_PARTICIPANT.id,
                    EntryProgressMigrationDurableBehaviorContract,
                ),
            ) { input ->
                verifyFeatureContract {
                    val type = input.provider(EntryProgressCapability.definition).type
                    val source = Entry.create().copy(id = 53L, type = type)
                    val target = source.copy(id = 54L)
                    val payload = EntryProgressMigrationPayload(target, EntryProgressSnapshot())
                    val feature = mockk<EntryProgressFeature> {
                        coEvery { prepareMigration(source, target, any()) } returns
                            EntryProgressMigrationPreparation.Prepared(payload)
                        coEvery { applyMigration(payload) } returns EntryProgressRestoreResult.Applied
                    }
                    val binding = entryProgressMigrationBinding { feature }
                    val prepared = binding.preparer.prepare(
                        EntryMigrationDurableEvent("contract", source, target, emptySet(), emptyList(), emptyList()),
                    )
                    contractExpectation(prepared != null, "Progress must prepare a durable Migration payload")
                    binding.deliveryHandler.deliver(requireNotNull(prepared))
                    coVerify(exactly = 1) { feature.applyMigration(payload) }
                }
            },
        )
        sink.addEntryBackupParticipationContract(
            ENTRY_PROGRESS_BACKUP_RESTORE_PARTICIPANT,
            EntryProgressBehaviorContract,
            EntryProgressSnapshot.serializer(),
            EntryProgressSnapshot(),
        )
        sink.add(
            FeatureContractVerifier(
                FeatureContractReference(ENTRY_PROGRESS_FEATURE_ID, EntryProgressBehaviorContract),
            ) { input ->
                verifyFeatureContract {
                    val provider = input.provider(EntryProgressCapability.definition)
                    val evaluation = productionSubjectEvaluation(
                        EntryProgressCapability.bind(provider),
                        EntryProgressFeatureContributor,
                    )
                    val entry = Entry.create().copy(id = 53L, type = provider.type)
                    val snapshot = EntryProgressSnapshot()
                    val restored = mutableListOf<EntryProgressSnapshot>()
                    val feature = DefaultEntryProgressFeature(
                        evaluation = evaluation,
                        interaction = object : EntryProgressInteraction {
                            override suspend fun snapshot(entry: Entry): EntryProgressSnapshot = snapshot
                            override suspend fun restore(entry: Entry, snapshot: EntryProgressSnapshot) {
                                restored += snapshot
                            }

                            override suspend fun copy(
                                sourceEntry: Entry,
                                targetEntry: Entry,
                                resourceMappings: List<EntryProgressResourceMapping>,
                            ) = Unit
                        },
                        repository = mockk(relaxed = true),
                        getEntryWithChapters = mockk(relaxed = true),
                        globalLibraryPreferences = mockk(relaxed = true),
                    )

                    contractExpectation(feature.isApplicable(provider.type), "Progress transfer must be applicable")
                    contractExpectation(
                        feature.snapshot(entry) == EntryProgressSnapshotResult.Available(snapshot),
                        "Progress transfer must expose its snapshot",
                    )
                    contractExpectation(
                        feature.restore(entry, snapshot) == EntryProgressRestoreResult.Applied,
                        "Progress transfer must apply restoration",
                    )
                    contractExpectation(restored == listOf(snapshot), "Progress transfer restored the wrong snapshot")
                }
            },
        )
        val mediaFeatureReference = FeatureContractReference(
            ENTRY_PROGRESS_FEATURE_ID,
            EntryProgressMediaSessionBehaviorContract,
        )
        sink.add(
            FeatureContractVerifier(mediaFeatureReference) { input ->
                verifyFeatureContract {
                    val progressProvider = input.provider(EntryProgressCapability.definition)
                    val mediaProvider = input.provider(EntryMediaSessionCapability.definition)
                    val event = mediaSessionContractEvent(progressProvider.type)
                    val repository = mockk<EntryProgressRepository> {
                        coEvery { get(any(), any(), any()) } returns null
                        coEvery { mergeAndSyncChild(event.progress) } returns event.progress
                    }
                    val feature = DefaultEntryProgressFeature(
                        evaluation = productionSubjectEvaluation(
                            listOf(
                                EntryProgressCapability.bind(progressProvider),
                                EntryMediaSessionCapability.bind(mediaProvider),
                            ),
                            EntryProgressFeatureContributor,
                        ),
                        interaction = mockk(relaxed = true),
                        repository = repository,
                        getEntryWithChapters = mockk(relaxed = true),
                        globalLibraryPreferences = mockk(relaxed = true),
                    )

                    val result = feature.recordMediaProgress(event)

                    contractExpectation(result.state == event.progress, "Progress must persist Media Session state")
                    contractExpectation(result.completedNow, "Progress must report a newly completed child")
                    coVerify(exactly = 1) { repository.mergeAndSyncChild(event.progress) }
                }
            },
        )
        val mediaExecutionReference = FeatureExecutionContractReference(
            ENTRY_PROGRESS_MEDIA_SESSION_PARTICIPANT.id,
            EntryProgressMediaSessionBehaviorContract,
        )
        sink.add(
            FeatureExecutionContractVerifier(mediaExecutionReference) { input ->
                verifyFeatureContract {
                    val event = mediaSessionContractEvent(
                        input.provider(EntryProgressCapability.definition).type,
                    )
                    val expected = EntryProgressRecordingResult(event.progress, completedNow = true)
                    val feature = mockk<EntryProgressFeature> {
                        coEvery { recordMediaProgress(event) } returns expected
                    }
                    val execution = EntryMediaSessionExecutionEvent(event)

                    entryProgressMediaSessionBinding { feature }.handler.execute(execution)

                    contractExpectation(
                        execution.progressResult == expected,
                        "Progress participation must publish the authoritative recording result",
                    )
                }
            },
        )
        sink.add(
            FeatureExecutionContractScenario(
                FeatureContractScenarioId("entry.progress.media-session.execution.applicable"),
                mediaExecutionReference,
            ) { listOf(contextEvidence(ENTRY_MEDIA_SESSION_PROGRESS_ALLOWED, true)) },
        )
    }
}
