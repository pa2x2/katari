package mihon.entry.interactions.validation

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.ENTRY_MERGE_DURABLE_EXECUTION_POINT
import mihon.entry.interactions.ENTRY_MERGE_FEATURE_OWNER
import mihon.entry.interactions.EntryInteractionPlugin
import mihon.entry.interactions.EntryInteractionProviderBinding
import mihon.entry.interactions.EntryMergeConsequenceTarget
import mihon.entry.interactions.EntryMergeDurableChange
import mihon.entry.interactions.EntryMergeDurableConsequences
import mihon.entry.interactions.EntryMergeDurableEvent
import mihon.entry.interactions.EntryMergeDurablePreparation
import mihon.entry.interactions.EntryMergeDurablePreparationResult
import mihon.entry.interactions.createEntryInteractionComposition
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureDurableExecutionEnvelope
import mihon.feature.graph.FeatureDurableExecutionParticipantBinding
import mihon.feature.graph.FeatureDurableExecutionPayload
import mihon.feature.graph.FeatureExecutionParticipantDefinition
import mihon.feature.graph.FeatureExecutionParticipantId
import mihon.feature.graph.featureGraphContributor
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry

class EntryMergeExecutionCompositionValidationTest {
    @Test
    fun `an unknown owner joins Merge through contribution and binding alone`() = runTest {
        val type = EntryType.entries.first()
        val owner = ContributionOwner("test.future-merge-owner")
        val contract = object : FeatureBehaviorContract {
            override val id = FeatureArtifactId("test.future-merge.behavior")
        }
        val participant = FeatureExecutionParticipantDefinition(
            id = FeatureExecutionParticipantId("test.future-merge"),
            owner = owner,
            point = ENTRY_MERGE_DURABLE_EXECUTION_POINT,
            behavioralContracts = listOf(contract),
        )
        val delivered = mutableListOf<String>()
        val composition = createEntryInteractionComposition(
            plugins = listOf(
                object : EntryInteractionPlugin {
                    override val type = type
                    override val owner = ContributionOwner("test.future-merge-type")
                    override val providerBindings = emptyList<EntryInteractionProviderBinding<*>>()
                },
            ),
            featureContributors = listOf(
                featureGraphContributor(ENTRY_MERGE_FEATURE_OWNER) {
                    add(ENTRY_MERGE_DURABLE_EXECUTION_POINT)
                },
                featureGraphContributor(owner) { add(participant) },
            ),
            durableExecutionBindings = listOf(
                FeatureDurableExecutionParticipantBinding(
                    definition = participant,
                    preparer = { FeatureDurableExecutionPayload(6, "opaque-future-state") },
                    deliveryHandler = { payload -> delivered += payload.value },
                ),
            ),
        )
        val entry = Entry.create().copy(id = 7L, profileId = 1L, type = type)
        val consequences = EntryMergeDurableConsequences(composition.featureExecutions)

        val prepared = consequences.prepare(
            listOf(
                EntryMergeDurablePreparation(
                    target = EntryMergeConsequenceTarget.PersistedEntry(entry.id),
                    event = EntryMergeDurableEvent(
                        operationId = "future-operation",
                        entry = entry,
                        changes = setOf(EntryMergeDurableChange.REMOVED_FROM_GROUP),
                        downloadRemovalRequested = false,
                    ),
                ),
            ),
        ) as EntryMergeDurablePreparationResult.Prepared
        prepared.requests.map { it.participantId to it.schemaVersion } shouldContainExactly
            listOf("test.future-merge" to 6)
        consequences.deliver(
            FeatureDurableExecutionEnvelope(
                participant = FeatureExecutionParticipantId(prepared.requests.single().participantId),
                schemaVersion = prepared.requests.single().schemaVersion,
                payload = prepared.requests.single().payload,
            ),
        )

        delivered shouldBe listOf("opaque-future-state")
    }
}
