package mihon.entry.interactions

import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureExecutionHandler
import mihon.feature.graph.FeatureExecutionParticipantBinding
import mihon.feature.graph.FeatureExecutionParticipantDefinition
import mihon.feature.graph.FeatureExecutionParticipantId
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor

internal object EntryTrackingMigrationParticipationBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.tracking.migration-participation.behavior")
}

internal val ENTRY_TRACKING_MIGRATION_PARTICIPANT = FeatureExecutionParticipantDefinition(
    id = FeatureExecutionParticipantId("entry.tracking.migration-preparation"),
    owner = ENTRY_TRACKING_OWNER,
    point = ENTRY_MIGRATION_TRANSITION_PREPARING_POINT,
    prerequisites = CapabilityExpression.Provided(EntryMigrationCapability.definition),
    behavioralContracts = listOf(EntryTrackingMigrationParticipationBehaviorContract),
)

internal object EntryTrackingMigrationContributor : FeatureGraphContributor {
    override val owner = ENTRY_TRACKING_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(ENTRY_TRACKING_MIGRATION_PARTICIPANT)
    }
}

internal fun entryTrackingMigrationBinding(
    feature: () -> EntryTrackingFeature,
) = FeatureExecutionParticipantBinding(
    definition = ENTRY_TRACKING_MIGRATION_PARTICIPANT,
    handler = FeatureExecutionHandler { event ->
        when (
            val prepared = feature().prepareMigrationTracks(
                source = event.source,
                target = event.target,
                tracks = event.sourceTracks.map { it.toTrackingRecord() },
            )
        ) {
            is EntryTrackingMigrationPreparationResult.Prepared -> {
                event.outcomes.addTracks(prepared.tracks.map { it.toDomainTrack() })
            }
            is EntryTrackingMigrationPreparationResult.Failed -> throw prepared.cause
        }
    },
)
