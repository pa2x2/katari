package mihon.feature.graph

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import java.util.concurrent.CancellationException

class FeatureExecutionRuntimeTest {

    private val contractOwner = ContributionOwner("example.contract")
    private val pointOwner = ContributionOwner("example.coordinator")
    private val participantOwner = ContributionOwner("example.participant")
    private val alpha = capabilityDefinition<AlphaProvider>(CapabilityId("example.alpha"), contractOwner)

    @Test
    fun `discovered participant executes only for content types satisfying its prerequisites`() = runSuspend {
        val point = point()
        val participant = participant(
            id = "example.alpha-participant",
            point = point,
            prerequisites = CapabilityExpression.Provided(alpha),
        )
        val graph = graph(
            points = listOf(point),
            participants = listOf(participant),
            types = listOf(
                contentType("supported", CapabilityProvider(alpha, AlphaProvider())),
                contentType("unsupported"),
            ),
        )
        val events = mutableListOf<String>()
        val runtime = runtime(graph, binding(participant) { events += it.value })

        val supported = runtime.executeInline(point, ContentTypeId("supported"), Event("supported"))
        val unsupported = runtime.executeInline(point, ContentTypeId("unsupported"), Event("unsupported"))

        supported.selectedParticipants shouldContainExactly listOf(participant.id)
        supported.completedParticipants shouldContainExactly listOf(participant.id)
        supported.isSuccessful shouldBe true
        unsupported.selectedParticipants shouldBe emptyList()
        events shouldContainExactly listOf("supported")
    }

    @Test
    fun `new execution participants enter an existing discovery pipeline without assembler edits`() {
        val point = point()
        val participants = mutableListOf(participant("example.initial", point))
        val pointContributor = featureGraphContributor(pointOwner) { add(point) }
        val participantContributor = featureGraphContributor(participantOwner) { participants.forEach(::add) }
        val type = contentType("subject")
        val typeContributor = featureGraphContributor(type.owner) { add(type) }
        val contributors = listOf(typeContributor, pointContributor, participantContributor)

        discoverAndAssembleFeatureGraph(contributors).executionParticipants
            .map { it.id.value } shouldContainExactly listOf("example.initial")

        participants += participant("example.discovered", point)

        discoverAndAssembleFeatureGraph(contributors).executionParticipants
            .map { it.id.value } shouldContainExactly listOf("example.discovered", "example.initial")
    }

    @Test
    fun `runtime rejects missing orphaned and duplicate participant bindings`() {
        val point = point()
        val participant = participant("example.declared", point)
        val graph = graph(listOf(point), listOf(participant))

        shouldThrow<IllegalStateException> {
            runtime(graph)
        }.message shouldContain "missing: [example.declared]"

        val orphan = participant("example.orphan", point)
        shouldThrow<IllegalStateException> {
            runtime(graph, binding(participant) {}, binding(orphan) {})
        }.message shouldContain "orphaned: [example.orphan]"

        shouldThrow<IllegalStateException> {
            runtime(graph, binding(participant) {}, binding(participant) {})
        }.message shouldContain "Duplicate execution participant bindings"
    }

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

        val result = runtime.executeInline(point, ContentTypeId("subject"), Event("event"))

        result.selectedParticipants shouldContainExactly listOf(independent.id, first.id, last.id)
        calls shouldContainExactly result.selectedParticipants.map { it.value }
    }

    @Test
    fun `context evidence is resolved by participant binding without coordinator knowledge`() = runSuspend {
        val point = point()
        val enabled = contextInputDefinition<Boolean>(
            id = ContextInputId("example.enabled"),
            owner = participantOwner,
        )
        val disabled = FeatureContextBlocker(FeatureArtifactId("example.disabled"), listOf(enabled))
        val participant = participant(
            id = "example.contextual",
            point = point,
            contextInputs = listOf(enabled),
            contextRule = featureContextRule(participantOwner) { evidence ->
                if (evidence.value(enabled)) {
                    FeatureContextDecision.Applicable
                } else {
                    FeatureContextDecision.Blocked(listOf(disabled))
                }
            },
            contextBlockers = listOf(disabled),
        )
        val graph = graph(listOf(point), listOf(participant))
        val calls = mutableListOf<String>()
        val runtime = runtime(
            graph,
            FeatureExecutionParticipantBinding(
                definition = participant,
                handler = FeatureExecutionHandler { calls += it.value },
                contextResolver = FeatureExecutionContextResolver { event ->
                    listOf(contextEvidence(enabled, event.enabled))
                },
            ),
        )

        runtime.executeInline(point, ContentTypeId("subject"), Event("disabled", enabled = false))
            .selectedParticipants shouldBe emptyList()
        runtime.executeInline(point, ContentTypeId("subject"), Event("enabled", enabled = true))
            .selectedParticipants shouldContainExactly listOf(participant.id)
        calls shouldContainExactly listOf("enabled")
    }

    @Test
    fun `continue policy reports failures and executes remaining participants`() = runSuspend {
        val point = point(failurePolicy = FeatureExecutionFailurePolicy.CONTINUE_AND_REPORT)
        val failed = participant("example.failed", point)
        val completed = participant("example.completed", point)
        val graph = graph(listOf(point), listOf(failed, completed))
        val runtime = runtime(
            graph,
            binding(failed) { error("failed") },
            binding(completed) {},
        )

        val result = runtime.executeInline(point, ContentTypeId("subject"), Event("event"))

        result.completedParticipants shouldContainExactly listOf(completed.id)
        result.failures.map { it.participant } shouldContainExactly listOf(failed.id)
        result.stoppedEarly shouldBe false
    }

    @Test
    fun `fail fast policy stops after the first failure`() = runSuspend {
        val point = point(failurePolicy = FeatureExecutionFailurePolicy.FAIL_FAST)
        val failed = participant("example.failed", point)
        val skipped = participant("example.skipped", point)
        val graph = graph(listOf(point), listOf(failed, skipped))
        var skippedCalls = 0
        val runtime = runtime(
            graph,
            binding(failed) { error("failed") },
            binding(skipped) { skippedCalls++ },
        )

        val result = runtime.executeInline(point, ContentTypeId("subject"), Event("event"))

        result.failures.map { it.participant } shouldContainExactly listOf(failed.id)
        result.stoppedEarly shouldBe true
        skippedCalls shouldBe 0
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
            runtime.executeInline(point, ContentTypeId("subject"), Event("event"))
        }
    }

    @Test
    fun `assembly rejects undeclared points unknown ordering and cycles`() {
        val point = point()
        val participant = participant("example.participant", point)
        val undeclaredPoint = shouldThrow<IllegalStateException> {
            graph(points = emptyList(), participants = listOf(participant))
        }
        undeclaredPoint.message shouldContain "targets undeclared point"

        val unknownOrder = participant(
            id = "example.ordered",
            point = point,
            order = FeatureExecutionOrder(after = setOf(FeatureExecutionParticipantId("example.unknown"))),
        )
        shouldThrow<IllegalArgumentException> {
            graph(listOf(point), listOf(unknownOrder))
        }.message shouldContain "unknown participant"

        val first = participant(
            id = "example.first",
            point = point,
            order = FeatureExecutionOrder(after = setOf(FeatureExecutionParticipantId("example.second"))),
        )
        val second = participant(
            id = "example.second",
            point = point,
            order = FeatureExecutionOrder(after = setOf(first.id)),
        )
        shouldThrow<IllegalStateException> {
            graph(listOf(point), listOf(first, second))
        }.message shouldContain "cyclic participant ordering"
    }

    @Test
    fun `assembly rejects duplicate and contradictory execution point declarations`() {
        val point = point()
        val participant = participant("example.participant", point)

        shouldThrow<IllegalStateException> {
            graph(listOf(point, point), listOf(participant))
        }.message shouldContain "Duplicate execution point example.point"

        val contradictoryPoint = durableFeatureExecutionPointDefinition<Event>(
            id = point.id,
            owner = point.owner,
            failurePolicy = point.failurePolicy,
        )
        val contradictoryParticipant = participant("example.contradictory", contradictoryPoint)
        shouldThrow<IllegalStateException> {
            graph(listOf(point), listOf(contradictoryParticipant))
        }.message shouldContain "Contradictory execution point definition example.point"
    }

    @Test
    fun `missing specialized participant work becomes an explicit obligation`() {
        val point = point()
        val adapter = specializedAdapterDefinition<ExampleAdapter>(
            id = SpecializedAdapterId("example.adapter"),
            owner = participantOwner,
        )
        val participant = participant(
            id = "example.specialized",
            point = point,
            specializedRequirements = listOf(adapter),
        )
        val graph = graph(listOf(point), listOf(participant))

        val evaluation = evaluateFeatureGraph(graph)

        evaluation.executionObligations.single().run {
            responsibleOwner shouldBe ContributionOwner("subject.type")
            subject.participant shouldBe participant.id
            requirement shouldBe adapter
        }
    }

    private fun point(
        failurePolicy: FeatureExecutionFailurePolicy = FeatureExecutionFailurePolicy.CONTINUE_AND_REPORT,
    ): InlineFeatureExecutionPointDefinition<Event> = inlineFeatureExecutionPointDefinition(
        id = FeatureExecutionPointId("example.point"),
        owner = pointOwner,
        failurePolicy = failurePolicy,
    )

    private fun participant(
        id: String,
        point: FeatureExecutionPointDefinition<Event>,
        prerequisites: CapabilityExpression = CapabilityExpression.Always,
        contextInputs: List<ContextInputDefinition<*>> = emptyList(),
        contextRule: FeatureContextRule? = null,
        contextBlockers: List<FeatureContextBlocker> = emptyList(),
        specializedRequirements: List<SpecializedAdapterDefinition<*>> = emptyList(),
        order: FeatureExecutionOrder = FeatureExecutionOrder(),
    ): FeatureExecutionParticipantDefinition<Event> {
        return FeatureExecutionParticipantDefinition(
            id = FeatureExecutionParticipantId(id),
            owner = participantOwner,
            point = point,
            prerequisites = prerequisites,
            contextInputs = contextInputs,
            contextRule = contextRule,
            contextBlockers = contextBlockers,
            specializedRequirements = specializedRequirements,
            order = order,
            behavioralContracts = listOf(ExampleContract),
        )
    }

    private fun graph(
        points: List<FeatureExecutionPointDefinition<*>>,
        participants: List<FeatureExecutionParticipantDefinition<*>>,
        types: List<ContentTypeContribution> = listOf(contentType("subject")),
    ): FeatureGraph {
        val contributors = buildList {
            types.groupBy { it.owner }.forEach { (owner, definitions) ->
                add(featureGraphContributor(owner) { definitions.forEach(::add) })
            }
            points.groupBy { it.owner }.forEach { (owner, definitions) ->
                add(featureGraphContributor(owner) { definitions.forEach(::add) })
            }
            participants.groupBy { it.owner }.forEach { (owner, definitions) ->
                add(featureGraphContributor(owner) { definitions.forEach(::add) })
            }
        }
        return discoverAndAssembleFeatureGraph(contributors)
    }

    private fun contentType(
        id: String,
        vararg providers: CapabilityProvider<*>,
    ): ContentTypeContribution {
        return ContentTypeContribution(
            contentType = ContentTypeId(id),
            owner = ContributionOwner("$id.type"),
            providers = providers.toList(),
        )
    }

    private fun runtime(
        graph: FeatureGraph,
        vararg bindings: FeatureExecutionParticipantBinding<*>,
    ): FeatureExecutionRuntime = FeatureExecutionRuntime(graph, evaluateFeatureGraph(graph), bindings.toList())

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
        kotlinx.coroutines.test.runTest { block() }
    }

    private data class Event(
        val value: String,
        val enabled: Boolean = true,
    )

    private object ExampleContract : FeatureBehaviorContract {
        override val id = FeatureArtifactId("example.execution-behavior")
    }

    private class AlphaProvider

    private class ExampleAdapter
}
