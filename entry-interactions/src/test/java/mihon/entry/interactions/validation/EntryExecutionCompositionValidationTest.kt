package mihon.entry.interactions.validation

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.EntryInteractionPlugin
import mihon.entry.interactions.EntryInteractionProviderBinding
import mihon.entry.interactions.createEntryInteractionComposition
import mihon.entry.interactions.toContentTypeId
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureDurableExecutionDeliveryHandler
import mihon.feature.graph.FeatureDurableExecutionParticipantBinding
import mihon.feature.graph.FeatureDurableExecutionPayload
import mihon.feature.graph.FeatureDurableExecutionPreparer
import mihon.feature.graph.FeatureExecutionFailurePolicy
import mihon.feature.graph.FeatureExecutionHandler
import mihon.feature.graph.FeatureExecutionParticipantBinding
import mihon.feature.graph.FeatureExecutionParticipantDefinition
import mihon.feature.graph.FeatureExecutionParticipantId
import mihon.feature.graph.FeatureExecutionPointId
import mihon.feature.graph.InlineFeatureExecutionPointDefinition
import mihon.feature.graph.durableFeatureExecutionPointDefinition
import mihon.feature.graph.featureGraphContributor
import mihon.feature.graph.inlineFeatureExecutionPointDefinition
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class EntryExecutionCompositionValidationTest {

    @Test
    fun `composition discovers and binds independently owned execution participants`() = runTest {
        val fixture = fixture()
        val calls = mutableListOf<Long>()
        val composition = createEntryInteractionComposition(
            plugins = listOf(plugin(fixture.type)),
            featureContributors = fixture.contributors,
            executionBindings = listOf(
                FeatureExecutionParticipantBinding(
                    definition = fixture.participant,
                    handler = FeatureExecutionHandler { calls += it.entryId },
                ),
            ),
        )

        val result = composition.featureExecutions.executeInline(
            point = fixture.point,
            contentType = fixture.type.toContentTypeId(),
            event = TestEvent(42L),
        )

        result.completedParticipants shouldContainExactly listOf(fixture.participant.id)
        calls shouldContainExactly listOf(42L)
    }

    @Test
    fun `composition fails when a discovered execution participant has no runtime binding`() {
        val fixture = fixture()

        val exception = assertThrows<IllegalStateException> {
            createEntryInteractionComposition(
                plugins = listOf(plugin(fixture.type)),
                featureContributors = fixture.contributors,
            )
        }

        exception.message.orEmpty() shouldContain "missing: [test.follow-up.record]"
    }

    @Test
    fun `composition discovers durable participants from the same contribution pipeline`() = runTest {
        val type = EntryType.entries.first()
        val pointOwner = ContributionOwner("test.durable-lifecycle")
        val participantOwner = ContributionOwner("test.durable-follow-up")
        val point = durableFeatureExecutionPointDefinition<TestEvent>(
            id = FeatureExecutionPointId("test.durable-lifecycle.changed"),
            owner = pointOwner,
            failurePolicy = FeatureExecutionFailurePolicy.FAIL_FAST,
        )
        val participant = FeatureExecutionParticipantDefinition(
            id = FeatureExecutionParticipantId("test.durable-follow-up.record"),
            owner = participantOwner,
            point = point,
            behavioralContracts = listOf(TestExecutionContract),
        )
        val delivered = mutableListOf<String>()
        val composition = createEntryInteractionComposition(
            plugins = listOf(plugin(type)),
            featureContributors = listOf(
                featureGraphContributor(pointOwner) { add(point) },
                featureGraphContributor(participantOwner) { add(participant) },
            ),
            durableExecutionBindings = listOf(
                FeatureDurableExecutionParticipantBinding(
                    definition = participant,
                    preparer = FeatureDurableExecutionPreparer { event ->
                        FeatureDurableExecutionPayload(1, event.entryId.toString())
                    },
                    deliveryHandler = FeatureDurableExecutionDeliveryHandler { payload ->
                        delivered += payload.value
                    },
                ),
            ),
        )

        val prepared = composition.featureExecutions.prepareDurable(
            point = point,
            contentType = type.toContentTypeId(),
            event = TestEvent(42L),
        )
        composition.featureExecutions.deliverDurable(prepared.envelopes.single())

        delivered shouldContainExactly listOf("42")
    }

    private fun fixture(): Fixture {
        val type = EntryType.entries.first()
        val pointOwner = ContributionOwner("test.lifecycle")
        val participantOwner = ContributionOwner("test.follow-up")
        val point = inlineFeatureExecutionPointDefinition<TestEvent>(
            id = FeatureExecutionPointId("test.lifecycle.changed"),
            owner = pointOwner,
            failurePolicy = FeatureExecutionFailurePolicy.CONTINUE_AND_REPORT,
        )
        val participant = FeatureExecutionParticipantDefinition(
            id = FeatureExecutionParticipantId("test.follow-up.record"),
            owner = participantOwner,
            point = point,
            behavioralContracts = listOf(TestExecutionContract),
        )
        return Fixture(
            type = type,
            point = point,
            participant = participant,
            contributors = listOf(
                featureGraphContributor(pointOwner) { add(point) },
                featureGraphContributor(participantOwner) { add(participant) },
            ),
        )
    }

    private fun plugin(type: EntryType): EntryInteractionPlugin {
        return object : EntryInteractionPlugin {
            override val type = type
            override val owner = ContributionOwner("test.type.${type.name.lowercase()}")
            override val providerBindings = emptyList<EntryInteractionProviderBinding<*>>()
        }
    }

    private data class Fixture(
        val type: EntryType,
        val point: InlineFeatureExecutionPointDefinition<TestEvent>,
        val participant: FeatureExecutionParticipantDefinition<TestEvent>,
        val contributors: List<mihon.feature.graph.FeatureGraphContributor>,
    )

    private data class TestEvent(val entryId: Long)

    private object TestExecutionContract : FeatureBehaviorContract {
        override val id = FeatureArtifactId("test.follow-up.behavior")
    }
}
