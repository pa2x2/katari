package mihon.feature.graph

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class FeatureArtifactSelectionTest {

    private val contractOwner = ContributionOwner("example.contract")
    private val featureOwner = ContributionOwner("example.feature")
    private val alpha = capabilityDefinition<AlphaProvider>(CapabilityId("example.alpha"), contractOwner)

    @Test
    fun `applicable contributions select the same feature owned artifacts`() {
        val adapterDefinition = specializedAdapterDefinition<ExampleAdapter>(
            id = SpecializedAdapterId("example.adapter"),
            owner = featureOwner,
        )
        val contract = TestContract("example.behavior")
        val projectionDefinition = featureProjectionDefinition<TestProjection>(
            id = FeatureArtifactId("example.reference"),
            owner = featureOwner,
        )
        val projectionImplementation = TestProjection()
        val projection = FeatureProjection(projectionDefinition, projectionImplementation)
        val graph = graph(
            contentTypes = listOf(
                contentType(
                    id = "complete",
                    providers = listOf(CapabilityProvider(alpha, AlphaProvider())),
                    adapters = listOf(SpecializedAdapter(adapterDefinition, ExampleAdapter())),
                ),
                contentType(
                    id = "second",
                    providers = listOf(CapabilityProvider(alpha, AlphaProvider())),
                    adapters = listOf(SpecializedAdapter(adapterDefinition, ExampleAdapter())),
                ),
                contentType(
                    id = "incomplete",
                    providers = listOf(CapabilityProvider(alpha, AlphaProvider())),
                ),
                contentType(id = "unsupported"),
            ),
            integration = FeatureIntegration(
                id = FeatureIntegrationId("example.integration"),
                prerequisites = CapabilityExpression.Provided(alpha),
                specializedRequirements = listOf(adapterDefinition),
                behavioralContracts = listOf(contract),
                projectionRequirements = listOf(projectionDefinition),
                projections = listOf(projection),
            ),
        )

        val selected = selectFeatureArtifacts(graph, evaluateFeatureGraph(graph))

        selected.behavioralContracts.map { it.subject.entryContentType.value } shouldContainExactly
            listOf("complete", "second")
        selected.projections.map { it.subject.entryContentType.value } shouldContainExactly listOf("complete", "second")
        selected.behavioralContracts.all { it.contract === contract } shouldBe true
        selected.projections.all { it.projection === projection } shouldBe true
        selected.projections.all { it.projection.implementation === projectionImplementation } shouldBe true
        selected.projections.all { selection ->
            selection.matchedProviders.single().capability == alpha &&
                selection.suppliedAdapters.single().definition == adapterDefinition &&
                selection.contextEvidence.isEmpty()
        } shouldBe true
        selected.obligations shouldBe emptyList()
    }

    private fun graph(
        contentTypes: List<ContentTypeContribution>,
        integration: FeatureIntegration,
    ): FeatureGraph {
        return assembleFeatureGraph(
            DiscoveredFeatureGraphContributions(
                contentTypes = contentTypes,
                features = listOf(
                    FeatureContribution(
                        feature = FeatureId("example"),
                        owner = featureOwner,
                        integrations = listOf(integration),
                    ),
                ),
            ),
        )
    }

    private fun contentType(
        id: String,
        providers: List<CapabilityProvider<*>> = emptyList(),
        adapters: List<SpecializedAdapter<*>> = emptyList(),
    ): ContentTypeContribution {
        return ContentTypeContribution(
            contentType = ContentTypeId(id),
            owner = ContributionOwner("$id.type"),
            providers = providers,
            specializedAdapters = adapters,
        )
    }

    private class TestContract(id: String) : FeatureBehaviorContract {
        override val id = FeatureArtifactId(id)
    }

    private class TestProjection

    private class AlphaProvider

    private class ExampleAdapter
}
