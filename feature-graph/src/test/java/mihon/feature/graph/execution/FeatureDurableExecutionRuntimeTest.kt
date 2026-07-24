package mihon.feature.graph

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

class FeatureDurableExecutionRuntimeTest {

    private val pointOwner = ContributionOwner("example.coordinator")
    private val participantOwner = ContributionOwner("example.participant")

    @Test
    fun `durable participants prepare opaque envelopes and deliver through their owning binding`() = runSuspend {
        val point = point()
        val participant = participant("example.durable", point)
        val graph = graph(point, listOf(participant))
        val delivered = mutableListOf<FeatureDurableExecutionPayload>()
        val runtime = runtime(
            graph,
            binding(
                participant = participant,
                prepare = { event -> FeatureDurableExecutionPayload(schemaVersion = 2, value = event.value) },
                deliver = delivered::add,
            ),
        )

        val result = runtime.prepareDurable(point, ContentTypeId("subject"), Event("persisted"))

        result.execution.completedParticipants shouldContainExactly listOf(participant.id)
        result.envelopes shouldContainExactly listOf(
            FeatureDurableExecutionEnvelope(
                participant = participant.id,
                schemaVersion = 2,
                payload = "persisted",
            ),
        )
        runtime.deliverDurable(result.envelopes.single())
        delivered shouldContainExactly listOf(FeatureDurableExecutionPayload(2, "persisted"))
    }

    @Test
    fun `new durable participant is selected without coordinator routing changes`() = runSuspend {
        val point = point()
        val initial = participant("example.initial-durable", point)
        val discovered = participant("example.discovered-durable", point)
        val graph = graph(point, listOf(initial, discovered))
        val runtime = runtime(
            graph,
            binding(initial, prepare = { FeatureDurableExecutionPayload(1, "initial") }),
            binding(discovered, prepare = { FeatureDurableExecutionPayload(1, "discovered") }),
        )

        val result = runtime.prepareDurable(point, ContentTypeId("subject"), Event("event"))

        result.envelopes.map { it.participant } shouldContainExactly listOf(discovered.id, initial.id)
    }

    @Test
    fun `durable preparation can be discarded by the participant owner`() = runSuspend {
        val point = point()
        val participant = participant("example.staged", point)
        val graph = graph(point, listOf(participant))
        val discarded = mutableListOf<FeatureDurableExecutionPayload>()
        val runtime = runtime(
            graph,
            binding(
                participant = participant,
                prepare = { FeatureDurableExecutionPayload(1, "stage") },
                discard = discarded::add,
            ),
        )
        val envelope = runtime.prepareDurable(point, ContentTypeId("subject"), Event("event")).envelopes.single()

        runtime.discardDurable(listOf(envelope)) shouldBe emptyList()
        discarded shouldContainExactly listOf(FeatureDurableExecutionPayload(1, "stage"))
    }

    @Test
    fun `runtime rejects missing duplicate and wrong-delivery durable bindings`() {
        val point = point()
        val participant = participant("example.durable", point)
        val graph = graph(point, listOf(participant))

        shouldThrow<IllegalStateException> {
            FeatureExecutionRuntime(graph, evaluateFeatureGraph(graph), emptyList())
        }.message shouldContain "Durable execution participant binding coverage mismatch"

        shouldThrow<IllegalArgumentException> {
            FeatureExecutionParticipantBinding(participant, FeatureExecutionHandler { })
        }.message shouldContain "requires a durable runtime binding"

        val binding = binding(participant)
        shouldThrow<IllegalStateException> {
            runtime(graph, binding, binding)
        }.message shouldContain "Duplicate durable execution participant bindings"
    }

    @Test
    fun `unknown persisted durable participant remains an explicit delivery failure`() = runSuspend {
        val point = point()
        val participant = participant("example.current", point)
        val graph = graph(point, listOf(participant))
        val runtime = runtime(graph, binding(participant))

        shouldThrow<IllegalArgumentException> {
            runtime.deliverDurable(
                FeatureDurableExecutionEnvelope(
                    participant = FeatureExecutionParticipantId("example.future"),
                    schemaVersion = 1,
                    payload = "opaque",
                ),
            )
        }.message shouldContain "No durable execution participant binding for example.future"
    }

    private fun point(): DurableFeatureExecutionPointDefinition<Event> = durableFeatureExecutionPointDefinition(
        id = FeatureExecutionPointId("example.durable-point"),
        owner = pointOwner,
        failurePolicy = FeatureExecutionFailurePolicy.FAIL_FAST,
    )

    private fun participant(
        id: String,
        point: FeatureExecutionPointDefinition<Event>,
    ): FeatureExecutionParticipantDefinition<Event> = FeatureExecutionParticipantDefinition(
        id = FeatureExecutionParticipantId(id),
        owner = participantOwner,
        point = point,
        behavioralContracts = listOf(ExampleContract),
    )

    private fun graph(
        point: FeatureExecutionPointDefinition<Event>,
        participants: List<FeatureExecutionParticipantDefinition<Event>>,
    ): FeatureGraph {
        val typeOwner = ContributionOwner("subject.type")
        return discoverAndAssembleFeatureGraph(
            listOf(
                featureGraphContributor(typeOwner) {
                    add(ContentTypeContribution(ContentTypeId("subject"), typeOwner, emptyList()))
                },
                featureGraphContributor(pointOwner) { add(point) },
                featureGraphContributor(participantOwner) { participants.forEach(::add) },
            ),
        )
    }

    private fun runtime(
        graph: FeatureGraph,
        vararg bindings: FeatureDurableExecutionParticipantBinding<*>,
    ): FeatureExecutionRuntime = FeatureExecutionRuntime(
        graph = graph,
        evaluation = evaluateFeatureGraph(graph),
        bindings = emptyList(),
        durableBindings = bindings.toList(),
    )

    private fun binding(
        participant: FeatureExecutionParticipantDefinition<Event>,
        prepare: suspend (Event) -> FeatureDurableExecutionPayload? = { null },
        deliver: suspend (FeatureDurableExecutionPayload) -> Unit = {},
        discard: suspend (FeatureDurableExecutionPayload) -> Unit = {},
    ): FeatureDurableExecutionParticipantBinding<Event> = FeatureDurableExecutionParticipantBinding(
        definition = participant,
        preparer = FeatureDurableExecutionPreparer(prepare),
        deliveryHandler = FeatureDurableExecutionDeliveryHandler(deliver),
        discardHandler = FeatureDurableExecutionDiscardHandler(discard),
    )

    private fun runSuspend(block: suspend () -> Unit) {
        kotlinx.coroutines.test.runTest { block() }
    }

    private data class Event(val value: String)

    private object ExampleContract : FeatureBehaviorContract {
        override val id = FeatureArtifactId("example.durable-execution-behavior")
    }
}
