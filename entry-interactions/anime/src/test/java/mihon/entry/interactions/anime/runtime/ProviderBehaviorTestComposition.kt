package mihon.entry.interactions.anime

import mihon.entry.interactions.EntryInteractionPlugin
import mihon.entry.interactions.EntryInteractions
import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorProjection
import mihon.feature.graph.FeatureContribution
import mihon.feature.graph.FeatureId
import mihon.feature.graph.FeatureIntegration
import mihon.feature.graph.FeatureIntegrationId
import mihon.feature.graph.featureGraphContributor

internal fun createEntryInteractions(plugins: List<EntryInteractionPlugin>): EntryInteractions {
    return mihon.entry.interactions.createEntryInteractions(
        plugins = plugins,
        featureContributors = plugins
            .flatMap(EntryInteractionPlugin::providerBindings)
            .distinctBy { it.graphProvider.capability.id }
            .map { binding ->
                val capability = binding.graphProvider.capability
                val suffix = capability.id.value
                val owner = ContributionOwner("test.feature.$suffix")
                featureGraphContributor(owner) {
                    add(
                        FeatureContribution(
                            feature = FeatureId("test.$suffix"),
                            owner = owner,
                            integrations = listOf(
                                FeatureIntegration(
                                    id = FeatureIntegrationId("test.$suffix.integration"),
                                    prerequisites = CapabilityExpression.Provided(capability),
                                    behaviorProjections = listOf(
                                        object : FeatureBehaviorProjection {
                                            override val id = FeatureArtifactId("test.$suffix.behavior")
                                        },
                                    ),
                                ),
                            ),
                        ),
                    )
                }
            } + plugins
            .flatMap(EntryInteractionPlugin::specializedAdapters)
            .distinctBy { it.definition.id }
            .map { adapter ->
                val definition = adapter.definition
                val suffix = definition.id.value
                featureGraphContributor(definition.owner) {
                    add(
                        FeatureContribution(
                            feature = FeatureId("test.$suffix"),
                            owner = definition.owner,
                            integrations = listOf(
                                FeatureIntegration(
                                    id = FeatureIntegrationId("test.$suffix.integration"),
                                    prerequisites = CapabilityExpression.Always,
                                    specializedPrerequisites = listOf(definition),
                                    behaviorProjections = listOf(
                                        object : FeatureBehaviorProjection {
                                            override val id = FeatureArtifactId("test.$suffix.behavior")
                                        },
                                    ),
                                ),
                            ),
                        ),
                    )
                }
            },
    )
}
