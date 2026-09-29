package mihon.feature.graph

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class FeatureGraphEvaluationTest {

    private val contractOwner = ContributionOwner("example.contract")
    private val featureOwner = ContributionOwner("example.feature")
    private val alpha = capabilityDefinition<AlphaProvider>(CapabilityId("example.alpha"), contractOwner)
    private val beta = capabilityDefinition<BetaProvider>(CapabilityId("example.beta"), contractOwner)
    private val gamma = capabilityDefinition<GammaProvider>(CapabilityId("example.gamma"), contractOwner)

    @Test
    fun `capability expressions evaluate all any and always without product knowledge`() {
        val alphaProvider = CapabilityProvider(alpha, AlphaProvider())
        val gammaProvider = CapabilityProvider(gamma, GammaProvider())
        val providers = listOf(gammaProvider, alphaProvider)

        val satisfied = allOf(
            CapabilityExpression.Always,
            CapabilityExpression.Provided(alpha),
            anyOf(
                CapabilityExpression.Provided(beta),
                CapabilityExpression.Provided(gamma),
            ),
        ).evaluateAgainst(providers)

        satisfied.isSatisfied shouldBe true
        satisfied.matchedProviders shouldContainExactly listOf(alphaProvider, gammaProvider)
        satisfied.unmetRequirements shouldBe emptyList()

        val missingAlternative = anyOf(
            CapabilityExpression.Provided(beta),
            CapabilityExpression.Provided(gamma),
        )
        val unsatisfied = allOf(
            CapabilityExpression.Provided(alpha),
            missingAlternative,
        ).evaluateAgainst(listOf(alphaProvider))

        unsatisfied.isSatisfied shouldBe false
        unsatisfied.matchedProviders shouldContainExactly listOf(alphaProvider)
        unsatisfied.unmetRequirements shouldContainExactly listOf(missingAlternative)
    }

    @Test
    fun `a missing specialized adapter becomes an obligation and a supplied one completes the integration`() {
        val adapter = specializedAdapterDefinition<ExampleAdapter>(
            id = SpecializedAdapterId("example.adapter"),
            owner = featureOwner,
        )
        val type = contentType("subject", CapabilityProvider(alpha, AlphaProvider()))
        val evaluation = evaluate(
            contentTypes = listOf(type),
            integrations = listOf(
                integration(
                    id = "example.integration",
                    prerequisites = CapabilityExpression.Provided(alpha),
                    specializedRequirements = listOf(adapter),
                ),
            ),
        )

        val result = evaluation.integrations.single() as IncompleteFeatureIntegration
        result.obligations shouldHaveSize 1
        result.obligations.single().responsibleOwner shouldBe type.owner
        result.obligations.single().subject.entryContentType shouldBe type.contentType
        result.obligations.single().subject.feature shouldBe FeatureId("example")
        result.obligations.single().requirement shouldBe adapter
        evaluation.obligations shouldContainExactly result.obligations
        evaluation.behaviorProjections shouldBe emptyList()

        val supplied = SpecializedAdapter(adapter, ExampleAdapter())
        val completed = evaluate(
            contentTypes = listOf(
                contentType(
                    "subject",
                    CapabilityProvider(alpha, AlphaProvider()),
                    specializedAdapters = listOf(supplied),
                ),
            ),
            integrations = listOf(
                integration(
                    id = "example.integration",
                    prerequisites = CapabilityExpression.Provided(alpha),
                    specializedRequirements = listOf(adapter),
                ),
            ),
        )

        val applicable = completed.integrations.single() as ApplicableFeatureIntegration
        applicable.suppliedAdapters shouldContainExactly listOf(supplied)
        completed.obligations shouldBe emptyList()
        completed.behaviorProjections shouldHaveSize 1
    }

    private fun evaluate(
        contentTypes: List<ContentTypeContribution>,
        integrations: List<FeatureIntegration>,
    ): FeatureGraphEvaluation {
        val feature = FeatureContribution(
            feature = FeatureId("example"),
            owner = featureOwner,
            integrations = integrations,
        )
        val graph = assembleFeatureGraph(
            DiscoveredFeatureGraphContributions(
                contentTypes = contentTypes,
                features = listOf(feature),
            ),
        )
        return evaluateFeatureGraph(graph)
    }

    private fun contentType(
        id: String,
        vararg providers: CapabilityProvider<*>,
        specializedAdapters: List<SpecializedAdapter<*>> = emptyList(),
    ): ContentTypeContribution {
        return ContentTypeContribution(
            contentType = ContentTypeId(id),
            owner = ContributionOwner("$id.type"),
            providers = providers.toList(),
            specializedAdapters = specializedAdapters,
        )
    }

    private fun integration(
        id: String,
        prerequisites: CapabilityExpression,
        specializedPrerequisites: List<SpecializedAdapterDefinition<*>> = emptyList(),
        specializedRequirements: List<SpecializedAdapterDefinition<*>> = emptyList(),
        behavior: FeatureBehaviorProjection = behavior("$id.projection"),
    ): FeatureIntegration {
        return FeatureIntegration(
            id = FeatureIntegrationId(id),
            prerequisites = prerequisites,
            specializedPrerequisites = specializedPrerequisites,
            specializedRequirements = specializedRequirements,
            behaviorProjections = listOf(behavior),
        )
    }

    private fun behavior(id: String) = object : FeatureBehaviorProjection {
        override val id = FeatureArtifactId(id)
    }

    private class AlphaProvider

    private class BetaProvider

    private class GammaProvider

    private class ExampleAdapter
}
