package mihon.feature.graph.validation.reporting

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.CapabilityId
import mihon.feature.graph.CapabilityProvider
import mihon.feature.graph.ContentTypeContribution
import mihon.feature.graph.ContentTypeId
import mihon.feature.graph.ContextInputId
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.DiscoveredFeatureGraphContributions
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureBehaviorProjection
import mihon.feature.graph.FeatureContextBlocker
import mihon.feature.graph.FeatureContextDecision
import mihon.feature.graph.FeatureContractScenarioId
import mihon.feature.graph.FeatureContribution
import mihon.feature.graph.FeatureExecutionFailurePolicy
import mihon.feature.graph.FeatureExecutionParticipantDefinition
import mihon.feature.graph.FeatureExecutionParticipantId
import mihon.feature.graph.FeatureExecutionPointId
import mihon.feature.graph.FeatureId
import mihon.feature.graph.FeatureIntegration
import mihon.feature.graph.FeatureIntegrationId
import mihon.feature.graph.FeatureProjection
import mihon.feature.graph.afterCommitVolatileFeatureExecutionPointDefinition
import mihon.feature.graph.assembleFeatureGraph
import mihon.feature.graph.capabilityDefinition
import mihon.feature.graph.contextEvidence
import mihon.feature.graph.contextInputDefinition
import mihon.feature.graph.evaluateFeatureGraph
import mihon.feature.graph.featureContextRule
import mihon.feature.graph.featureProjectionDefinition
import mihon.feature.graph.validation.FeatureContractFailure
import mihon.feature.graph.validation.FeatureContractScenario
import mihon.feature.graph.validation.FeatureContractVerificationResult
import mihon.feature.graph.validation.FeatureContractVerifier
import mihon.feature.graph.validation.FeatureExecutionContractReference
import mihon.feature.graph.validation.FeatureExecutionContractVerifier
import mihon.feature.graph.validation.FeatureValidationContributor
import mihon.feature.graph.validation.featureValidationContributor
import mihon.feature.graph.validation.planFeatureContractValidation
import mihon.feature.graph.validation.validateFeatureContracts
import org.junit.jupiter.api.Test
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

class FeatureDeveloperReportTest {
    private val capabilityOwner = ContributionOwner("future.playback")
    private val featureOwner = ContributionOwner("future.queue")
    private val contextOwner = ContributionOwner("future.environment")
    private val featureId = FeatureId("future.queue")
    private val integrationId = FeatureIntegrationId("future.queue.playback")
    private val provider = capabilityDefinition<FutureProvider>(CapabilityId("future.playback"), capabilityOwner)
    private val context =
        contextInputDefinition<FutureContext>(ContextInputId("future.environment.ready"), contextOwner)
    private val blocker = FeatureContextBlocker(FeatureArtifactId("future.environment.unavailable"), listOf(context))
    private val contract = object : FeatureBehaviorContract {
        override val id = FeatureArtifactId("future.queue.behavior")
    }
    private val behavior = object : FeatureBehaviorProjection {
        override val id = FeatureArtifactId("future.queue.action")
    }

    @Test
    fun `unknown contributions appear automatically without turning scenarios into type support`() = runSuspend {
        val projectionDefinition = featureProjectionDefinition<FutureProjection>(
            FeatureArtifactId("future.queue.developer-projection"),
            featureOwner,
        )
        val integration = FeatureIntegration(
            id = integrationId,
            prerequisites = CapabilityExpression.Provided(provider),
            contextInputs = listOf(context),
            contextRule = featureContextRule(featureOwner) { evidence ->
                if (evidence.value(context).ready) {
                    FeatureContextDecision.Applicable
                } else {
                    FeatureContextDecision.Blocked(listOf(blocker))
                }
            },
            contextBlockers = listOf(blocker),
            behaviorProjections = listOf(behavior),
            behavioralContracts = listOf(contract),
            projectionRequirements = listOf(projectionDefinition),
            projections = listOf(FeatureProjection(projectionDefinition, FutureProjection)),
        )
        val graph = graph(
            types = listOf(
                type("audio", providesCapability = true),
                type("silent", providesCapability = false),
            ),
            integration = integration,
        )
        val evaluation = evaluateFeatureGraph(graph)
        val validation = validateFeatureContracts(
            planFeatureContractValidation(graph, evaluation, listOf(validationContributor())),
        )

        val report = buildFeatureDeveloperReport(graph, evaluation, validation)

        report.contentTypes.map { it.id } shouldContainExactly listOf("audio", "silent")
        report.contentTypes.first().providers.map { it.id } shouldContainExactly listOf("future.playback")
        report.features.map { it.id } shouldContainExactly listOf("future.queue")
        report.integrations.map { it.state } shouldContainExactly listOf(
            FeatureDeveloperIntegrationState.CONDITIONAL,
            FeatureDeveloperIntegrationState.INAPPLICABLE,
        )
        report.integrations.first().apply {
            contextInputs.map { it.id } shouldContainExactly listOf("future.environment.ready")
            declaredBlockers.map { it.id } shouldContainExactly listOf("future.environment.unavailable")
            behaviors.single().availability shouldBe FeatureDeveloperArtifactAvailability.CONDITIONAL
            contracts.single().validations.single().apply {
                scenario shouldBe "future.queue.applicable"
                outcome shouldBe FeatureDeveloperContractValidationOutcome.PASSED
            }
            projections.single().apply {
                implementationPresent shouldBe true
                availability shouldBe FeatureDeveloperArtifactAvailability.CONDITIONAL
            }
        }
        report.obligations shouldBe emptyList()

        val rendered = renderFeatureDeveloperReport(report)
        rendered shouldBe renderFeatureDeveloperReport(report)
        rendered shouldContain "Contextual validation scenarios are samples"
        rendered shouldContain "audio -> future.queue/future.queue.playback [conditional]"
        rendered shouldContain "validation: scenario=future.queue.applicable passed"
    }

    @Test
    fun `missing selected artifacts and validation work retain responsible owners`() = runSuspend {
        val projectionDefinition = featureProjectionDefinition<FutureProjection>(
            FeatureArtifactId("future.queue.missing-projection"),
            featureOwner,
        )
        val integration = FeatureIntegration(
            id = integrationId,
            prerequisites = CapabilityExpression.Provided(provider),
            behaviorProjections = listOf(behavior),
            behavioralContracts = listOf(contract),
            projectionRequirements = listOf(projectionDefinition),
        )
        val graph = graph(listOf(type("audio", providesCapability = true)), integration)
        val evaluation = evaluateFeatureGraph(graph)
        val validation = validateFeatureContracts(
            planFeatureContractValidation(graph, evaluation, emptyList()),
        )

        val report = buildFeatureDeveloperReport(graph, evaluation, validation)

        report.integrations.single().state shouldBe FeatureDeveloperIntegrationState.APPLICABLE
        report.integrations.single().projections.single().implementationPresent shouldBe false
        report.obligations.map { it.category } shouldContainExactly listOf(
            FeatureDeveloperObligationCategory.CONTRACT_VERIFIER,
            FeatureDeveloperObligationCategory.PROJECTION,
        )
        report.obligations.forEach { obligation ->
            obligation.responsibleOwner shouldBe featureOwner.value
            obligation.subjects.single().contentType shouldBe "audio"
        }
    }

    @Test
    fun `applicable scenario reports missing conditional projection without changing static support`() = runSuspend {
        val projectionDefinition = featureProjectionDefinition<FutureProjection>(
            FeatureArtifactId("future.queue.conditional-projection"),
            featureOwner,
        )
        val integration = FeatureIntegration(
            id = integrationId,
            prerequisites = CapabilityExpression.Provided(provider),
            contextInputs = listOf(context),
            contextRule = featureContextRule(featureOwner) { FeatureContextDecision.Applicable },
            behaviorProjections = listOf(behavior),
            behavioralContracts = listOf(contract),
            projectionRequirements = listOf(projectionDefinition),
        )
        val graph = graph(
            listOf(
                type("audio", providesCapability = true),
                type("video", providesCapability = true),
            ),
            integration,
        )
        val evaluation = evaluateFeatureGraph(graph)
        val validation = validateFeatureContracts(
            planFeatureContractValidation(graph, evaluation, listOf(validationContributor())),
        )

        val report = buildFeatureDeveloperReport(graph, evaluation, validation)

        report.integrations.forEach { reported ->
            reported.state shouldBe FeatureDeveloperIntegrationState.CONDITIONAL
            reported.projections.single().availability shouldBe FeatureDeveloperArtifactAvailability.CONDITIONAL
        }
        report.obligations.single().apply {
            category shouldBe FeatureDeveloperObligationCategory.PROJECTION
            responsibleOwner shouldBe featureOwner.value
            artifact shouldBe "future.queue.conditional-projection"
            subjects.map { it.contentType } shouldContainExactly listOf("audio", "video")
        }
    }

    @Test
    fun `contract failures and crashes remain structured in the report`() = runSuspend {
        val integration = FeatureIntegration(
            id = integrationId,
            prerequisites = CapabilityExpression.Provided(provider),
            behaviorProjections = listOf(behavior),
            behavioralContracts = listOf(contract),
        )
        val graph = graph(listOf(type("audio", providesCapability = true)), integration)
        val evaluation = evaluateFeatureGraph(graph)
        val failedValidation = validateFeatureContracts(
            planFeatureContractValidation(
                graph,
                evaluation,
                listOf(
                    verifierContributor {
                        FeatureContractVerificationResult.Failed(
                            listOf(FeatureContractFailure("observable mismatch")),
                        )
                    },
                ),
            ),
        )
        val crashedValidation = validateFeatureContracts(
            planFeatureContractValidation(
                graph,
                evaluation,
                listOf(verifierContributor { error("broken verifier") }),
            ),
        )

        buildFeatureDeveloperReport(graph, evaluation, failedValidation)
            .integrations.single().contracts.single().validations.single().apply {
                outcome shouldBe FeatureDeveloperContractValidationOutcome.FAILED
                details shouldContainExactly listOf("observable mismatch")
            }
        buildFeatureDeveloperReport(graph, evaluation, crashedValidation)
            .integrations.single().contracts.single().validations.single().apply {
                outcome shouldBe FeatureDeveloperContractValidationOutcome.CRASHED
                details.last() shouldBe "broken verifier"
            }
    }

    @Test
    fun `execution points and independently owned participants appear in the report`() = runSuspend {
        val pointOwner = ContributionOwner("future.lifecycle")
        val participantOwner = ContributionOwner("future.cleanup")
        val point = afterCommitVolatileFeatureExecutionPointDefinition<FutureEvent>(
            id = FeatureExecutionPointId("future.lifecycle.removed"),
            owner = pointOwner,
            failurePolicy = FeatureExecutionFailurePolicy.CONTINUE_AND_REPORT,
        )
        val executionContract = object : FeatureBehaviorContract {
            override val id = FeatureArtifactId("future.cleanup.behavior")
        }
        val participant = FeatureExecutionParticipantDefinition(
            id = FeatureExecutionParticipantId("future.cleanup.cover"),
            owner = participantOwner,
            point = point,
            prerequisites = CapabilityExpression.Provided(provider),
            behavioralContracts = listOf(executionContract),
        )
        val graph = assembleFeatureGraph(
            DiscoveredFeatureGraphContributions(
                contentTypes = listOf(
                    type("audio", providesCapability = true),
                    type("silent", providesCapability = false),
                ),
                features = emptyList(),
                executionPoints = listOf(point),
                executionParticipants = listOf(participant),
            ),
        )
        val evaluation = evaluateFeatureGraph(graph)
        val participantValidation = featureValidationContributor(participantOwner) {
            add(
                FeatureExecutionContractVerifier(
                    FeatureExecutionContractReference(participant.id, executionContract),
                ) {
                    FeatureContractVerificationResult.Passed
                },
            )
        }
        val validation = validateFeatureContracts(
            planFeatureContractValidation(graph, evaluation, listOf(participantValidation)),
        )

        val report = buildFeatureDeveloperReport(graph, evaluation, validation)

        report.executionPoints.single().apply {
            id shouldBe "future.lifecycle.removed"
            phase shouldBe "after-commit-volatile"
        }
        report.executionParticipants.map { it.state } shouldContainExactly listOf(
            FeatureDeveloperIntegrationState.APPLICABLE,
            FeatureDeveloperIntegrationState.INAPPLICABLE,
        )
        report.executionParticipants.first().contracts.map { it.id } shouldContainExactly
            listOf("future.cleanup.behavior")
        report.executionParticipants.first().contracts.single().validations.single().outcome shouldBe
            FeatureDeveloperContractValidationOutcome.PASSED
        renderFeatureDeveloperReport(report) shouldContain
            "audio -> future.lifecycle.removed/future.cleanup.cover [applicable]"
        Unit
    }

    private fun validationContributor(): FeatureValidationContributor = featureValidationContributor(featureOwner) {
        add(
            FeatureContractVerifier(mihon.feature.graph.validation.FeatureContractReference(featureId, contract)) {
                FeatureContractVerificationResult.Passed
            },
        )
        add(
            FeatureContractScenario(
                id = FeatureContractScenarioId("future.queue.applicable"),
                contract = mihon.feature.graph.validation.FeatureContractReference(featureId, contract),
                integration = integrationId,
            ) {
                listOf(contextEvidence(context, FutureContext(ready = true)))
            },
        )
    }

    private fun verifierContributor(
        verify: suspend () -> FeatureContractVerificationResult,
    ): FeatureValidationContributor = featureValidationContributor(featureOwner) {
        add(
            FeatureContractVerifier(mihon.feature.graph.validation.FeatureContractReference(featureId, contract)) {
                verify()
            },
        )
    }

    private fun graph(
        types: List<ContentTypeContribution>,
        integration: FeatureIntegration,
    ) = assembleFeatureGraph(
        DiscoveredFeatureGraphContributions(
            contentTypes = types,
            features = listOf(FeatureContribution(featureId, featureOwner, listOf(integration))),
        ),
    )

    private fun type(id: String, providesCapability: Boolean) = ContentTypeContribution(
        contentType = ContentTypeId(id),
        owner = ContributionOwner("$id.type"),
        providers = if (providesCapability) {
            listOf(CapabilityProvider(provider, FutureProvider))
        } else {
            emptyList()
        },
    )

    private data object FutureProvider
    private data class FutureContext(val ready: Boolean)
    private data object FutureProjection
    private data class FutureEvent(val entryId: Long)
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
    return requireNotNull(outcome).getOrThrow()
}
