package mihon.entry.interactions.translate

import mihon.entry.interactions.download.EntryDownloadCapability
import mihon.feature.graph.CapabilityExpression
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
import mihon.feature.graph.allOf

internal val ENTRY_TRANSLATE_FEATURE_ID = FeatureId("entry.translate")
private val FEATURE_OWNER = ContributionOwner("entry-translate")
internal val ENTRY_TRANSLATE_INTEGRATION_ID = FeatureIntegrationId("entry.translate.provider")

/** Translation works on downloaded chapters, so a type must provide both. */
internal object EntryTranslateBehavior : FeatureBehaviorProjection {
    override val id = FeatureArtifactId("entry.translate.downloaded-chapters")
}

internal object EntryTranslateBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.translate.behavior")
}

internal object EntryTranslateFeatureContributor : FeatureGraphContributor {
    override val owner = FEATURE_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(
            FeatureContribution(
                feature = ENTRY_TRANSLATE_FEATURE_ID,
                owner = owner,
                integrations = listOf(
                    FeatureIntegration(
                        id = ENTRY_TRANSLATE_INTEGRATION_ID,
                        prerequisites = allOf(
                            CapabilityExpression.Provided(EntryTranslateCapability.definition),
                            CapabilityExpression.Provided(EntryDownloadCapability.definition),
                        ),
                        behaviorProjections = listOf(EntryTranslateBehavior),
                        behavioralContracts = listOf(EntryTranslateBehaviorContract),
                    ),
                ),
            ),
        )
    }
}
