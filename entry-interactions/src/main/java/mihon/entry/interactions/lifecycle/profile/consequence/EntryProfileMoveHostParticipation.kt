package mihon.entry.interactions

import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureExecutionParticipantDefinition
import mihon.feature.graph.FeatureExecutionParticipantId
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor

internal object EntryProfileMoveSourceVisibilityBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.profile-move.source-visibility.behavior")
}

internal object EntryProfileMoveCustomCoverBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.custom-cover.profile-move.behavior")
}

internal val ENTRY_PROFILE_MOVE_SOURCE_VISIBILITY_PARTICIPANT = FeatureExecutionParticipantDefinition(
    id = FeatureExecutionParticipantId("entry.profile-move.source-visibility"),
    owner = ENTRY_PROFILE_MOVE_OWNER,
    point = ENTRY_PROFILE_MOVED_EXECUTION_POINT,
    behavioralContracts = listOf(EntryProfileMoveSourceVisibilityBehaviorContract),
)

internal val ENTRY_PROFILE_MOVE_CUSTOM_COVER_PARTICIPANT = FeatureExecutionParticipantDefinition(
    id = FeatureExecutionParticipantId("entry.custom-cover.profile-move"),
    owner = ENTRY_LIBRARY_CUSTOM_COVER_OWNER,
    point = ENTRY_PROFILE_MOVED_EXECUTION_POINT,
    behavioralContracts = listOf(EntryProfileMoveCustomCoverBehaviorContract),
)

internal object EntryProfileMoveHostContributor : FeatureGraphContributor {
    override val owner = ENTRY_PROFILE_MOVE_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(ENTRY_PROFILE_MOVE_SOURCE_VISIBILITY_PARTICIPANT)
    }
}

internal object EntryProfileMoveCustomCoverContributor : FeatureGraphContributor {
    override val owner = ENTRY_LIBRARY_CUSTOM_COVER_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(ENTRY_PROFILE_MOVE_CUSTOM_COVER_PARTICIPANT)
    }
}
