package mihon.entry.interactions

import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureExecutionParticipantDefinition
import mihon.feature.graph.FeatureExecutionParticipantId
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor

internal val ENTRY_LIBRARY_CUSTOM_COVER_OWNER = ContributionOwner("entry-library-custom-cover-host")

internal object EntryLibraryCustomCoverBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.library-custom-cover.removal.behavior")
}

internal val ENTRY_LIBRARY_CUSTOM_COVER_REMOVAL_PARTICIPANT = FeatureExecutionParticipantDefinition(
    id = FeatureExecutionParticipantId("entry.library-custom-cover.removal"),
    owner = ENTRY_LIBRARY_CUSTOM_COVER_OWNER,
    point = ENTRY_LIBRARY_REMOVED_EXECUTION_POINT,
    behavioralContracts = listOf(EntryLibraryCustomCoverBehaviorContract),
)

internal object EntryLibraryCustomCoverContributor : FeatureGraphContributor {
    override val owner = ENTRY_LIBRARY_CUSTOM_COVER_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(ENTRY_LIBRARY_CUSTOM_COVER_REMOVAL_PARTICIPANT)
    }
}
