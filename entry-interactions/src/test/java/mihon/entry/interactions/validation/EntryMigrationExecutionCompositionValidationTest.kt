package mihon.entry.interactions.validation

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.ENTRY_MIGRATION_DURABLE_EXECUTION_POINT
import mihon.entry.interactions.ENTRY_MIGRATION_FEATURE_OWNER
import mihon.entry.interactions.EntryInteractionPlugin
import mihon.entry.interactions.EntryInteractionProviderBinding
import mihon.entry.interactions.EntryMigrationDurableConsequences
import mihon.entry.interactions.EntryMigrationDurableEvent
import mihon.entry.interactions.createEntryInteractionComposition
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureDurableExecutionParticipantBinding
import mihon.feature.graph.FeatureDurableExecutionPayload
import mihon.feature.graph.FeatureExecutionParticipantDefinition
import mihon.feature.graph.FeatureExecutionParticipantId
import mihon.feature.graph.featureGraphContributor
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry

class EntryMigrationExecutionCompositionValidationTest {
    @Test
    fun `an unknown owner joins Migration through contribution and binding alone`() = runTest {
        val type = EntryType.entries.first()
        val owner = ContributionOwner("test.future-migration-owner")
        val contract = object : FeatureBehaviorContract {
            override val id = FeatureArtifactId("test.future-migration.behavior")
        }
        val participant = FeatureExecutionParticipantDefinition(
            id = FeatureExecutionParticipantId("test.future-migration"),
            owner = owner,
            point = ENTRY_MIGRATION_DURABLE_EXECUTION_POINT,
            behavioralContracts = listOf(contract),
        )
        val delivered = mutableListOf<String>()
        val composition = createEntryInteractionComposition(
            plugins = listOf(
                object : EntryInteractionPlugin {
                    override val type = type
                    override val owner = ContributionOwner("test.book")
                    override val providerBindings = emptyList<EntryInteractionProviderBinding<*>>()
                },
            ),
            featureContributors = listOf(
                featureGraphContributor(ENTRY_MIGRATION_FEATURE_OWNER) {
                    add(ENTRY_MIGRATION_DURABLE_EXECUTION_POINT)
                },
                featureGraphContributor(owner) { add(participant) },
            ),
            durableExecutionBindings = listOf(
                FeatureDurableExecutionParticipantBinding(
                    definition = participant,
                    preparer = { FeatureDurableExecutionPayload(4, "opaque-future-state") },
                    deliveryHandler = { payload -> delivered += payload.value },
                ),
            ),
        )
        val source = Entry.create().copy(id = 1L, profileId = 1L, type = type)
        val event = EntryMigrationDurableEvent(
            operationId = "future-operation",
            source = source,
            target = source.copy(id = 2L),
            selectedOptions = emptySet(),
            sourceChildren = emptyList(),
            targetChildren = emptyList(),
        )
        val consequences = EntryMigrationDurableConsequences(composition.featureExecutions)

        val prepared = consequences.prepare(event)
        prepared.envelopes.map { it.participant.value to it.schemaVersion } shouldContainExactly
            listOf("test.future-migration" to 4)
        consequences.deliver(prepared.envelopes.single())

        delivered shouldBe listOf("opaque-future-state")
    }
}
