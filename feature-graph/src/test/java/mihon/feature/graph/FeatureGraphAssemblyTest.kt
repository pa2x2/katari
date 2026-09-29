package mihon.feature.graph

import io.kotest.matchers.collections.shouldContainExactly
import org.junit.jupiter.api.Test

class FeatureGraphAssemblyTest {

    private val contractOwner = ContributionOwner("example.contract")
    private val featureOwner = ContributionOwner("example.feature")
    private val alpha = capabilityDefinition<AlphaProvider>(CapabilityId("example.alpha"), contractOwner)

    @Test
    fun `graph ordering is deterministic across contributor order`() {
        val typesOwner = ContributionOwner("example.types")
        val types = featureGraphContributor(typesOwner) {
            add(type("zeta", typesOwner))
            add(type("alpha", typesOwner))
        }
        val feature = featureContributor(alpha)

        val forward = discoverAndAssembleFeatureGraph(listOf(types, feature))
        val reverse = discoverAndAssembleFeatureGraph(listOf(feature, types))

        forward shouldContainSameGraphAs reverse
        forward.entryContentTypes.map { it.contentType.value } shouldContainExactly listOf("alpha", "zeta")
    }

    private fun type(
        id: String,
        owner: ContributionOwner = ContributionOwner("$id.type"),
    ): ContentTypeContribution {
        return ContentTypeContribution(
            contentType = ContentTypeId(id),
            owner = owner,
            providers = listOf(CapabilityProvider(alpha, AlphaProvider())),
        )
    }

    private fun featureContributor(capability: CapabilityDefinition<*>): FeatureGraphContributor {
        return featureGraphContributor(featureOwner) { add(feature(capability)) }
    }

    private fun feature(
        capability: CapabilityDefinition<*>,
        id: String = "example-feature",
    ): FeatureContribution {
        return FeatureContribution(
            feature = FeatureId(id),
            owner = featureOwner,
            integrations = listOf(
                FeatureIntegration(
                    id = FeatureIntegrationId("$id.integration"),
                    prerequisites = CapabilityExpression.Provided(capability),
                    behaviorProjections = listOf(behavior("$id.projection")),
                ),
            ),
        )
    }

    private fun behavior(id: String) = object : FeatureBehaviorProjection {
        override val id = FeatureArtifactId(id)
    }

    private infix fun FeatureGraph.shouldContainSameGraphAs(other: FeatureGraph) {
        entryContentTypes.map { it.contentType } shouldContainExactly other.entryContentTypes.map { it.contentType }
        features.map { it.feature } shouldContainExactly other.features.map { it.feature }
        executionPoints.map { it.id } shouldContainExactly other.executionPoints.map { it.id }
        executionParticipants.map { it.id } shouldContainExactly other.executionParticipants.map { it.id }
        capabilities.map { it.id } shouldContainExactly other.capabilities.map { it.id }
        contractFixtures.map { it.id } shouldContainExactly other.contractFixtures.map { it.id }
        projections.map { it.id } shouldContainExactly other.projections.map { it.id }
    }

    private class AlphaProvider
}
