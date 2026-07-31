package mihon.feature.graph.execution

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.test.runTest
import mihon.feature.graph.ApplicationSubjectContribution
import mihon.feature.graph.ContentTypeId
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureExecutionParticipantId
import mihon.feature.graph.FeatureExecutionPointId
import mihon.feature.graph.FeatureGraph
import mihon.feature.graph.FeatureSubjectId
import mihon.feature.graph.FeatureSubjectScope
import mihon.feature.graph.discoverAndAssembleFeatureGraph
import mihon.feature.graph.evaluateFeatureGraph
import mihon.feature.graph.featureGraphContributor
import org.junit.jupiter.api.Test

class ApplicationFeatureExecutionTest {

    private val applicationOwner = ContributionOwner("example.application")
    private val pointOwner = ContributionOwner("example.coordinator")
    private val participantOwner = ContributionOwner("example.participant")

    @Test
    fun `inline execution can target the application subject`() = runTest {
        val point = inlineFeatureExecutionPointDefinition<Event>(
            id = FeatureExecutionPointId("example.inline"),
            owner = pointOwner,
            failurePolicy = FeatureExecutionFailurePolicy.FAIL_FAST,
            subjectScope = FeatureSubjectScope.Application,
        )
        val participant = participant("example.inline-participant", point)
        val graph = graph(point, participant)
        val events = mutableListOf<Event>()
        val runtime = FeatureExecutionRuntime(
            graph = graph,
            evaluation = evaluateFeatureGraph(graph),
            bindings = listOf(
                FeatureExecutionParticipantBinding(
                    definition = participant,
                    handler = FeatureExecutionHandler(events::add),
                ),
            ),
        )

        val result = runtime.executeInline(point, FeatureSubjectId.Application, Event("inline"))

        result.affectedSubject shouldBe FeatureSubjectId.Application
        result.completedParticipants shouldContainExactly listOf(participant.id)
        events shouldContainExactly listOf(Event("inline"))

        shouldThrow<IllegalStateException> {
            runtime.executeInline(
                point,
                FeatureSubjectId.EntryContentType(ContentTypeId("book")),
                Event("wrong-scope"),
            )
        }.message shouldContain "targets Application, not EntryContentType"
    }

    @Test
    fun `durable preparation can target the application subject`() = runTest {
        val point = durableFeatureExecutionPointDefinition<Event>(
            id = FeatureExecutionPointId("example.durable"),
            owner = pointOwner,
            failurePolicy = FeatureExecutionFailurePolicy.FAIL_FAST,
            subjectScope = FeatureSubjectScope.Application,
        )
        val participant = participant("example.durable-participant", point)
        val graph = graph(point, participant)
        val runtime = FeatureExecutionRuntime(
            graph = graph,
            evaluation = evaluateFeatureGraph(graph),
            bindings = emptyList(),
            durableBindings = listOf(
                FeatureDurableExecutionParticipantBinding(
                    definition = participant,
                    preparer = FeatureDurableExecutionPreparer {
                        FeatureDurableExecutionPayload(schemaVersion = 1, value = it.value)
                    },
                    deliveryHandler = FeatureDurableExecutionDeliveryHandler { },
                ),
            ),
        )

        val result = runtime.prepareDurable(point, FeatureSubjectId.Application, Event("durable"))

        result.execution.affectedSubject shouldBe FeatureSubjectId.Application
        result.envelopes.map { it.payload } shouldContainExactly listOf("durable")
    }

    @Test
    fun `transactional and volatile execution can target the application subject`() = runTest {
        val transactionalPoint = transactionalFeatureExecutionPointDefinition<Event>(
            id = FeatureExecutionPointId("example.transactional"),
            owner = pointOwner,
            failurePolicy = FeatureExecutionFailurePolicy.FAIL_FAST,
            subjectScope = FeatureSubjectScope.Application,
        )
        val volatilePoint = afterCommitVolatileFeatureExecutionPointDefinition<Event>(
            id = FeatureExecutionPointId("example.volatile"),
            owner = pointOwner,
            failurePolicy = FeatureExecutionFailurePolicy.FAIL_FAST,
            subjectScope = FeatureSubjectScope.Application,
        )
        val transactionalParticipant = participant("example.transactional-participant", transactionalPoint)
        val volatileParticipant = participant("example.volatile-participant", volatilePoint)
        val graph = graph(
            points = listOf(transactionalPoint, volatilePoint),
            participants = listOf(transactionalParticipant, volatileParticipant),
        )
        val events = mutableListOf<Event>()
        val runtime = FeatureExecutionRuntime(
            graph = graph,
            evaluation = evaluateFeatureGraph(graph),
            bindings = listOf(transactionalParticipant, volatileParticipant).map { participant ->
                FeatureExecutionParticipantBinding(
                    definition = participant,
                    handler = FeatureExecutionHandler(events::add),
                )
            },
        )

        runtime.coordinateFeatureCommit(
            commit = {
                callback {
                    execute(transactionalPoint, FeatureSubjectId.Application, Event("transactional"))
                }.invoke()
                true
            },
            committed = { it },
            volatileConsequences = {
                execute(volatilePoint, FeatureSubjectId.Application, Event("volatile"))
            },
        )

        events shouldContainExactly listOf(Event("transactional"), Event("volatile"))
    }

    private fun <E : Any> participant(
        id: String,
        point: FeatureExecutionPointDefinition<E>,
    ) = FeatureExecutionParticipantDefinition(
        id = FeatureExecutionParticipantId(id),
        owner = participantOwner,
        point = point,
        behavioralContracts = listOf(ExecutionContract),
    )

    private fun graph(
        point: FeatureExecutionPointDefinition<Event>,
        participant: FeatureExecutionParticipantDefinition<Event>,
    ): FeatureGraph = graph(listOf(point), listOf(participant))

    private fun graph(
        points: List<FeatureExecutionPointDefinition<Event>>,
        participants: List<FeatureExecutionParticipantDefinition<Event>>,
    ): FeatureGraph = discoverAndAssembleFeatureGraph(
        listOf(
            featureGraphContributor(applicationOwner) {
                add(ApplicationSubjectContribution(applicationOwner))
            },
            featureGraphContributor(pointOwner) { points.forEach(::add) },
            featureGraphContributor(participantOwner) { participants.forEach(::add) },
        ),
    )

    private data class Event(val value: String)

    private object ExecutionContract : FeatureBehaviorContract {
        override val id = FeatureArtifactId("example.execution-contract")
    }
}
