package mihon.tts.runtime.graph

import mihon.feature.graph.CapabilityDefinition
import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.CapabilityId
import mihon.feature.graph.CapabilityProvider
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorProjection
import mihon.feature.graph.FeatureContribution
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.FeatureId
import mihon.feature.graph.FeatureIntegration
import mihon.feature.graph.FeatureIntegrationId
import mihon.feature.graph.FeatureSubjectScope
import mihon.feature.graph.capabilityDefinition
import mihon.tts.spi.engine.TtsEngineRegistry

internal val TTS_FEATURE_ID = FeatureId("tts")
internal val TTS_ENGINE_REGISTRY_INTEGRATION_ID = FeatureIntegrationId("tts.engine-registry")
internal val TTS_FEATURE_OWNER = ContributionOwner("tts")

internal object TtsEngineRegistryCapability {
    val definition: CapabilityDefinition<TtsEngineRegistry> = capabilityDefinition(
        id = CapabilityId("tts.engine-registry"),
        owner = TTS_FEATURE_OWNER,
    )

    fun bind(registry: TtsEngineRegistry): CapabilityProvider<TtsEngineRegistry> {
        return CapabilityProvider(definition, registry)
    }
}

private data object TtsShortFormPlaybackBehavior : FeatureBehaviorProjection {
    override val id = FeatureArtifactId("tts.prepare-play-stop")
}

internal object TtsFeatureContributor : FeatureGraphContributor {
    override val owner = TTS_FEATURE_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(
            FeatureContribution(
                feature = TTS_FEATURE_ID,
                owner = owner,
                integrations = listOf(
                    FeatureIntegration(
                        id = TTS_ENGINE_REGISTRY_INTEGRATION_ID,
                        prerequisites = CapabilityExpression.Provided(TtsEngineRegistryCapability.definition),
                        subjectScope = FeatureSubjectScope.Application,
                        behaviorProjections = listOf(TtsShortFormPlaybackBehavior),
                    ),
                ),
            ),
        )
    }
}
