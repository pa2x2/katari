package mihon.model.artifacts.runtime.graph

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
import mihon.model.artifacts.api.ModelArtifactStore

internal val MODEL_ARTIFACTS_FEATURE_ID = FeatureId("model-artifacts")
internal val MODEL_ARTIFACTS_STORE_INTEGRATION_ID = FeatureIntegrationId("model-artifacts.store")
internal val MODEL_ARTIFACTS_FEATURE_OWNER = ContributionOwner("model-artifacts")

internal object ModelArtifactStoreCapability {
    val definition: CapabilityDefinition<ModelArtifactStore> = capabilityDefinition(
        id = CapabilityId("model-artifacts.store"),
        owner = MODEL_ARTIFACTS_FEATURE_OWNER,
    )

    fun bind(store: ModelArtifactStore): CapabilityProvider<ModelArtifactStore> {
        return CapabilityProvider(definition, store)
    }
}

private data object ModelArtifactApprovedDownloadBehavior : FeatureBehaviorProjection {
    override val id = FeatureArtifactId("model-artifacts.approve-download-verify")
}

internal object ModelArtifactsFeatureContributor : FeatureGraphContributor {
    override val owner = MODEL_ARTIFACTS_FEATURE_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(
            FeatureContribution(
                feature = MODEL_ARTIFACTS_FEATURE_ID,
                owner = owner,
                integrations = listOf(
                    FeatureIntegration(
                        id = MODEL_ARTIFACTS_STORE_INTEGRATION_ID,
                        prerequisites = CapabilityExpression.Provided(ModelArtifactStoreCapability.definition),
                        subjectScope = FeatureSubjectScope.Application,
                        behaviorProjections = listOf(ModelArtifactApprovedDownloadBehavior),
                    ),
                ),
            ),
        )
    }
}
