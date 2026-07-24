package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.validation.contractExpectation
import mihon.entry.interactions.validation.verifyFeatureContract
import mihon.feature.graph.FeatureId
import mihon.feature.graph.validation.FeatureContractReference
import mihon.feature.graph.validation.FeatureContractVerifier
import mihon.feature.graph.validation.FeatureValidationContributionSink
import mihon.feature.graph.validation.FeatureValidationContributor
import tachiyomi.domain.entry.model.Entry

class EntryBackupContractValidationContributor : FeatureValidationContributor {
    override val owner = EntryBackupFeatureContributor.owner

    override fun contributeTo(sink: FeatureValidationContributionSink) {
        sink.add(
            FeatureContractVerifier(
                FeatureContractReference(FeatureId("entry.backup"), EntryBackupBehaviorContract),
            ) { input ->
                verifyFeatureContract {
                    val type = EntryType.entries.single { it.toContentTypeId() == input.subject.contentType }
                    val composition = lifecycleContractComposition(
                        type,
                        EntryBackupFeatureContributor,
                        listOf(
                            ENTRY_BACKUP_SNAPSHOT_EXECUTION_POINT,
                            ENTRY_BACKUP_RESTORE_EXECUTION_POINT,
                            ENTRY_BACKUP_RESTORE_FINALIZING_EXECUTION_POINT,
                        ),
                    )
                    val feature = EntryBackupCoordinator(composition.featureExecutions)
                    val entry = Entry.create().copy(id = 1, profileId = 1, source = 2, url = "/entry", type = type)
                    val session = EntryBackupRestoreSession(EntryBackupRestoreSessionId("contract"))
                    contractExpectation(
                        feature.snapshot(1, entry, EntryBackupSelection(true, true)).isEmpty(),
                        "Backup coordinator must accept an Entry without Feature-owned state",
                    )
                    feature.restore(
                        session,
                        1,
                        entry,
                        listOf(EntryFeatureStateEnvelope("future.feature", 1, byteArrayOf(1))),
                    )
                    contractExpectation(
                        feature.finalizeRestore(session, 1, setOf(type)).issues.isEmpty(),
                        "Unknown Feature state must not invalidate an otherwise usable backup",
                    )
                }
            },
        )
    }
}
