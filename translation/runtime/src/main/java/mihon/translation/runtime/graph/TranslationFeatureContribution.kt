package mihon.translation.runtime.graph

import mihon.feature.graph.CapabilityDefinition
import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.CapabilityId
import mihon.feature.graph.CapabilityProvider
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureBehaviorProjection
import mihon.feature.graph.FeatureContribution
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.FeatureId
import mihon.feature.graph.FeatureIntegration
import mihon.feature.graph.FeatureIntegrationId
import mihon.feature.graph.FeatureSubjectScope
import mihon.feature.graph.capabilityDefinition
import mihon.translation.spi.engine.TranslationEngineRegistry

internal val TRANSLATION_FEATURE_ID = FeatureId("translation")
internal val TRANSLATION_ENGINE_REGISTRY_INTEGRATION_ID = FeatureIntegrationId("translation.engine-registry")

internal val TRANSLATION_FEATURE_OWNER = ContributionOwner("translation")

internal object TranslationEngineRegistryCapability {
    val definition: CapabilityDefinition<TranslationEngineRegistry> = capabilityDefinition(
        id = CapabilityId("translation.engine-registry"),
        owner = TRANSLATION_FEATURE_OWNER,
    )

    fun bind(registry: TranslationEngineRegistry): CapabilityProvider<TranslationEngineRegistry> {
        return CapabilityProvider(definition, registry)
    }
}

internal object TranslationFeatureBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("translation.behavior")
}

private data object TranslationBehavior : FeatureBehaviorProjection {
    override val id = FeatureArtifactId("translation.prepare-and-execute")
}

internal object TranslationFeatureContributor : FeatureGraphContributor {
    override val owner = TRANSLATION_FEATURE_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(
            FeatureContribution(
                feature = TRANSLATION_FEATURE_ID,
                owner = owner,
                integrations = listOf(
                    FeatureIntegration(
                        id = TRANSLATION_ENGINE_REGISTRY_INTEGRATION_ID,
                        prerequisites = CapabilityExpression.Provided(TranslationEngineRegistryCapability.definition),
                        subjectScope = FeatureSubjectScope.Application,
                        behaviorProjections = listOf(TranslationBehavior),
                        behavioralContracts = listOf(TranslationFeatureBehaviorContract),
                    ),
                ),
            ),
        )
    }
}
