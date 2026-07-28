package mihon.entry.interactions

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import mihon.entry.interactions.validation.contractExpectation
import mihon.entry.interactions.validation.productionSubjectEvaluation
import mihon.entry.interactions.validation.verifyFeatureContract
import mihon.feature.graph.validation.FeatureContractReference
import mihon.feature.graph.validation.FeatureContractVerifier
import mihon.feature.graph.validation.FeatureExecutionContractReference
import mihon.feature.graph.validation.FeatureExecutionContractVerifier
import mihon.feature.graph.validation.FeatureValidationContributionSink
import mihon.feature.graph.validation.FeatureValidationContributor
import tachiyomi.domain.entry.model.Entry

class EntryPlaybackPreferencesContractValidationContributor : FeatureValidationContributor {
    override val owner = EntryPlaybackPreferencesFeatureContributor.owner

    override fun contributeTo(sink: FeatureValidationContributionSink) {
        sink.addEntryBackupParticipationContract(
            ENTRY_PLAYBACK_PREFERENCES_BACKUP_SNAPSHOT_PARTICIPANT,
            EntryPlaybackPreferencesBehaviorContract,
            EntryPlaybackPreferencesSnapshot.serializer(),
            EntryPlaybackPreferencesSnapshot(dubKey = "dub"),
        )
        sink.add(
            FeatureExecutionContractVerifier(
                FeatureExecutionContractReference(
                    ENTRY_PLAYBACK_PREFERENCES_MIGRATION_PARTICIPANT.id,
                    EntryPlaybackPreferencesMigrationDurableBehaviorContract,
                ),
            ) { input ->
                verifyFeatureContract {
                    val type = input.provider(EntryPlaybackPreferencesCapability.definition).type
                    val source = Entry.create().copy(id = 61L, type = type)
                    val target = source.copy(id = 62L)
                    val payload = EntryPlaybackPreferencesMigrationPayload(
                        target,
                        EntryPlaybackPreferencesSnapshot(dubKey = "contract-dub"),
                    )
                    val feature = mockk<EntryPlaybackPreferencesFeature> {
                        coEvery { prepareMigration(source, target) } returns
                            EntryPlaybackPreferencesMigrationPreparation.Prepared(payload)
                        coEvery { applyMigration(payload) } returns EntryPlaybackPreferencesRestoreResult.Applied
                    }
                    val binding = entryPlaybackPreferencesMigrationBinding { feature }
                    val prepared = binding.preparer.prepare(
                        EntryMigrationDurableEvent("contract", source, target, emptySet(), emptyList(), emptyList()),
                    )
                    contractExpectation(
                        prepared != null,
                        "Playback preferences must prepare a durable Migration payload",
                    )
                    binding.deliveryHandler.deliver(requireNotNull(prepared))
                    coVerify(exactly = 1) { feature.applyMigration(payload) }
                }
            },
        )
        sink.addEntryBackupParticipationContract(
            ENTRY_PLAYBACK_PREFERENCES_BACKUP_RESTORE_PARTICIPANT,
            EntryPlaybackPreferencesBehaviorContract,
            EntryPlaybackPreferencesSnapshot.serializer(),
            EntryPlaybackPreferencesSnapshot(dubKey = "dub"),
        )
        sink.add(
            FeatureContractVerifier(
                FeatureContractReference(
                    ENTRY_PLAYBACK_PREFERENCES_FEATURE_ID,
                    EntryPlaybackPreferencesBehaviorContract,
                ),
            ) { input ->
                verifyFeatureContract {
                    val provider = input.provider(EntryPlaybackPreferencesCapability.definition)
                    val evaluation = productionSubjectEvaluation(
                        EntryPlaybackPreferencesCapability.bind(provider),
                        EntryPlaybackPreferencesFeatureContributor,
                    )
                    val entry = Entry.create().copy(id = 61L, type = provider.type)
                    val snapshot = EntryPlaybackPreferencesSnapshot(dubKey = "contract-dub")
                    val restored = mutableListOf<EntryPlaybackPreferencesSnapshot>()
                    val feature = DefaultEntryPlaybackPreferencesFeature(
                        evaluation = evaluation,
                        interaction = object : EntryPlaybackPreferencesInteraction {
                            override suspend fun snapshot(entry: Entry): EntryPlaybackPreferencesSnapshot = snapshot
                            override suspend fun restore(entry: Entry, snapshot: EntryPlaybackPreferencesSnapshot) {
                                restored += snapshot
                            }

                            override suspend fun copy(sourceEntry: Entry, targetEntry: Entry): Boolean = true
                        },
                    )

                    contractExpectation(
                        feature.isApplicable(provider.type),
                        "Playback-preference transfer must be applicable",
                    )
                    contractExpectation(
                        feature.snapshot(entry) == EntryPlaybackPreferencesSnapshotResult.Captured(snapshot),
                        "Playback-preference transfer must capture its snapshot",
                    )
                    contractExpectation(
                        feature.restore(entry, snapshot) == EntryPlaybackPreferencesRestoreResult.Applied,
                        "Playback-preference transfer must apply restoration",
                    )
                    contractExpectation(
                        restored == listOf(snapshot),
                        "Playback-preference transfer restored the wrong snapshot",
                    )
                }
            },
        )
    }
}
