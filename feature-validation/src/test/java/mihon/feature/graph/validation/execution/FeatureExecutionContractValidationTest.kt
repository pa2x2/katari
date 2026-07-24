package mihon.feature.graph.validation.execution

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.CapabilityId
import mihon.feature.graph.CapabilityProvider
import mihon.feature.graph.ContentTypeContribution
import mihon.feature.graph.ContentTypeId
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.DiscoveredFeatureGraphContributions
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureExecutionFailurePolicy
import mihon.feature.graph.FeatureExecutionParticipantDefinition
import mihon.feature.graph.FeatureExecutionParticipantId
import mihon.feature.graph.FeatureExecutionPointId
import mihon.feature.graph.afterCommitVolatileFeatureExecutionPointDefinition
import mihon.feature.graph.assembleFeatureGraph
import mihon.feature.graph.capabilityDefinition
import mihon.feature.graph.evaluateFeatureGraph
import mihon.feature.graph.validation.CompletedFeatureExecutionContractExecution
import mihon.feature.graph.validation.FeatureContractVerificationResult
import mihon.feature.graph.validation.FeatureExecutionContractReference
import mihon.feature.graph.validation.FeatureExecutionContractVerifier
import mihon.feature.graph.validation.MissingFeatureExecutionContractVerifierObligation
import mihon.feature.graph.validation.ValidationFeatureContractPlanIssue
import mihon.feature.graph.validation.featureValidationContributor
import mihon.feature.graph.validation.planFeatureContractValidation
import mihon.feature.graph.validation.validateFeatureContracts
import org.junit.jupiter.api.Test
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

class FeatureExecutionContractValidationTest {
    private val capabilityOwner = ContributionOwner("example.capability")
    private val pointOwner = ContributionOwner("example.point")
    private val participantOwner = ContributionOwner("example.participant")
    private val capability = capabilityDefinition<ExampleProvider>(CapabilityId("example.provider"), capabilityOwner)
    private val contract = object : FeatureBehaviorContract {
        override val id = FeatureArtifactId("example.execution.contract")
    }
    private val point = afterCommitVolatileFeatureExecutionPointDefinition<ExampleEvent>(
        id = FeatureExecutionPointId("example.point"),
        owner = pointOwner,
        failurePolicy = FeatureExecutionFailurePolicy.FAIL_FAST,
    )
    private val participant = FeatureExecutionParticipantDefinition(
        id = FeatureExecutionParticipantId("example.participant"),
        owner = participantOwner,
        point = point,
        prerequisites = CapabilityExpression.Provided(capability),
        behavioralContracts = listOf(contract),
    )

    @Test
    fun `applicable participant contract is automatically selected and executed`() = runSuspend {
        val graph = graph("supported")
        val executedTypes = mutableListOf<ContentTypeId>()
        val contributor = featureValidationContributor(participantOwner) {
            add(
                FeatureExecutionContractVerifier(
                    FeatureExecutionContractReference(participant.id, contract),
                ) { input ->
                    input.provider(capability)
                    executedTypes += input.subject.contentType
                    FeatureContractVerificationResult.Passed
                },
            )
        }

        val plan = planFeatureContractValidation(graph, evaluateFeatureGraph(graph), listOf(contributor))
        val result = validateFeatureContracts(plan)

        plan.isComplete shouldBe true
        plan.executionParticipantExecutions.map { it.contractSelection.subject.participant } shouldContainExactly
            listOf(participant.id)
        (result.executionParticipantExecutions.single() is CompletedFeatureExecutionContractExecution) shouldBe true
        executedTypes shouldContainExactly listOf(ContentTypeId("supported"))
    }

    @Test
    fun `missing participant verifier is a validation obligation`() {
        val graph = graph("supported", "also-supported")

        val plan = planFeatureContractValidation(graph, evaluateFeatureGraph(graph), emptyList())

        plan.isComplete shouldBe false
        plan.issues.mapNotNull { issue ->
            (issue as? ValidationFeatureContractPlanIssue)?.obligation as?
                MissingFeatureExecutionContractVerifierObligation
        }.single().let { obligation ->
            obligation.contract.participant shouldBe participant.id
            obligation.affectedSubjects.map { it.contentType } shouldContainExactly listOf(
                ContentTypeId("also-supported"),
                ContentTypeId("supported"),
            )
        }
    }

    private fun graph(vararg contentTypeNames: String) = assembleFeatureGraph(
        DiscoveredFeatureGraphContributions(
            contentTypes = contentTypeNames.map { contentTypeName ->
                ContentTypeContribution(
                    contentType = ContentTypeId(contentTypeName),
                    owner = ContributionOwner("example.type.$contentTypeName"),
                    providers = listOf(CapabilityProvider(capability, ExampleProvider())),
                )
            },
            features = emptyList(),
            executionPoints = listOf(point),
            executionParticipants = listOf(participant),
        ),
    )

    private class ExampleProvider
    private data class ExampleEvent(val value: String)
}

private fun <T> runSuspend(block: suspend () -> T): T {
    var outcome: Result<T>? = null
    block.startCoroutine(
        object : Continuation<T> {
            override val context = EmptyCoroutineContext

            override fun resumeWith(result: Result<T>) {
                outcome = result
            }
        },
    )
    return requireNotNull(outcome) { "Test coroutine did not complete synchronously" }.getOrThrow()
}
