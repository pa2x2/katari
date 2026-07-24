package mihon.entry.interactions

import mihon.entry.interactions.documentation.EntryContentTypeReferenceSection
import mihon.entry.interactions.documentation.entryContentTypeReferenceContribution
import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.ContextInputId
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureBehaviorProjection
import mihon.feature.graph.FeatureContextBlocker
import mihon.feature.graph.FeatureContextDecision
import mihon.feature.graph.FeatureContribution
import mihon.feature.graph.FeatureExecutionParticipantDefinition
import mihon.feature.graph.FeatureExecutionParticipantId
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.FeatureId
import mihon.feature.graph.FeatureIntegration
import mihon.feature.graph.FeatureIntegrationId
import mihon.feature.graph.contextInputDefinition
import mihon.feature.graph.featureContextRule

internal val ENTRY_HISTORY_OWNER = ContributionOwner("entry-history")
internal val ENTRY_HISTORY_FEATURE_ID = FeatureId("entry.history")
internal val ENTRY_HISTORY_INTEGRATION_ID = FeatureIntegrationId("entry.history.media-session")
private val ENTRY_HISTORY_REFERENCE = entryContentTypeReferenceContribution(
    id = "history",
    owner = ENTRY_HISTORY_OWNER,
    section = EntryContentTypeReferenceSection.ENTRY_INTERACTIONS,
    label = "Record reading and playback history",
    order = 355,
)

internal val ENTRY_MEDIA_SESSION_HISTORY_ALLOWED = contextInputDefinition<Boolean>(
    ContextInputId("entry.media-session.history-allowed"),
    ENTRY_MEDIA_SESSION_INCOGNITO_OWNER,
)
private val ENTRY_MEDIA_SESSION_HISTORY_BLOCKED = FeatureContextBlocker(
    FeatureArtifactId("entry.media-session.history-blocked"),
    listOf(ENTRY_MEDIA_SESSION_HISTORY_ALLOWED),
)

internal object EntryHistoryBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.history.media-session.behavior")
}

private object EntryHistoryBehavior : FeatureBehaviorProjection {
    override val id = FeatureArtifactId("entry.history.media-session")
}

internal val ENTRY_HISTORY_MEDIA_SESSION_PARTICIPANT = FeatureExecutionParticipantDefinition(
    id = FeatureExecutionParticipantId("entry.history.media-session"),
    owner = ENTRY_HISTORY_OWNER,
    point = ENTRY_MEDIA_SESSION_CONSEQUENCE_EXECUTION_POINT,
    prerequisites = CapabilityExpression.Provided(EntryMediaSessionCapability.definition),
    contextInputs = listOf(ENTRY_MEDIA_SESSION_HISTORY_ALLOWED),
    contextRule = featureContextRule(ENTRY_HISTORY_OWNER) { evidence ->
        if (evidence.value(ENTRY_MEDIA_SESSION_HISTORY_ALLOWED)) {
            FeatureContextDecision.Applicable
        } else {
            FeatureContextDecision.Blocked(listOf(ENTRY_MEDIA_SESSION_HISTORY_BLOCKED))
        }
    },
    contextBlockers = listOf(ENTRY_MEDIA_SESSION_HISTORY_BLOCKED),
    behavioralContracts = listOf(EntryHistoryBehaviorContract),
)

internal object EntryHistoryFeatureContributor : FeatureGraphContributor {
    override val owner = ENTRY_HISTORY_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(
            FeatureContribution(
                feature = ENTRY_HISTORY_FEATURE_ID,
                owner = owner,
                integrations = listOf(
                    FeatureIntegration(
                        id = ENTRY_HISTORY_INTEGRATION_ID,
                        prerequisites = CapabilityExpression.Provided(EntryMediaSessionCapability.definition),
                        behaviorProjections = listOf(EntryHistoryBehavior),
                        behavioralContracts = listOf(EntryHistoryBehaviorContract),
                        projectionRequirements = listOf(ENTRY_HISTORY_REFERENCE.requirement),
                        projections = listOf(ENTRY_HISTORY_REFERENCE.projection),
                    ),
                ),
            ),
        )
        sink.add(ENTRY_HISTORY_MEDIA_SESSION_PARTICIPANT)
    }
}
