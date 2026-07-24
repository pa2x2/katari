package mihon.feature.graph

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class UnknownContributionAcceptanceTest {

    @Test
    fun `unknown contributions enter behaviors obligations contracts and projections without curated edits`() {
        val contractOwner = ContributionOwner("example.contract")
        val typesOwner = ContributionOwner("example.types")
        val featureOwner = ContributionOwner("example.feature")
        val alpha = capabilityDefinition<AlphaProvider>(CapabilityId("example.alpha"), contractOwner)
        val adapterDefinition = specializedAdapterDefinition<ExampleAdapter>(
            id = SpecializedAdapterId("example.adapter"),
            owner = featureOwner,
        )
        val behavior = behavior("example.projection")
        val contract = RecordingContract()
        val projectionDefinition = featureProjectionDefinition<RecordingProjection>(
            id = FeatureArtifactId("example.projection"),
            owner = featureOwner,
        )
        val projectionImplementation = RecordingProjection()
        val projection = FeatureProjection(projectionDefinition, projectionImplementation)
        val types = mutableListOf(
            contentType("existing", typesOwner, alpha, adapterDefinition),
            ContentTypeContribution(ContentTypeId("partial"), typesOwner),
        )
        val typeContributor = featureGraphContributor(typesOwner) { types.forEach(::add) }
        val featureContributor = featureGraphContributor(featureOwner) {
            add(
                FeatureContribution(
                    feature = FeatureId("example"),
                    owner = featureOwner,
                    integrations = listOf(
                        FeatureIntegration(
                            id = FeatureIntegrationId("example.integration"),
                            prerequisites = CapabilityExpression.Provided(alpha),
                            specializedRequirements = listOf(adapterDefinition),
                            behaviorProjections = listOf(behavior),
                            behavioralContracts = listOf(contract),
                            projectionRequirements = listOf(projectionDefinition),
                            projections = listOf(projection),
                        ),
                    ),
                ),
            )
        }
        val contributors = listOf(typeContributor, featureContributor)

        val initial = discoverAndAssembleFeatureGraph(contributors)
            .let { graph -> graph to evaluateFeatureGraph(graph) }
        initial.second.behaviorProjections.map { it.subject.contentType.value } shouldContainExactly listOf("existing")
        initial.second.obligations shouldBe emptyList()

        types += contentType("future-complete", typesOwner, alpha, adapterDefinition)
        types += ContentTypeContribution(
            contentType = ContentTypeId("future-incomplete"),
            owner = typesOwner,
            providers = listOf(CapabilityProvider(alpha, AlphaProvider())),
        )

        val expandedGraph = discoverAndAssembleFeatureGraph(contributors)
        val expandedEvaluation = evaluateFeatureGraph(expandedGraph)
        val selected = selectFeatureArtifacts(expandedGraph, expandedEvaluation)

        expandedEvaluation.behaviorProjections.map { it.subject.contentType.value } shouldContainExactly listOf(
            "existing",
            "future-complete",
        )
        expandedEvaluation.behaviorProjections.all { it.projection === behavior } shouldBe true
        expandedEvaluation.obligations.map { it.subject.contentType.value } shouldContainExactly listOf(
            "future-incomplete",
        )
        selected.behavioralContracts.map { it.subject.contentType.value } shouldContainExactly listOf(
            "existing",
            "future-complete",
        )
        selected.projections.map { it.subject.contentType.value } shouldContainExactly listOf(
            "existing",
            "future-complete",
        )
        selected.behavioralContracts.forEach { (it.contract as RecordingContract).execute(it.subject) }
        selected.projections.forEach {
            (it.projection.implementation as RecordingProjection).project(it.subject)
        }
        contract.subjects shouldContainExactly listOf(ContentTypeId("existing"), ContentTypeId("future-complete"))
        projectionImplementation.subjects shouldContainExactly listOf(
            ContentTypeId("existing"),
            ContentTypeId("future-complete"),
        )
    }

    @Test
    fun `unknown contextual integration resolves through unchanged discovery path`() {
        val typesOwner = ContributionOwner("future.types")
        val capabilityOwner = ContributionOwner("future.capability")
        val contextOwner = ContributionOwner("future.context")
        val featureOwner = ContributionOwner("future.feature")
        val capability = capabilityDefinition<FutureProvider>(CapabilityId("future.capability"), capabilityOwner)
        val context = contextInputDefinition<FutureContext>(ContextInputId("future.context"), contextOwner)
        val adapter = specializedAdapterDefinition<FutureAdapter>(
            SpecializedAdapterId("future.adapter"),
            featureOwner,
        )
        val behavior = behavior("future.projection")
        val blocker = FeatureContextBlocker(FeatureArtifactId("future.disabled"), listOf(context))
        val types = mutableListOf<ContentTypeContribution>()
        val features = mutableListOf<FeatureContribution>()
        val contributors = listOf(
            featureGraphContributor(typesOwner) { types.forEach(::add) },
            featureGraphContributor(featureOwner) { features.forEach(::add) },
        )

        types += ContentTypeContribution(ContentTypeId("future.empty"), typesOwner)
        features += FeatureContribution(
            feature = FeatureId("future.baseline"),
            owner = featureOwner,
            integrations = listOf(
                FeatureIntegration(
                    id = FeatureIntegrationId("future.baseline"),
                    prerequisites = CapabilityExpression.Always,
                    behaviorProjections = listOf(behavior("future.baseline")),
                ),
            ),
        )
        discoverAndAssembleFeatureGraph(contributors)

        types += ContentTypeContribution(
            contentType = ContentTypeId("future.complete"),
            owner = typesOwner,
            providers = listOf(CapabilityProvider(capability, FutureProvider())),
            specializedAdapters = listOf(SpecializedAdapter(adapter, FutureAdapter())),
        )
        types += ContentTypeContribution(
            contentType = ContentTypeId("future.incomplete"),
            owner = typesOwner,
            providers = listOf(CapabilityProvider(capability, FutureProvider())),
        )
        features += FeatureContribution(
            feature = FeatureId("future.conditional"),
            owner = featureOwner,
            integrations = listOf(
                FeatureIntegration(
                    id = FeatureIntegrationId("future.conditional"),
                    prerequisites = CapabilityExpression.Provided(capability),
                    contextInputs = listOf(context),
                    contextRule = featureContextRule(featureOwner) { evidence ->
                        if (evidence.value(context).enabled) {
                            FeatureContextDecision.Applicable
                        } else {
                            FeatureContextDecision.Blocked(listOf(blocker))
                        }
                    },
                    contextBlockers = listOf(blocker),
                    specializedRequirements = listOf(adapter),
                    behaviorProjections = listOf(behavior),
                ),
            ),
        )

        val graph = discoverAndAssembleFeatureGraph(contributors)
        val evaluation = evaluateFeatureGraph(graph)
        evaluation.candidateBehaviorProjections
            .filter { it.subject.feature == FeatureId("future.conditional") }
            .map { it.subject.contentType } shouldContainExactly listOf(
            ContentTypeId("future.complete"),
            ContentTypeId("future.incomplete"),
        )
        evaluation.candidateBehaviorProjections
            .filter { it.subject.feature == FeatureId("future.conditional") }
            .all { it.projection === behavior } shouldBe true

        resolveFeatureContext(
            evaluation,
            ContentTypeId("future.complete"),
            FeatureId("future.conditional"),
            FeatureIntegrationId("future.conditional"),
            emptyList(),
        )
            .integration.shouldBeInstanceOf<MissingFeatureContextEvidence>()
        resolveFeatureContext(
            evaluation = evaluation,
            contentType = ContentTypeId("future.complete"),
            feature = FeatureId("future.conditional"),
            integration = FeatureIntegrationId("future.conditional"),
            evidence = listOf(contextEvidence(context, FutureContext(enabled = false))),
        ).integration.shouldBeInstanceOf<BlockedFeatureContext>()
        resolveFeatureContext(
            evaluation = evaluation,
            contentType = ContentTypeId("future.complete"),
            feature = FeatureId("future.conditional"),
            integration = FeatureIntegrationId("future.conditional"),
            evidence = listOf(contextEvidence(context, FutureContext(enabled = true))),
        ).behaviorProjections.map { it.projection } shouldContainExactly listOf(behavior)

        val incomplete = resolveFeatureContext(
            evaluation = evaluation,
            contentType = ContentTypeId("future.incomplete"),
            feature = FeatureId("future.conditional"),
            integration = FeatureIntegrationId("future.conditional"),
            evidence = listOf(contextEvidence(context, FutureContext(enabled = true))),
        )
        incomplete.integration.shouldBeInstanceOf<IncompleteFeatureContext>()
        incomplete.obligations.single().responsibleOwner shouldBe typesOwner
        incomplete.obligations.single().requirement shouldBe adapter
    }

    private fun contentType(
        id: String,
        owner: ContributionOwner,
        capability: CapabilityDefinition<AlphaProvider>,
        adapter: SpecializedAdapterDefinition<ExampleAdapter>,
    ): ContentTypeContribution {
        return ContentTypeContribution(
            contentType = ContentTypeId(id),
            owner = owner,
            providers = listOf(CapabilityProvider(capability, AlphaProvider())),
            specializedAdapters = listOf(SpecializedAdapter(adapter, ExampleAdapter())),
        )
    }

    private fun behavior(id: String) = object : FeatureBehaviorProjection {
        override val id = FeatureArtifactId(id)
    }

    private class RecordingContract : FeatureBehaviorContract {
        override val id = FeatureArtifactId("example.contract")
        val subjects = mutableListOf<ContentTypeId>()

        fun execute(subject: FeatureIntegrationSubject) {
            subjects += subject.contentType
        }
    }

    private class RecordingProjection {
        val subjects = mutableListOf<ContentTypeId>()

        fun project(subject: FeatureIntegrationSubject) {
            subjects += subject.contentType
        }
    }

    private class AlphaProvider

    private class ExampleAdapter

    private data class FutureContext(val enabled: Boolean)

    private class FutureProvider

    private class FutureAdapter
}
