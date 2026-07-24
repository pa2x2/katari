package mihon.entry.interactions.documentation.acceptance

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import mihon.entry.interactions.documentation.EntryContentTypeReferenceContextEvidenceProvider
import mihon.entry.interactions.documentation.EntryContentTypeReferenceProjection
import mihon.entry.interactions.documentation.EntryContentTypeReferenceProjectionInput
import mihon.entry.interactions.documentation.EntryContentTypeReferenceProjectionResult
import mihon.entry.interactions.documentation.EntryContentTypeReferenceRow
import mihon.entry.interactions.documentation.EntryContentTypeReferenceSection
import mihon.entry.interactions.documentation.EntryContentTypeReferenceStatus
import mihon.entry.interactions.documentation.projection.planEntryContentTypeReference
import mihon.entry.interactions.documentation.rendering.renderEntryContentTypeReferenceMarkdown
import mihon.entry.interactions.documentation.rendering.renderEntrySourceSdkConsumerCoverageMarkdown
import mihon.entry.interactions.documentation.source.entrySourceContextInputDefinition
import mihon.entry.interactions.documentation.source.planEntrySourceSdkConsumerCoverage
import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.CapabilityId
import mihon.feature.graph.CapabilityProvider
import mihon.feature.graph.ContentTypeContribution
import mihon.feature.graph.ContentTypeId
import mihon.feature.graph.ContextInputId
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureBehaviorProjection
import mihon.feature.graph.FeatureContextBlocker
import mihon.feature.graph.FeatureContextDecision
import mihon.feature.graph.FeatureContractScenarioId
import mihon.feature.graph.FeatureContribution
import mihon.feature.graph.FeatureId
import mihon.feature.graph.FeatureIntegration
import mihon.feature.graph.FeatureIntegrationId
import mihon.feature.graph.FeatureProjection
import mihon.feature.graph.capabilityDefinition
import mihon.feature.graph.contextEvidence
import mihon.feature.graph.discoverAndAssembleFeatureGraph
import mihon.feature.graph.evaluateFeatureGraph
import mihon.feature.graph.featureContextRule
import mihon.feature.graph.featureGraphContributor
import mihon.feature.graph.featureProjectionDefinition
import mihon.feature.graph.validation.FeatureContractReference
import mihon.feature.graph.validation.FeatureContractScenario
import mihon.feature.graph.validation.FeatureContractVerificationResult
import mihon.feature.graph.validation.FeatureContractVerifier
import mihon.feature.graph.validation.featureValidationContributor
import mihon.feature.graph.validation.planFeatureContractValidation
import mihon.feature.graph.validation.reporting.FeatureDeveloperContractValidationOutcome
import mihon.feature.graph.validation.reporting.FeatureDeveloperIntegrationState
import mihon.feature.graph.validation.reporting.buildFeatureDeveloperReport
import mihon.feature.graph.validation.reporting.renderFeatureDeveloperReport
import mihon.feature.graph.validation.validateFeatureContracts
import org.junit.jupiter.api.Test
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

class UnknownEntryFeatureContractValidationAcceptanceTest {

    private val typeId = ContentTypeId("future-audio")
    private val typeOwner = ContributionOwner("future-audio")
    private val capabilityOwner = ContributionOwner("future-playback")
    private val featureOwner = ContributionOwner("future-queue")
    private val featureId = FeatureId("future.queue")
    private val staticIntegrationId = FeatureIntegrationId("future.queue.static")
    private val contextualIntegrationId = FeatureIntegrationId("future.queue.contextual")
    private val capability = capabilityDefinition<FuturePlaybackProvider>(
        CapabilityId("future.playback"),
        capabilityOwner,
    )
    private val provider = FuturePlaybackProvider()
    private val sourceContext = entrySourceContextInputDefinition<FutureSourceContext>(
        id = ContextInputId("future.source.playback"),
        contracts = setOf(FutureSourceContract::class),
        contractIntegrations = mapOf(
            FutureSourceContract::class to setOf(contextualIntegrationId),
        ),
    )
    private val staticContract = contract("future.queue.static-behavior")
    private val contextualContract = contract("future.queue.contextual-behavior")
    private val staticProjection = rowProjection(
        id = "future-playback",
        label = "Future playback",
        order = 100,
    )
    private val contextualProjection = rowProjection(
        id = "future-source-queue",
        label = "Future source queue",
        order = 200,
    )

    @Test
    fun `unknown contribution crosses contracts reporting and documentation without curated edits`() = runSuspend {
        val graph = discoverAndAssembleFeatureGraph(
            listOf(
                featureGraphContributor(typeOwner) {
                    add(
                        ContentTypeContribution(
                            contentType = typeId,
                            owner = typeOwner,
                            providers = listOf(CapabilityProvider(capability, provider)),
                        ),
                    )
                },
                featureGraphContributor(featureOwner) {
                    add(featureContribution())
                },
            ),
        )
        val evaluation = evaluateFeatureGraph(graph)
        val executed = mutableListOf<FeatureIntegrationId>()
        val validationContributor = featureValidationContributor(featureOwner) {
            add(
                FeatureContractVerifier(FeatureContractReference(featureId, staticContract)) { input ->
                    input.provider(capability) shouldBe provider
                    executed += input.subject.integration
                    FeatureContractVerificationResult.Passed
                },
            )
            add(
                FeatureContractVerifier(FeatureContractReference(featureId, contextualContract)) { input ->
                    input.provider(capability) shouldBe provider
                    input.evidence(sourceContext) shouldBe FutureSourceContext(available = true)
                    executed += input.subject.integration
                    FeatureContractVerificationResult.Passed
                },
            )
            add(
                FeatureContractScenario(
                    id = FeatureContractScenarioId("future.queue.source-available"),
                    contract = FeatureContractReference(featureId, contextualContract),
                    integration = contextualIntegrationId,
                ) {
                    listOf(contextEvidence(sourceContext, FutureSourceContext(available = true)))
                },
            )
        }
        val validation = validateFeatureContracts(
            planFeatureContractValidation(graph, evaluation, listOf(validationContributor)),
        )

        validation.isSuccessful shouldBe true
        executed.toSet() shouldBe setOf(staticIntegrationId, contextualIntegrationId)

        val report = buildFeatureDeveloperReport(graph, evaluation, validation)
        report.contentTypes.map { it.id } shouldContainExactly listOf(typeId.value)
        report.features.map { it.id } shouldContainExactly listOf(featureId.value)
        report.obligations shouldBe emptyList()
        report.integrations.associateBy { it.id }.apply {
            getValue(staticIntegrationId.value).apply {
                state shouldBe FeatureDeveloperIntegrationState.APPLICABLE
                contracts.single().validations.single().apply {
                    scenario shouldBe null
                    outcome shouldBe FeatureDeveloperContractValidationOutcome.PASSED
                }
            }
            getValue(contextualIntegrationId.value).apply {
                state shouldBe FeatureDeveloperIntegrationState.CONDITIONAL
                contracts.single().validations.single().apply {
                    scenario shouldBe "future.queue.source-available"
                    outcome shouldBe FeatureDeveloperContractValidationOutcome.PASSED
                }
            }
        }
        renderFeatureDeveloperReport(report) shouldContain
            "future-audio -> future.queue/future.queue.contextual [conditional]"

        val reference = planEntryContentTypeReference(
            graph = graph,
            evaluation = evaluation,
            contextEvidence = EntryContentTypeReferenceContextEvidenceProvider { _, input ->
                if (input == sourceContext) {
                    contextEvidence(sourceContext, FutureSourceContext(available = true))
                } else {
                    null
                }
            },
        )
        reference.isComplete shouldBe true
        reference.contentTypes shouldContainExactly listOf(typeId)
        reference.rows.associate { row ->
            row.definition.id to row.statuses.getValue(typeId)
        } shouldBe mapOf(
            "future-playback" to EntryContentTypeReferenceStatus.SUPPORTED,
            "future-source-queue" to EntryContentTypeReferenceStatus.SUPPORTED,
        )
        renderEntryContentTypeReferenceMarkdown(reference).apply {
            this shouldContain "Future Audio"
            this shouldContain "Future playback"
            this shouldContain "Future source queue"
        }

        val sourceCoverage = planEntrySourceSdkConsumerCoverage(graph)
        sourceCoverage.isComplete shouldBe true
        sourceCoverage.consumers.single().apply {
            contract shouldBe FutureSourceContract::class
            feature shouldBe featureId
            integration shouldBe contextualIntegrationId
            contextInput shouldBe sourceContext.id
        }
        renderEntrySourceSdkConsumerCoverageMarkdown(sourceCoverage) shouldContain "`FutureSourceContract`"
        Unit
    }

    private fun featureContribution(): FeatureContribution {
        val unavailable = FeatureContextBlocker(
            id = FeatureArtifactId("future.source.unavailable"),
            inputs = listOf(sourceContext),
        )
        return FeatureContribution(
            feature = featureId,
            owner = featureOwner,
            integrations = listOf(
                FeatureIntegration(
                    id = staticIntegrationId,
                    prerequisites = CapabilityExpression.Provided(capability),
                    behaviorProjections = listOf(behavior("future.queue.static-action")),
                    behavioralContracts = listOf(staticContract),
                    projectionRequirements = listOf(staticProjection.definition),
                    projections = listOf(staticProjection),
                ),
                FeatureIntegration(
                    id = contextualIntegrationId,
                    prerequisites = CapabilityExpression.Provided(capability),
                    contextInputs = listOf(sourceContext),
                    contextRule = featureContextRule(featureOwner) { evidence ->
                        if (evidence.value(sourceContext).available) {
                            FeatureContextDecision.Applicable
                        } else {
                            FeatureContextDecision.Blocked(listOf(unavailable))
                        }
                    },
                    contextBlockers = listOf(unavailable),
                    behaviorProjections = listOf(behavior("future.queue.contextual-action")),
                    behavioralContracts = listOf(contextualContract),
                    projectionRequirements = listOf(contextualProjection.definition),
                    projections = listOf(contextualProjection),
                ),
            ),
        )
    }

    private fun rowProjection(
        id: String,
        label: String,
        order: Int,
    ): FeatureProjection<EntryContentTypeReferenceProjection> {
        val definition = featureProjectionDefinition<EntryContentTypeReferenceProjection>(
            id = FeatureArtifactId("content-type-reference.$id"),
            owner = featureOwner,
        )
        return FeatureProjection(
            definition = definition,
            implementation = object : EntryContentTypeReferenceProjection {
                override val element = EntryContentTypeReferenceRow(
                    id = id,
                    section = EntryContentTypeReferenceSection.ENTRY_INTERACTIONS,
                    label = label,
                    order = order,
                )

                override fun project(
                    input: EntryContentTypeReferenceProjectionInput,
                ): EntryContentTypeReferenceProjectionResult {
                    input.requireMatchedProvider<FuturePlaybackProvider>()
                    return EntryContentTypeReferenceProjectionResult.Cell(
                        EntryContentTypeReferenceStatus.SUPPORTED,
                    )
                }
            },
        )
    }

    private fun contract(id: String) = object : FeatureBehaviorContract {
        override val id = FeatureArtifactId(id)
    }

    private fun behavior(id: String) = object : FeatureBehaviorProjection {
        override val id = FeatureArtifactId(id)
    }

    private class FuturePlaybackProvider

    private data class FutureSourceContext(val available: Boolean)

    private interface FutureSourceContract
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
