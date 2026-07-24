package mihon.entry.interactions.documentation.source

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.ContextInputId
import mihon.feature.graph.DiscoveredFeatureGraphContributions
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorProjection
import mihon.feature.graph.FeatureContribution
import mihon.feature.graph.FeatureId
import mihon.feature.graph.FeatureIntegration
import mihon.feature.graph.FeatureIntegrationId
import mihon.feature.graph.assembleFeatureGraph
import mihon.feature.graph.contextInputDefinition
import mihon.feature.graph.featureContextRule
import org.junit.jupiter.api.Test

class EntrySourceSdkConsumerCoveragePlannerTest {
    @Test
    fun `discovers future contextual consumers from source context metadata`() {
        val context = entrySourceContextInputDefinition<Boolean>(
            id = ContextInputId("future.source.capability"),
            contracts = setOf(FutureSourceContract::class),
        )
        val graph = graph(context)

        val plan = planEntrySourceSdkConsumerCoverage(graph)

        plan.isComplete shouldBe true
        plan.consumers.shouldContainExactly(
            EntrySourceSdkContextConsumer(
                contract = FutureSourceContract::class,
                feature = FeatureId("future.feature"),
                featureOwner = FUTURE_FEATURE_OWNER,
                integration = FeatureIntegrationId("future.feature.context"),
                contextInput = context.id,
            ),
        )
    }

    @Test
    fun `requires source owned contexts to classify contract coverage`() {
        val context = contextInputDefinition<Boolean>(
            id = ContextInputId("future.source.unclassified"),
            owner = ENTRY_SOURCE_CONTEXT_OWNER,
        )

        val plan = planEntrySourceSdkConsumerCoverage(graph(context))

        plan.isComplete shouldBe false
        plan.issues.single().apply {
            responsibleOwner shouldBe ENTRY_SOURCE_CONTEXT_OWNER
            contextInput shouldBe context.id
            details shouldBe
                "Source-owned context input future.source.unclassified does not classify SDK contract coverage for " +
                "integration future.feature.context"
        }
    }

    @Test
    fun `requires classification when an existing source input enters a future integration`() {
        val context = entrySourceContextInputDefinition<Boolean>(
            id = ContextInputId("future.source.scoped"),
            contracts = setOf(FutureSourceContract::class),
            contractIntegrations = mapOf(
                FutureSourceContract::class to setOf(FeatureIntegrationId("earlier.integration")),
            ),
        )

        val plan = planEntrySourceSdkConsumerCoverage(graph(context))

        plan.isComplete shouldBe false
        plan.issues.single().details shouldBe
            "Source-owned context input future.source.scoped does not classify SDK contract coverage for integration " +
            "future.feature.context"
    }

    @Test
    fun `accepts explicit non contract source context`() {
        val context = entrySourceContextInputDefinition<Boolean>(
            id = ContextInputId("future.source.registration"),
            nonContractReason = "Runtime registration state",
        )

        val plan = planEntrySourceSdkConsumerCoverage(graph(context))

        plan.isComplete shouldBe true
        plan.consumers shouldBe emptyList()
        plan.exclusions.single().reason shouldBe "Runtime registration state"
    }

    private fun graph(context: mihon.feature.graph.ContextInputDefinition<*>) = assembleFeatureGraph(
        DiscoveredFeatureGraphContributions(
            contentTypes = emptyList(),
            features = listOf(
                FeatureContribution(
                    feature = FeatureId("future.feature"),
                    owner = FUTURE_FEATURE_OWNER,
                    integrations = listOf(
                        FeatureIntegration(
                            id = FeatureIntegrationId("future.feature.context"),
                            prerequisites = CapabilityExpression.Always,
                            contextInputs = listOf(context),
                            contextRule = featureContextRule(FUTURE_FEATURE_OWNER) {
                                mihon.feature.graph.FeatureContextDecision.Applicable
                            },
                            behaviorProjections = listOf(FutureBehavior),
                        ),
                    ),
                ),
            ),
        ),
    )

    private interface FutureSourceContract

    private object FutureBehavior : FeatureBehaviorProjection {
        override val id = FeatureArtifactId("future.behavior")
    }

    private companion object {
        val FUTURE_FEATURE_OWNER = mihon.feature.graph.ContributionOwner("future.feature")
    }
}
