package mihon.feature.graph.execution

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import kotlinx.coroutines.test.runTest
import mihon.feature.graph.ContentTypeContribution
import mihon.feature.graph.ContentTypeId
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureExecutionParticipantId
import mihon.feature.graph.FeatureExecutionPointId
import mihon.feature.graph.FeatureGraph
import mihon.feature.graph.FeatureSubjectId
import mihon.feature.graph.discoverAndAssembleFeatureGraph
import mihon.feature.graph.evaluateFeatureGraph
import mihon.feature.graph.featureGraphContributor
import org.junit.jupiter.api.Test
import java.util.concurrent.CancellationException

class FeatureExecutionRuntimeTest {

    private val pointOwner = ContributionOwner("example.coordinator")
    private val participantOwner = ContributionOwner("example.participant")

    @Test
    fun `participant ordering is deterministic and honors explicit dependencies`() = runSuspend {
        val point = point()
        val last = participant(
            id = "example.alpha",
            point = point,
            order = FeatureExecutionOrder(after = setOf(FeatureExecutionParticipantId("example.zeta"))),
        )
        val first = participant("example.zeta", point)
        val independent = participant("example.middle", point)
        val graph = graph(listOf(point), listOf(last, first, independent))
        val calls = mutableListOf<String>()
        val runtime = runtime(
            graph,
            binding(last) { calls += last.id.value },
            binding(first) { calls += first.id.value },
            binding(independent) { calls += independent.id.value },
        )

        val result = runtime.executeInline(
            point,
            FeatureSubjectId.EntryContentType(ContentTypeId("subject")),
            Event("event"),
        )

        result.selectedParticipants shouldContainExactly listOf(independent.id, first.id, last.id)
        calls shouldContainExactly result.selectedParticipants.map { it.value }
    }

    @Test
    fun `participant cancellation escapes execution instead of becoming a reported failure`() = runSuspend {
        val point = point()
        val participant = participant("example.cancelled", point)
        val graph = graph(listOf(point), listOf(participant))
        val runtime = runtime(
            graph,
            binding(participant) { throw CancellationException("cancelled") },
        )

        shouldThrow<CancellationException> {
            runtime.executeInline(point, FeatureSubjectId.EntryContentType(ContentTypeId("subject")), Event("event"))
        }
    }

    private fun point(): InlineFeatureExecutionPointDefinition<Event> = inlineFeatureExecutionPointDefinition(
        id = FeatureExecutionPointId("example.point"),
        owner = pointOwner,
        failurePolicy = FeatureExecutionFailurePolicy.CONTINUE_AND_REPORT,
    )

    private fun participant(
        id: String,
        point: FeatureExecutionPointDefinition<Event>,
        order: FeatureExecutionOrder = FeatureExecutionOrder(),
    ): FeatureExecutionParticipantDefinition<Event> {
        return FeatureExecutionParticipantDefinition(
            id = FeatureExecutionParticipantId(id),
            owner = participantOwner,
            point = point,
            order = order,
            behavioralContracts = listOf(ExampleContract),
        )
    }

    private fun graph(
        points: List<FeatureExecutionPointDefinition<*>>,
        participants: List<FeatureExecutionParticipantDefinition<*>>,
    ): FeatureGraph {
        val typeOwner = ContributionOwner("subject.type")
        return discoverAndAssembleFeatureGraph(
            listOf(
                featureGraphContributor(typeOwner) {
                    add(ContentTypeContribution(ContentTypeId("subject"), typeOwner, emptyList()))
                },
                featureGraphContributor(pointOwner) { points.forEach(::add) },
                featureGraphContributor(participantOwner) { participants.forEach(::add) },
            ),
        )
    }

    private fun runtime(
        graph: FeatureGraph,
        vararg bindings: FeatureExecutionParticipantBinding<*>,
    ): FeatureExecutionRuntime =
        FeatureExecutionRuntime(graph, evaluateFeatureGraph(graph), bindings.toList())

    private fun binding(
        participant: FeatureExecutionParticipantDefinition<Event>,
        handler: suspend (Event) -> Unit,
    ): FeatureExecutionParticipantBinding<Event> {
        return FeatureExecutionParticipantBinding(
            definition = participant,
            handler = FeatureExecutionHandler(handler),
        )
    }

    private fun runSuspend(block: suspend () -> Unit) {
        runTest { block() }
    }

    private data class Event(val value: String)

    private object ExampleContract : FeatureBehaviorContract {
        override val id = FeatureArtifactId("example.execution-behavior")
    }
}
