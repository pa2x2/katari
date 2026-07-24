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
    fun `missing capability prerequisite is inapplicable and creates no obligation`() {
        val adapter = specializedAdapterDefinition<ExampleAdapter>(
            id = SpecializedAdapterId("example.adapter"),
            owner = featureOwner,
        )
        val evaluation = evaluate(
            contentTypes = listOf(contentType("subject")),
            integrations = listOf(
                integration(
                    id = "example.integration",
                    prerequisites = CapabilityExpression.Provided(beta),
                    specializedRequirements = listOf(adapter),
                ),
            ),
        )

        val result = evaluation.integrations.single() as InapplicableFeatureIntegration
        result.matchedProviders shouldBe emptyList()
        result.unmetPrerequisites shouldContainExactly listOf(CapabilityExpression.Provided(beta))
        evaluation.obligations shouldBe emptyList()
        evaluation.behaviorProjections shouldBe emptyList()
    }

    @Test
    fun `missing specialized adapter becomes an obligation after prerequisites are satisfied`() {
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
        result.obligations.single().subject.contentType shouldBe type.contentType
        result.obligations.single().subject.feature shouldBe FeatureId("example")
        result.obligations.single().requirement shouldBe adapter
        evaluation.obligations shouldContainExactly result.obligations
        evaluation.behaviorProjections shouldBe emptyList()
    }

    @Test
    fun `missing specialized prerequisite is inapplicable without creating an obligation`() {
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
                    specializedPrerequisites = listOf(adapter),
                ),
            ),
        )

        val result = evaluation.integrations.single() as InapplicableFeatureIntegration
        result.matchedProviders shouldHaveSize 1
        result.unmetPrerequisites shouldBe emptyList()
        result.unmetSpecializedPrerequisites shouldContainExactly listOf(adapter)
        evaluation.obligations shouldBe emptyList()
        evaluation.behaviorProjections shouldBe emptyList()
    }

    @Test
    fun `supplied specialized prerequisite participates in contextual selection`() {
        val adapter = specializedAdapterDefinition<ExampleAdapter>(
            id = SpecializedAdapterId("example.adapter"),
            owner = featureOwner,
        )
        val context = contextInputDefinition<ExampleContext>(
            id = ContextInputId("example.context"),
            owner = featureOwner,
        )
        val supplied = SpecializedAdapter(adapter, ExampleAdapter())
        val evaluation = evaluate(
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
                    contextInputs = listOf(context),
                    specializedPrerequisites = listOf(adapter),
                ),
            ),
        )

        val candidate = evaluation.integrations.single() as ConditionalFeatureIntegration
        candidate.suppliedAdapters shouldContainExactly listOf(supplied)
        candidate.pendingSpecializedRequirements shouldBe emptyList()

        val resolved = resolveFeatureContext(candidate, listOf(contextEvidence(context, ExampleContext())))
        val applicable = resolved.integration as ApplicableFeatureContext
        applicable.suppliedAdapters shouldContainExactly listOf(supplied)
    }

    @Test
    fun `unresolved context remains conditional without activating work or obligations`() {
        val context = contextInputDefinition<ExampleContext>(
            id = ContextInputId("example.context"),
            owner = featureOwner,
        )
        val adapter = specializedAdapterDefinition<ExampleAdapter>(
            id = SpecializedAdapterId("example.adapter"),
            owner = featureOwner,
        )
        val evaluation = evaluate(
            contentTypes = listOf(contentType("subject", CapabilityProvider(alpha, AlphaProvider()))),
            integrations = listOf(
                integration(
                    id = "example.integration",
                    prerequisites = CapabilityExpression.Provided(alpha),
                    contextInputs = listOf(context),
                    specializedRequirements = listOf(adapter),
                ),
            ),
        )

        val result = evaluation.integrations.single() as ConditionalFeatureIntegration
        result.unresolvedContextInputs shouldContainExactly listOf(context)
        result.suppliedAdapters shouldBe emptyList()
        result.pendingSpecializedRequirements shouldContainExactly listOf(adapter)
        evaluation.obligations shouldBe emptyList()
        evaluation.behaviorProjections shouldBe emptyList()
        evaluation.candidateBehaviorProjections shouldHaveSize 1
    }

    @Test
    fun `supplied adapter completes applicable integration`() {
        val adapter = specializedAdapterDefinition<ExampleAdapter>(
            id = SpecializedAdapterId("example.adapter"),
            owner = featureOwner,
        )
        val supplied = SpecializedAdapter(adapter, ExampleAdapter())
        val evaluation = evaluate(
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

        val result = evaluation.integrations.single() as ApplicableFeatureIntegration
        result.suppliedAdapters shouldContainExactly listOf(supplied)
        evaluation.obligations shouldBe emptyList()
        evaluation.behaviorProjections shouldHaveSize 1
    }

    @Test
    fun `compatible types create edges to one behavior projection instance`() {
        val behavior = behavior("example.shared-gate")
        val evaluation = evaluate(
            contentTypes = listOf(
                contentType("zeta", CapabilityProvider(alpha, AlphaProvider())),
                contentType("alpha", CapabilityProvider(alpha, AlphaProvider())),
            ),
            integrations = listOf(
                integration(
                    id = "example.integration",
                    prerequisites = CapabilityExpression.Provided(alpha),
                    behavior = behavior,
                ),
            ),
        )

        evaluation.behaviorProjections.map { it.subject.contentType.value } shouldContainExactly listOf("alpha", "zeta")
        evaluation.behaviorProjections.all { it.projection === behavior } shouldBe true
    }

    @Test
    fun `evaluation ordering is derived and deterministic`() {
        val evaluation = evaluate(
            contentTypes = listOf(
                contentType("zeta", CapabilityProvider(alpha, AlphaProvider())),
                contentType("alpha", CapabilityProvider(alpha, AlphaProvider())),
            ),
            integrations = listOf(
                integration("example.zeta", CapabilityExpression.Provided(alpha)),
                integration("example.alpha", CapabilityExpression.Provided(alpha)),
            ),
        )

        evaluation.integrations.map {
            "${it.subject.contentType.value}:${it.subject.integration.value}"
        } shouldContainExactly listOf(
            "alpha:example.alpha",
            "alpha:example.zeta",
            "zeta:example.alpha",
            "zeta:example.zeta",
        )
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
        contextInputs: List<ContextInputDefinition<*>> = emptyList(),
        specializedPrerequisites: List<SpecializedAdapterDefinition<*>> = emptyList(),
        specializedRequirements: List<SpecializedAdapterDefinition<*>> = emptyList(),
        behavior: FeatureBehaviorProjection = behavior("$id.projection"),
    ): FeatureIntegration {
        return FeatureIntegration(
            id = FeatureIntegrationId(id),
            prerequisites = prerequisites,
            contextInputs = contextInputs,
            contextRule = if (contextInputs.isEmpty()) {
                null
            } else {
                featureContextRule(featureOwner) { FeatureContextDecision.Applicable }
            },
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

    private class ExampleContext
}
