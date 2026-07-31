package mihon.entry.interactions.media.session

import mihon.entry.interactions.media.EntryMediaSessionCapability
import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureBehaviorProjection
import mihon.feature.graph.FeatureContribution
import mihon.feature.graph.FeatureExecutionParticipantId
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.FeatureId
import mihon.feature.graph.FeatureIntegration
import mihon.feature.graph.FeatureIntegrationId
import mihon.feature.graph.execution.FeatureExecutionParticipantDefinition

internal val ENTRY_MEDIA_SESSION_INCOGNITO_OWNER = ContributionOwner("entry-media-session-incognito")
internal val ENTRY_MEDIA_SESSION_INCOGNITO_FEATURE_ID = FeatureId("entry.media-session.incognito")
internal val ENTRY_MEDIA_SESSION_INCOGNITO_INTEGRATION_ID =
    FeatureIntegrationId("entry.media-session.incognito.policy")

internal object EntryMediaSessionIncognitoBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.media-session.incognito.behavior")
}

private object EntryMediaSessionIncognitoBehavior : FeatureBehaviorProjection {
    override val id = FeatureArtifactId("entry.media-session.incognito.policy")
}

internal val ENTRY_MEDIA_SESSION_INCOGNITO_PARTICIPANT = FeatureExecutionParticipantDefinition(
    id = FeatureExecutionParticipantId("entry.media-session.incognito"),
    owner = ENTRY_MEDIA_SESSION_INCOGNITO_OWNER,
    point = ENTRY_MEDIA_SESSION_POLICY_EXECUTION_POINT,
    prerequisites = CapabilityExpression.Provided(EntryMediaSessionCapability.definition),
    behavioralContracts = listOf(EntryMediaSessionIncognitoBehaviorContract),
)

internal object EntryMediaSessionIncognitoContributor : FeatureGraphContributor {
    override val owner = ENTRY_MEDIA_SESSION_INCOGNITO_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(
            FeatureContribution(
                feature = ENTRY_MEDIA_SESSION_INCOGNITO_FEATURE_ID,
                owner = owner,
                integrations = listOf(
                    FeatureIntegration(
                        id = ENTRY_MEDIA_SESSION_INCOGNITO_INTEGRATION_ID,
                        prerequisites = CapabilityExpression.Provided(EntryMediaSessionCapability.definition),
                        behaviorProjections = listOf(EntryMediaSessionIncognitoBehavior),
                        behavioralContracts = listOf(EntryMediaSessionIncognitoBehaviorContract),
                    ),
                ),
            ),
        )
        sink.add(ENTRY_MEDIA_SESSION_INCOGNITO_PARTICIPANT)
    }
}

internal val ENTRY_MEDIA_SESSION_RECORDING_CONSEQUENCES = setOf(
    EntryMediaSessionConsequence.RECORD_PROGRESS,
    EntryMediaSessionConsequence.RECORD_HISTORY,
    EntryMediaSessionConsequence.SYNCHRONIZE_TRACKING,
)
