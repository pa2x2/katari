package mihon.feature.graph

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class FeatureContextResolutionTest {

    private val capabilityOwner = ContributionOwner("example.capability")
    private val contextOwner = ContributionOwner("example.context")
    private val featureOwner = ContributionOwner("example.feature")
    private val alpha = capabilityDefinition<AlphaProvider>(CapabilityId("example.alpha"), capabilityOwner)
    private val source = contextInputDefinition<SourceContext>(ContextInputId("example.source"), contextOwner)
    private val preference = contextInputDefinition<PreferenceContext>(
        ContextInputId("example.preference"),
        contextOwner,
    )
    private val adapter = specializedAdapterDefinition<ExampleAdapter>(
        SpecializedAdapterId("example.adapter"),
        featureOwner,
    )
    private val behavior = behavior("example.projection")
    private val sourceUnsupported = blocker("example.source-unsupported", source)
    private val disabled = blocker("example.disabled", preference)

    @Test
    fun `missing evidence remains distinct from a contextual blocker`() {
        val candidate = candidate(
            contentType(adapters = listOf(SpecializedAdapter(adapter, ExampleAdapter()))),
        )

        val missing = resolveFeatureContext(
            candidate,
            listOf(contextEvidence(source, SourceContext(supported = true))),
        )

        val missingResult = missing.integration as MissingFeatureContextEvidence
        missingResult.missingInputs shouldContainExactly listOf(preference)
        missingResult.evidence.map { it.input } shouldContainExactly listOf(source)
        missing.obligations shouldBe emptyList()
        missing.behaviorProjections shouldBe emptyList()

        val blocked = resolveFeatureContext(
            candidate,
            listOf(
                contextEvidence(preference, PreferenceContext(enabled = false)),
                contextEvidence(source, SourceContext(supported = true)),
            ),
        )

        val blockedResult = blocked.integration as BlockedFeatureContext
        blockedResult.blockers.map { it.id } shouldContainExactly listOf(FeatureArtifactId("example.disabled"))
        blockedResult.blockers.single().inputs shouldContainExactly listOf(preference)
        blockedResult.evidence.map { it.input } shouldContainExactly listOf(preference, source)
        blocked.obligations shouldBe emptyList()
        blocked.behaviorProjections shouldBe emptyList()
    }

    @Test
    fun `applicable context exposes a missing adapter as an obligation and activates with a supplied one`() {
        val candidate = candidate(contentType())

        val evaluated = resolveFeatureContext(candidate, applicableEvidence())

        val result = evaluated.integration as IncompleteFeatureContext
        result.suppliedAdapters shouldBe emptyList()
        result.obligations shouldHaveSize 1
        result.obligations.single().responsibleOwner shouldBe ContributionOwner("example.type")
        result.obligations.single().requirement shouldBe adapter
        evaluated.obligations shouldContainExactly result.obligations
        evaluated.behaviorProjections shouldBe emptyList()

        val suppliedAdapter = SpecializedAdapter(adapter, ExampleAdapter())
        val suppliedCandidate = candidate(contentType(adapters = listOf(suppliedAdapter)))

        val activated = resolveFeatureContext(suppliedCandidate, applicableEvidence())

        val applicable = activated.integration as ApplicableFeatureContext
        applicable.suppliedAdapters shouldContainExactly listOf(suppliedAdapter)
        applicable.evidence.map { it.input } shouldContainExactly listOf(preference, source)
        activated.obligations shouldBe emptyList()
        activated.behaviorProjections.map { it.projection } shouldContainExactly listOf(behavior)
        activated.behaviorProjections.single().subject shouldBe suppliedCandidate.subject
    }

    private fun candidate(
        type: ContentTypeContribution,
        integration: FeatureIntegration = integration(),
    ): ConditionalFeatureIntegration {
        val graph = assembleFeatureGraph(
            DiscoveredFeatureGraphContributions(
                contentTypes = listOf(type),
                features = listOf(
                    FeatureContribution(
                        feature = FeatureId("example"),
                        owner = featureOwner,
                        integrations = listOf(integration),
                    ),
                ),
            ),
        )
        val evaluation = evaluateFeatureGraph(graph)
        evaluation.candidateBehaviorProjections.map { it.projection } shouldContainExactly listOf(behavior)
        evaluation.behaviorProjections shouldBe emptyList()
        evaluation.obligations shouldBe emptyList()
        return evaluation.integrations.single() as ConditionalFeatureIntegration
    }

    private fun integration(
        rule: FeatureContextRule = featureContextRule(featureOwner) { evidence ->
            when {
                !evidence.value(source).supported -> FeatureContextDecision.Blocked(listOf(sourceUnsupported))
                !evidence.value(preference).enabled -> FeatureContextDecision.Blocked(listOf(disabled))
                else -> FeatureContextDecision.Applicable
            }
        },
    ): FeatureIntegration {
        return FeatureIntegration(
            id = FeatureIntegrationId("example.integration"),
            prerequisites = CapabilityExpression.Provided(alpha),
            contextInputs = listOf(source, preference),
            contextRule = rule,
            contextBlockers = listOf(sourceUnsupported, disabled),
            specializedRequirements = listOf(adapter),
            behaviorProjections = listOf(behavior),
        )
    }

    private fun contentType(
        adapters: List<SpecializedAdapter<*>> = emptyList(),
    ): ContentTypeContribution {
        return ContentTypeContribution(
            contentType = ContentTypeId("example"),
            owner = ContributionOwner("example.type"),
            providers = listOf(CapabilityProvider(alpha, AlphaProvider())),
            specializedAdapters = adapters,
        )
    }

    private fun applicableEvidence(): List<ContextEvidence<*>> = listOf(
        contextEvidence(source, SourceContext(supported = true)),
        contextEvidence(preference, PreferenceContext(enabled = true)),
    )

    private fun blocker(
        value: String,
        vararg inputs: ContextInputDefinition<*>,
    ): FeatureContextBlocker = FeatureContextBlocker(FeatureArtifactId(value), inputs.toList())

    private fun behavior(value: String): FeatureBehaviorProjection {
        return object : FeatureBehaviorProjection {
            override val id = FeatureArtifactId(value)
        }
    }

    private data class SourceContext(val supported: Boolean)

    private data class PreferenceContext(val enabled: Boolean)

    private class AlphaProvider

    private class ExampleAdapter
}
