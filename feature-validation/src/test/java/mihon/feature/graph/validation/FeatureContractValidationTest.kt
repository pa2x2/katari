package mihon.feature.graph.validation

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.CapabilityId
import mihon.feature.graph.CapabilityProvider
import mihon.feature.graph.ContentTypeContribution
import mihon.feature.graph.ContentTypeId
import mihon.feature.graph.ContextInputId
import mihon.feature.graph.ContractFixture
import mihon.feature.graph.ContractFixtureId
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.DiscoveredFeatureGraphContributions
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureContextBlocker
import mihon.feature.graph.FeatureContextDecision
import mihon.feature.graph.FeatureContractScenarioId
import mihon.feature.graph.FeatureContribution
import mihon.feature.graph.FeatureGraph
import mihon.feature.graph.FeatureId
import mihon.feature.graph.FeatureIntegration
import mihon.feature.graph.FeatureIntegrationId
import mihon.feature.graph.FeatureObligation
import mihon.feature.graph.MissingContractFixtureObligation
import mihon.feature.graph.SpecializedAdapter
import mihon.feature.graph.SpecializedAdapterId
import mihon.feature.graph.SpecializedFeatureObligation
import mihon.feature.graph.assembleFeatureGraph
import mihon.feature.graph.capabilityDefinition
import mihon.feature.graph.contextEvidence
import mihon.feature.graph.contextInputDefinition
import mihon.feature.graph.contractFixtureDefinition
import mihon.feature.graph.evaluateFeatureGraph
import mihon.feature.graph.featureContextRule
import mihon.feature.graph.specializedAdapterDefinition
import org.junit.jupiter.api.Test
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

class FeatureContractValidationTest {
    private val capabilityOwner = ContributionOwner("example.capability")
    private val featureOwner = ContributionOwner("example.feature")
    private val contextOwner = ContributionOwner("example.context")
    private val feature = FeatureId("example.feature")
    private val integration = FeatureIntegrationId("example.integration")
    private val contractId = FeatureArtifactId("example.behavior")
    private val providerDefinition = capabilityDefinition<ExampleProvider>(
        CapabilityId("example.provider"),
        capabilityOwner,
    )

    @Test
    fun `missing verifier and media fixture are owned without invalidating unsupported types`() = runSuspend {
        val fixture = contractFixtureDefinition<ExampleFixture>(ContractFixtureId("example.fixture"), featureOwner)
        val contract = contract(listOf(fixture))
        val graph = graph(
            contentTypes = listOf(
                type("complete", fixtures = listOf(ContractFixture(fixture, ExampleFixture("media")))),
                type("missing"),
                unsupportedType("unsupported"),
            ),
            integration = integration(contract),
        )

        val plan = planFeatureContractValidation(graph, evaluateFeatureGraph(graph), emptyList())

        plan.executions shouldBe emptyList()
        plan.isComplete shouldBe false
        plan.validationIssues<MissingFeatureContractVerifierObligation>().single().apply {
            responsibleOwner shouldBe featureOwner
            affectedSubjects.map { it.entryContentType.value } shouldContainExactly listOf("complete", "missing")
        }
        plan.graphIssues<MissingContractFixtureObligation>().single().apply {
            responsibleOwner shouldBe ContributionOwner("missing.type")
            subject.entryContentType shouldBe ContentTypeId("missing")
        }
        validateFeatureContracts(plan).isSuccessful shouldBe false
    }

    @Test
    fun `context scenario resolves each candidate before contract selection`() = runSuspend {
        val context = contextInputDefinition<ExampleContext>(ContextInputId("example.context"), contextOwner)
        val blocked = FeatureContextBlocker(FeatureArtifactId("example.blocked"), listOf(context))
        val adapter =
            specializedAdapterDefinition<ExampleAdapter>(SpecializedAdapterId("example.adapter"), featureOwner)
        val contract = contract()
        val contextualIntegration = FeatureIntegration(
            id = integration,
            prerequisites = CapabilityExpression.Provided(providerDefinition),
            contextInputs = listOf(context),
            contextRule = featureContextRule(featureOwner) { evidence ->
                if (evidence.value(context).enabled) {
                    FeatureContextDecision.Applicable
                } else {
                    FeatureContextDecision.Blocked(listOf(blocked))
                }
            },
            contextBlockers = listOf(blocked),
            specializedRequirements = listOf(adapter),
            behavioralContracts = listOf(contract),
        )
        val graph = graph(
            contentTypes = listOf(
                type("complete", adapters = listOf(SpecializedAdapter(adapter, ExampleAdapter("adapted")))),
                type("incomplete"),
            ),
            integration = contextualIntegration,
        )
        val contributor = featureValidationContributor(featureOwner) {
            add(
                FeatureContractVerifier(reference(contract)) { input ->
                    input.provider(providerDefinition).state shouldBe "ready"
                    input.adapter(adapter).state shouldBe "adapted"
                    input.evidence(context).enabled shouldBe true
                    FeatureContractVerificationResult.Passed
                },
            )
            add(
                FeatureContractScenario(
                    id = FeatureContractScenarioId("example.applicable"),
                    contract = reference(contract),
                    integration = integration,
                    evidenceFactory = FeatureContractEvidenceFactory {
                        listOf(contextEvidence(context, ExampleContext(enabled = true)))
                    },
                ),
            )
        }

        val plan = planFeatureContractValidation(graph, evaluateFeatureGraph(graph), listOf(contributor))
        val validation = validateFeatureContracts(plan)

        validation.executions.single().shouldBeInstanceOf<CompletedFeatureContractExecution>()
        validation.isSuccessful shouldBe false
        plan.executions.single().contractSelection.contextEvidence.single().value shouldBe ExampleContext(true)
        plan.graphIssues<SpecializedFeatureObligation>().single().apply {
            responsibleOwner shouldBe ContributionOwner("incomplete.type")
            subject.entryContentType shouldBe ContentTypeId("incomplete")
        }
    }

    @Test
    fun `missing or blocked enabling scenario is a feature obligation`() {
        val context = contextInputDefinition<ExampleContext>(ContextInputId("example.context"), contextOwner)
        val blocked = FeatureContextBlocker(FeatureArtifactId("example.blocked"), listOf(context))
        val contract = contract()
        val contextualIntegration = FeatureIntegration(
            id = integration,
            prerequisites = CapabilityExpression.Provided(providerDefinition),
            contextInputs = listOf(context),
            contextRule = featureContextRule(featureOwner) {
                FeatureContextDecision.Blocked(listOf(blocked))
            },
            contextBlockers = listOf(blocked),
            behavioralContracts = listOf(contract),
        )
        val graph = graph(listOf(type("future"), type("second")), contextualIntegration)
        val verifierOnly = verifierContributor(contract) { FeatureContractVerificationResult.Passed }

        planFeatureContractValidation(graph, evaluateFeatureGraph(graph), listOf(verifierOnly))
            .validationIssues<MissingFeatureContractScenarioObligation>()
            .single()
            .affectedSubjects.map { it.entryContentType.value } shouldContainExactly listOf("future", "second")

        val blockedScenario = featureValidationContributor(featureOwner) {
            verifierOnly.contributeTo(this)
            add(
                FeatureContractScenario(
                    FeatureContractScenarioId("example.blocked-scenario"),
                    reference(contract),
                    integration,
                ) { listOf(contextEvidence(context, ExampleContext(false))) },
            )
        }
        val blockedGraph = graph(listOf(type("future")), contextualIntegration)
        val blockedPlan = planFeatureContractValidation(
            blockedGraph,
            evaluateFeatureGraph(blockedGraph),
            listOf(blockedScenario),
        )

        blockedPlan.executions shouldBe emptyList()
        blockedPlan.validationIssues<InvalidFeatureContractScenarioObligation>()
            .single()
            .reason shouldBe "blocked by: example.blocked"
    }

    @Test
    fun `execution failures and crashes remain structured`() = runSuspend {
        val contract = contract()
        val graph = graph(listOf(type("future")), integration(contract))
        val failed = verifierContributor(contract) {
            FeatureContractVerificationResult.Failed(listOf(FeatureContractFailure("observable mismatch")))
        }
        val failedValidation = validateFeatureContracts(
            planFeatureContractValidation(graph, evaluateFeatureGraph(graph), listOf(failed)),
        )
        val failedResult = failedValidation.executions.single() as CompletedFeatureContractExecution
        failedResult.verification shouldBe FeatureContractVerificationResult.Failed(
            listOf(FeatureContractFailure("observable mismatch")),
        )
        failedValidation.isSuccessful shouldBe false

        val crashed = verifierContributor(contract) { error("broken verifier") }
        val crashedValidation = validateFeatureContracts(
            planFeatureContractValidation(graph, evaluateFeatureGraph(graph), listOf(crashed)),
        )
        crashedValidation.executions.single().shouldBeInstanceOf<CrashedFeatureContractExecution>()
            .cause.message shouldBe "broken verifier"
        crashedValidation.isSuccessful shouldBe false
    }

    private fun verifierContributor(
        contract: FeatureBehaviorContract,
        verify: suspend (FeatureContractExecutionInput) -> FeatureContractVerificationResult,
    ): FeatureValidationContributor = featureValidationContributor(featureOwner) {
        add(FeatureContractVerifier(reference(contract), FeatureContractVerification(verify)))
    }

    private fun reference(contract: FeatureBehaviorContract) = FeatureContractReference(feature, contract)

    private fun contract(
        fixtures: List<mihon.feature.graph.ContractFixtureDefinition<*>> = emptyList(),
    ): FeatureBehaviorContract = object : FeatureBehaviorContract {
        override val id = contractId
        override val fixtureRequirements = fixtures
    }

    private fun integration(contract: FeatureBehaviorContract) = FeatureIntegration(
        id = integration,
        prerequisites = CapabilityExpression.Provided(providerDefinition),
        behavioralContracts = listOf(contract),
    )

    private fun graph(
        contentTypes: List<ContentTypeContribution>,
        integration: FeatureIntegration,
    ): FeatureGraph = assembleFeatureGraph(
        DiscoveredFeatureGraphContributions(
            contentTypes = contentTypes,
            features = listOf(FeatureContribution(feature, featureOwner, listOf(integration))),
        ),
    )

    private fun type(
        id: String,
        adapters: List<SpecializedAdapter<*>> = emptyList(),
        fixtures: List<ContractFixture<*>> = emptyList(),
    ) = ContentTypeContribution(
        contentType = ContentTypeId(id),
        owner = ContributionOwner("$id.type"),
        providers = listOf(CapabilityProvider(providerDefinition, ExampleProvider("ready"))),
        specializedAdapters = adapters,
        contractFixtures = fixtures,
    )

    private fun unsupportedType(id: String) = ContentTypeContribution(
        ContentTypeId(id),
        ContributionOwner("$id.type"),
    )

    private data class ExampleProvider(val state: String)
    private data class ExampleFixture(val state: String)
    private data class ExampleContext(val enabled: Boolean)
    private data class ExampleAdapter(val state: String)
}

private inline fun <reified O : FeatureObligation> FeatureContractValidationPlan.graphIssues(): List<O> {
    return issues.filterIsInstance<GraphFeatureContractPlanIssue>()
        .map { it.obligation }
        .filterIsInstance<O>()
}

private typealias ValidationObligation = FeatureContractValidationObligation

private inline fun <reified O : ValidationObligation> FeatureContractValidationPlan.validationIssues(): List<O> {
    return issues.filterIsInstance<ValidationFeatureContractPlanIssue>()
        .map { it.obligation }
        .filterIsInstance<O>()
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
