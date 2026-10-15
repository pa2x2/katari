package mihon.entry.interactions.tracking

import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureContribution
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor

internal object EntryTrackingLibraryAdditionBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.tracking.library-addition.behavior")
}

internal object EntryTrackingFeatureContributor : FeatureGraphContributor {
    override val owner = ENTRY_TRACKING_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(
            FeatureContribution(
                feature = ENTRY_TRACKING_FEATURE_ID,
                owner = owner,
                integrations = entryTrackingIntegrations(),
            ),
        )
    }
}
