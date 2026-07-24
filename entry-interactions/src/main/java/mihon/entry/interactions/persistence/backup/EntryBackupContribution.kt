package mihon.entry.interactions

import mihon.entry.interactions.documentation.EntryContentTypeReferenceSection
import mihon.entry.interactions.documentation.entryContentTypeReferenceContribution
import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureBehaviorProjection
import mihon.feature.graph.FeatureContribution
import mihon.feature.graph.FeatureExecutionFailurePolicy
import mihon.feature.graph.FeatureExecutionPointId
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.FeatureId
import mihon.feature.graph.FeatureIntegration
import mihon.feature.graph.FeatureIntegrationId
import mihon.feature.graph.inlineFeatureExecutionPointDefinition

internal val ENTRY_BACKUP_OWNER = ContributionOwner("entry-backup")
private val ENTRY_BACKUP_FEATURE_ID = FeatureId("entry.backup")
private val ENTRY_BACKUP_REFERENCE = entryContentTypeReferenceContribution(
    id = "backup",
    owner = ENTRY_BACKUP_OWNER,
    section = EntryContentTypeReferenceSection.ENTRY_INTERACTIONS,
    label = "Preserve Feature-owned Entry state",
    order = 1800,
)

internal object EntryBackupBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.backup.behavior")
}

private object EntryBackupBehavior : FeatureBehaviorProjection {
    override val id = FeatureArtifactId("entry.backup.feature-state")
}

internal val ENTRY_BACKUP_SNAPSHOT_EXECUTION_POINT = inlineFeatureExecutionPointDefinition<EntryBackupSnapshotEvent>(
    id = FeatureExecutionPointId("entry.backup.snapshot"),
    owner = ENTRY_BACKUP_OWNER,
    failurePolicy = FeatureExecutionFailurePolicy.FAIL_FAST,
)

internal val ENTRY_BACKUP_RESTORE_EXECUTION_POINT = inlineFeatureExecutionPointDefinition<EntryBackupRestoreEvent>(
    id = FeatureExecutionPointId("entry.backup.restore"),
    owner = ENTRY_BACKUP_OWNER,
    failurePolicy = FeatureExecutionFailurePolicy.FAIL_FAST,
)

internal val ENTRY_BACKUP_RESTORE_FINALIZING_EXECUTION_POINT =
    inlineFeatureExecutionPointDefinition<EntryBackupRestoreFinalizingEvent>(
        id = FeatureExecutionPointId("entry.backup.restore-finalizing"),
        owner = ENTRY_BACKUP_OWNER,
        failurePolicy = FeatureExecutionFailurePolicy.FAIL_FAST,
    )

internal object EntryBackupFeatureContributor : FeatureGraphContributor {
    override val owner = ENTRY_BACKUP_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(
            FeatureContribution(
                feature = ENTRY_BACKUP_FEATURE_ID,
                owner = owner,
                integrations = listOf(
                    FeatureIntegration(
                        id = FeatureIntegrationId("entry.backup.feature-state"),
                        prerequisites = CapabilityExpression.Always,
                        behaviorProjections = listOf(EntryBackupBehavior),
                        behavioralContracts = listOf(EntryBackupBehaviorContract),
                        projectionRequirements = listOf(ENTRY_BACKUP_REFERENCE.requirement),
                        projections = listOf(ENTRY_BACKUP_REFERENCE.projection),
                    ),
                ),
            ),
        )
        sink.add(ENTRY_BACKUP_SNAPSHOT_EXECUTION_POINT)
        sink.add(ENTRY_BACKUP_RESTORE_EXECUTION_POINT)
        sink.add(ENTRY_BACKUP_RESTORE_FINALIZING_EXECUTION_POINT)
    }
}
