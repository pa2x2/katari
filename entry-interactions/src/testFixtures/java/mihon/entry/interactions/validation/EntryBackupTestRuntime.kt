package mihon.entry.interactions.validation

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.persistence.backup.ENTRY_BACKUP_RESTORE_EXECUTION_POINT
import mihon.entry.interactions.persistence.backup.ENTRY_BACKUP_RESTORE_FINALIZING_EXECUTION_POINT
import mihon.entry.interactions.persistence.backup.ENTRY_BACKUP_SNAPSHOT_EXECUTION_POINT
import mihon.entry.interactions.persistence.backup.EntryBackupCoordinator
import mihon.entry.interactions.persistence.backup.EntryBackupFeature
import mihon.entry.interactions.persistence.backup.EntryBackupFeatureContributor
import mihon.entry.interactions.persistence.backup.EntryBackupRestoreEvent
import mihon.entry.interactions.persistence.backup.EntryBackupRestoreFinalizingEvent
import mihon.entry.interactions.persistence.backup.EntryBackupSnapshotEvent
import mihon.entry.interactions.persistence.backup.EntryFeatureStateEnvelope
import mihon.entry.interactions.runtime.EntryInteractionPlugin
import mihon.entry.interactions.runtime.EntryInteractionProviderBinding
import mihon.entry.interactions.runtime.createEntryInteractionComposition
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureExecutionParticipantId
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.execution.FeatureExecutionHandler
import mihon.feature.graph.execution.FeatureExecutionParticipantBinding
import mihon.feature.graph.execution.FeatureExecutionParticipantDefinition

data class EntryBackupTestRuntime(
    val feature: EntryBackupFeature,
    val restoredPayload: () -> List<Byte>?,
    val finalizedTypes: List<EntryType>,
)

fun entryBackupTestRuntime(stateId: String): EntryBackupTestRuntime {
    val participant = TestBackupParticipant()
    var restoredPayload: List<Byte>? = null
    val finalized = mutableListOf<EntryType>()
    val plugin = object : EntryInteractionPlugin {
        override val type = EntryType.MANGA
        override val owner = ContributionOwner("test-manga")
        override val providerBindings = emptyList<EntryInteractionProviderBinding<*>>()
    }
    val composition = createEntryInteractionComposition(
        plugins = listOf(plugin),
        featureContributors = listOf(EntryBackupFeatureContributor, participant),
        executionBindings = listOf(
            FeatureExecutionParticipantBinding(
                participant.snapshot,
                FeatureExecutionHandler<EntryBackupSnapshotEvent> { event ->
                    event.contributions.add(EntryFeatureStateEnvelope(stateId, 1, byteArrayOf(7)))
                },
            ),
            FeatureExecutionParticipantBinding(
                participant.restore,
                FeatureExecutionHandler<EntryBackupRestoreEvent> { event ->
                    restoredPayload = event.states.state(stateId)?.payload?.toList()
                },
            ),
            FeatureExecutionParticipantBinding(
                participant.finalize,
                FeatureExecutionHandler<EntryBackupRestoreFinalizingEvent> { event -> finalized += event.type },
            ),
        ),
    )
    return EntryBackupTestRuntime(
        feature = EntryBackupCoordinator(composition.featureExecutions),
        restoredPayload = { restoredPayload },
        finalizedTypes = finalized,
    )
}

private class TestBackupParticipant : FeatureGraphContributor {
    override val owner = ContributionOwner("test-backup-participant")

    private object Contract : FeatureBehaviorContract {
        override val id = FeatureArtifactId("test-backup-participant.behavior")
    }

    val snapshot = FeatureExecutionParticipantDefinition(
        id = FeatureExecutionParticipantId("test.backup.snapshot"),
        owner = owner,
        point = ENTRY_BACKUP_SNAPSHOT_EXECUTION_POINT,
        behavioralContracts = listOf(Contract),
    )
    val restore = FeatureExecutionParticipantDefinition(
        id = FeatureExecutionParticipantId("test.backup.restore"),
        owner = owner,
        point = ENTRY_BACKUP_RESTORE_EXECUTION_POINT,
        behavioralContracts = listOf(Contract),
    )
    val finalize = FeatureExecutionParticipantDefinition(
        id = FeatureExecutionParticipantId("test.backup.finalize"),
        owner = owner,
        point = ENTRY_BACKUP_RESTORE_FINALIZING_EXECUTION_POINT,
        behavioralContracts = listOf(Contract),
    )

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(snapshot)
        sink.add(restore)
        sink.add(finalize)
    }
}
