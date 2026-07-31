package mihon.entry.interactions.tracking.migration

import mihon.entry.interactions.migration.preparation.ENTRY_MIGRATION_TRANSITION_PREPARING_POINT
import mihon.entry.interactions.state.EntryMigrationCapability
import mihon.entry.interactions.tracking.ENTRY_TRACKING_OWNER
import mihon.entry.interactions.tracking.EntryTrackingFeature
import mihon.entry.interactions.tracking.EntryTrackingMigrationPreparationResult
import mihon.entry.interactions.tracking.toDomainTrack
import mihon.entry.interactions.tracking.toTrackingRecord
import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureExecutionParticipantId
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.execution.FeatureExecutionHandler
import mihon.feature.graph.execution.FeatureExecutionParticipantBinding
import mihon.feature.graph.execution.FeatureExecutionParticipantDefinition

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
