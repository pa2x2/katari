package mihon.feature.graph.validation.projection

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.ContentTypeContribution
import mihon.feature.graph.ContentTypeId
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.DiscoveredFeatureGraphContributions
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorProjection
import mihon.feature.graph.FeatureContribution
import mihon.feature.graph.FeatureId
import mihon.feature.graph.FeatureIntegration
import mihon.feature.graph.FeatureIntegrationId
import mihon.feature.graph.FeatureProjection
import mihon.feature.graph.assembleFeatureGraph
import mihon.feature.graph.featureProjectionDefinition
import mihon.feature.graph.featureProjectionExclusion
import org.junit.jupiter.api.Test

class FeatureProjectionParticipationTest {
    private val includedOwner = ContributionOwner("future-included-owner")
    private val excludedOwner = ContributionOwner("future-excluded-owner")
    private val missingOwner = ContributionOwner("future-missing-owner")
    private val unclassifiedOwner = ContributionOwner("future-unclassified-owner")
    private val projectionDefinition = featureProjectionDefinition<FutureProjection>(
        FeatureArtifactId("future.reference"),
        includedOwner,
    )

    @Test
    fun `unknown features must include or explicitly exclude an optional projection channel`() {
        val graph = assembleFeatureGraph(
            DiscoveredFeatureGraphContributions(
                contentTypes = listOf(
                    ContentTypeContribution(
                        contentType = ContentTypeId("future"),
                        owner = ContributionOwner("future-type-owner"),
                        providers = emptyList(),
                    ),
                ),
                features = listOf(
                    FeatureContribution(
                        feature = FeatureId("future.included"),
                        owner = includedOwner,
                        integrations = listOf(
                            FeatureIntegration(
                                id = FeatureIntegrationId("future.included.integration"),
                                prerequisites = CapabilityExpression.Always,
                                projectionRequirements = listOf(projectionDefinition),
                                projections = listOf(FeatureProjection(projectionDefinition, FutureProjection)),
                            ),
                        ),
                    ),
                    FeatureContribution(
                        feature = FeatureId("future.excluded"),
                        owner = excludedOwner,
                        integrations = listOf(effectfulIntegration("future.excluded.integration")),
                        projectionExclusions = listOf(
                            featureProjectionExclusion<FutureProjection>("No user-facing type comparison"),
                        ),
                    ),
                    FeatureContribution(
                        feature = FeatureId("future.missing"),
                        owner = missingOwner,
                        integrations = listOf(
                            effectfulIntegration("future.missing.integration"),
                            FeatureIntegration(
                                id = FeatureIntegrationId("future.missing-projection.integration"),
                                prerequisites = CapabilityExpression.Always,
                                projectionRequirements = listOf(
                                    featureProjectionDefinition<FutureProjection>(
                                        FeatureArtifactId("future.missing-projection.reference"),
                                        missingOwner,
                                    ),
                                ),
                            ),
                        ),
                    ),
                    FeatureContribution(
                        feature = FeatureId("future.unclassified"),
                        owner = unclassifiedOwner,
                        integrations = listOf(effectfulIntegration("future.unclassified.integration")),
                    ),
                ),
            ),
        )

        val result = classifyFeatureProjectionParticipation(graph, FutureProjection::class)

        result.participation.map { it.feature.value } shouldContainExactly listOf(
            "future.excluded",
            "future.included",
            "future.missing",
        )
        result.missing shouldContainExactly listOf(
            MissingFeatureProjectionParticipation(FeatureId("future.unclassified"), unclassifiedOwner),
        )
        result.missingImplementations.map { it.feature.value to it.integration.value } shouldContainExactly listOf(
            "future.missing" to "future.missing-projection.integration",
        )
        result.isComplete shouldBe false
    }

    private fun effectfulIntegration(id: String) = FeatureIntegration(
        id = FeatureIntegrationId(id),
        prerequisites = CapabilityExpression.Always,
        behaviorProjections = listOf(object : FeatureBehaviorProjection {
            override val id = FeatureArtifactId("$id.effect")
        }),
    )

    private data object FutureProjection
}
