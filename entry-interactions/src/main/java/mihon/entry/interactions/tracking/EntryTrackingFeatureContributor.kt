package mihon.entry.interactions

import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.ContextInputId
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureContextBlocker
import mihon.feature.graph.FeatureContextDecision
import mihon.feature.graph.FeatureContribution
import mihon.feature.graph.FeatureExecutionOrder
import mihon.feature.graph.FeatureExecutionParticipantDefinition
import mihon.feature.graph.FeatureExecutionParticipantId
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.allOf
import mihon.feature.graph.contextInputDefinition
import mihon.feature.graph.featureContextRule

internal object EntryTrackingLibraryAdditionBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.tracking.library-addition.behavior")
}

internal val ENTRY_TRACKING_LIBRARY_ADDITION_PARTICIPANT = FeatureExecutionParticipantDefinition(
    id = FeatureExecutionParticipantId("entry.tracking.library-addition"),
    owner = ENTRY_TRACKING_OWNER,
    point = ENTRY_LIBRARY_ADDED_EXECUTION_POINT,
    behavioralContracts = listOf(EntryTrackingLibraryAdditionBehaviorContract),
)

internal val ENTRY_MEDIA_SESSION_TRACKING_ALLOWED = contextInputDefinition<Boolean>(
    ContextInputId("entry.media-session.tracking-allowed"),
    ENTRY_MEDIA_SESSION_INCOGNITO_OWNER,
)
private val ENTRY_MEDIA_SESSION_TRACKING_BLOCKED = FeatureContextBlocker(
    FeatureArtifactId("entry.media-session.tracking-blocked"),
    listOf(ENTRY_MEDIA_SESSION_TRACKING_ALLOWED),
)

internal object EntryTrackingMediaSessionBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.tracking.media-session.behavior")
}

internal val ENTRY_TRACKING_MEDIA_SESSION_PARTICIPANT = FeatureExecutionParticipantDefinition(
    id = FeatureExecutionParticipantId("entry.tracking.media-session"),
    owner = ENTRY_TRACKING_OWNER,
    point = ENTRY_MEDIA_SESSION_CONSEQUENCE_EXECUTION_POINT,
    prerequisites = allOf(
        CapabilityExpression.Provided(EntryMediaSessionCapability.definition),
        CapabilityExpression.Provided(EntryProgressCapability.definition),
    ),
    contextInputs = listOf(ENTRY_MEDIA_SESSION_TRACKING_ALLOWED),
    contextRule = featureContextRule(ENTRY_TRACKING_OWNER) { evidence ->
        if (evidence.value(ENTRY_MEDIA_SESSION_TRACKING_ALLOWED)) {
            FeatureContextDecision.Applicable
        } else {
            FeatureContextDecision.Blocked(listOf(ENTRY_MEDIA_SESSION_TRACKING_BLOCKED))
        }
    },
    contextBlockers = listOf(ENTRY_MEDIA_SESSION_TRACKING_BLOCKED),
    order = FeatureExecutionOrder(after = setOf(ENTRY_PROGRESS_MEDIA_SESSION_PARTICIPANT.id)),
    behavioralContracts = listOf(EntryTrackingMediaSessionBehaviorContract),
)

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

internal object EntryTrackingMediaSessionContributor : FeatureGraphContributor {
    override val owner = ENTRY_TRACKING_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(ENTRY_TRACKING_MEDIA_SESSION_PARTICIPANT)
    }
}

internal object EntryTrackingLibraryMembershipContributor : FeatureGraphContributor {
    override val owner = ENTRY_TRACKING_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(ENTRY_TRACKING_LIBRARY_ADDITION_PARTICIPANT)
    }
}
