package mihon.feature.graph

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
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
    fun `applicable context exposes delayed specialized obligation`() {
        val candidate = candidate(contentType())

        val evaluated = resolveFeatureContext(candidate, applicableEvidence())

        val result = evaluated.integration as IncompleteFeatureContext
        result.suppliedAdapters shouldBe emptyList()
        result.obligations shouldHaveSize 1
        result.obligations.single().responsibleOwner shouldBe ContributionOwner("example.type")
        result.obligations.single().requirement shouldBe adapter
        evaluated.obligations shouldContainExactly result.obligations
        evaluated.behaviorProjections shouldBe emptyList()
    }

    @Test
    fun `applicable context activates behavior projections with supplied adapters`() {
        val suppliedAdapter = SpecializedAdapter(adapter, ExampleAdapter())
        val candidate = candidate(contentType(adapters = listOf(suppliedAdapter)))

        val evaluated = resolveFeatureContext(candidate, applicableEvidence())

        val result = evaluated.integration as ApplicableFeatureContext
        result.suppliedAdapters shouldContainExactly listOf(suppliedAdapter)
        result.evidence.map { it.input } shouldContainExactly listOf(preference, source)
        evaluated.obligations shouldBe emptyList()
        evaluated.behaviorProjections.map { it.projection } shouldContainExactly listOf(behavior)
        evaluated.behaviorProjections.single().subject shouldBe candidate.subject
    }

    @Test
    fun `context is reevaluated from each immutable evidence snapshot`() {
        val candidate = candidate(
            contentType(adapters = listOf(SpecializedAdapter(adapter, ExampleAdapter()))),
        )

        resolveFeatureContext(
            candidate,
            listOf(
                contextEvidence(source, SourceContext(supported = false)),
                contextEvidence(preference, PreferenceContext(enabled = true)),
            ),
        ).integration.shouldBeInstanceOf<BlockedFeatureContext>()

        resolveFeatureContext(candidate, applicableEvidence()).integration
            .shouldBeInstanceOf<ApplicableFeatureContext>()
    }

    @Test
    fun `resolution rejects unexpected contradictory and undeclared evidence access`() {
        val candidate = candidate(
            contentType(adapters = listOf(SpecializedAdapter(adapter, ExampleAdapter()))),
        )
        val other = contextInputDefinition<SourceContext>(ContextInputId("example.other"), contextOwner)
        val contradictory = contextInputDefinition<SourceContext>(source.id, ContributionOwner("other.context"))

        shouldThrow<IllegalArgumentException> {
            resolveFeatureContext(candidate, applicableEvidence() + contextEvidence(other, SourceContext(true)))
        }.message shouldContain "Unexpected context input"

        shouldThrow<IllegalArgumentException> {
            resolveFeatureContext(
                candidate,
                listOf(
                    contextEvidence(contradictory, SourceContext(true)),
                    contextEvidence(preference, PreferenceContext(true)),
                ),
            )
        }.message shouldContain "Contradictory context input"

        val integration = integration(
            rule = featureContextRule(featureOwner) { evidence ->
                evidence.value(other)
                FeatureContextDecision.Applicable
            },
        )
        val undeclaredCandidate = candidate(contentType(), integration)
        shouldThrow<IllegalArgumentException> {
            resolveFeatureContext(undeclaredCandidate, applicableEvidence())
        }.message shouldContain "undeclared input"

        val inventedBlocker = blocker("example.invented", source)
        val inventedBlockerCandidate = candidate(
            contentType(),
            integration(
                rule = featureContextRule(featureOwner) {
                    FeatureContextDecision.Blocked(listOf(inventedBlocker))
                },
            ),
        )
        shouldThrow<IllegalArgumentException> {
            resolveFeatureContext(inventedBlockerCandidate, applicableEvidence())
        }.message shouldContain "undeclared blocker"
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
