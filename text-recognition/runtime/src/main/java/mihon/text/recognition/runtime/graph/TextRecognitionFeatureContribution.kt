package mihon.text.recognition.runtime.graph

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
import mihon.text.recognition.api.TextRecognitionFeature

internal val TEXT_RECOGNITION_FEATURE_ID = FeatureId("text-recognition")
internal val TEXT_RECOGNITION_PIPELINE_INTEGRATION_ID = FeatureIntegrationId("text-recognition.pipeline")
internal val TEXT_RECOGNITION_FEATURE_OWNER = ContributionOwner("text-recognition")

internal object TextRecognitionFeatureCapability {
    val definition: CapabilityDefinition<TextRecognitionFeature> = capabilityDefinition(
        id = CapabilityId("text-recognition.pipeline"),
        owner = TEXT_RECOGNITION_FEATURE_OWNER,
    )

    fun bind(feature: TextRecognitionFeature): CapabilityProvider<TextRecognitionFeature> {
        return CapabilityProvider(definition, feature)
    }
}

private data object TextRecognitionPrepareAndRecognizeBehavior : FeatureBehaviorProjection {
    override val id = FeatureArtifactId("text-recognition.prepare-and-recognize")
}

internal object TextRecognitionFeatureContributor : FeatureGraphContributor {
    override val owner = TEXT_RECOGNITION_FEATURE_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(
            FeatureContribution(
                feature = TEXT_RECOGNITION_FEATURE_ID,
                owner = owner,
                integrations = listOf(
                    FeatureIntegration(
                        id = TEXT_RECOGNITION_PIPELINE_INTEGRATION_ID,
                        prerequisites = CapabilityExpression.Provided(TextRecognitionFeatureCapability.definition),
                        subjectScope = FeatureSubjectScope.Application,
                        behaviorProjections = listOf(TextRecognitionPrepareAndRecognizeBehavior),
                    ),
                ),
            ),
        )
    }
}
