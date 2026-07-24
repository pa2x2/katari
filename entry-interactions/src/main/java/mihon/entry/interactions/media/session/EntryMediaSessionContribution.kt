package mihon.entry.interactions

import mihon.entry.interactions.documentation.EntryContentTypeReferenceSection
import mihon.entry.interactions.documentation.entryContentTypeReferenceContribution
import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureBehaviorProjection
import mihon.feature.graph.FeatureContribution
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.FeatureId
import mihon.feature.graph.FeatureIntegration
import mihon.feature.graph.FeatureIntegrationId

internal val ENTRY_MEDIA_SESSION_FEATURE_ID = FeatureId("entry.media-session")
internal val ENTRY_MEDIA_SESSION_INTEGRATION_ID = FeatureIntegrationId("entry.media-session.provider")
private val ENTRY_MEDIA_SESSION_REFERENCE = entryContentTypeReferenceContribution(
    id = "media-session",
    owner = ENTRY_MEDIA_SESSION_OWNER,
    section = EntryContentTypeReferenceSection.ENTRY_INTERACTIONS,
    label = "Report viewing progress and activity",
    order = 340,
)

internal object EntryMediaSessionBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.media-session.behavior")
}

private enum class EntryMediaSessionBehavior(
    override val id: FeatureArtifactId,
) : FeatureBehaviorProjection {
    EVENT_EMISSION(FeatureArtifactId("entry.media-session.event-emission")),
    POLICY_EVALUATION(FeatureArtifactId("entry.media-session.policy-evaluation")),
    CONSEQUENCE_EXECUTION(FeatureArtifactId("entry.media-session.consequence-execution")),
}

internal object EntryMediaSessionFeatureContributor : FeatureGraphContributor {
    override val owner = ENTRY_MEDIA_SESSION_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(
            FeatureContribution(
                feature = ENTRY_MEDIA_SESSION_FEATURE_ID,
                owner = owner,
                integrations = listOf(
                    FeatureIntegration(
                        id = ENTRY_MEDIA_SESSION_INTEGRATION_ID,
                        prerequisites = CapabilityExpression.Provided(EntryMediaSessionCapability.definition),
                        behaviorProjections = EntryMediaSessionBehavior.entries,
                        behavioralContracts = listOf(EntryMediaSessionBehaviorContract),
                        projectionRequirements = listOf(ENTRY_MEDIA_SESSION_REFERENCE.requirement),
                        projections = listOf(ENTRY_MEDIA_SESSION_REFERENCE.projection),
                    ),
                ),
            ),
        )
        sink.add(ENTRY_MEDIA_SESSION_POLICY_EXECUTION_POINT)
        sink.add(ENTRY_MEDIA_SESSION_CONSEQUENCE_EXECUTION_POINT)
    }
}
