package mihon.entry.interactions.translation.backup

import mihon.entry.interactions.persistence.backup.ENTRY_BACKUP_RESTORE_EXECUTION_POINT
import mihon.entry.interactions.persistence.backup.ENTRY_BACKUP_SNAPSHOT_EXECUTION_POINT
import mihon.entry.interactions.persistence.backup.EntryBackupRestoreEvent
import mihon.entry.interactions.persistence.backup.EntryBackupSnapshotEvent
import mihon.entry.interactions.persistence.backup.decodeEntryBackupState
import mihon.entry.interactions.persistence.backup.entryBackupStateEnvelope
import mihon.entry.interactions.state.EntryTranslationLanguagesCapability
import mihon.entry.interactions.translation.DefaultEntryTranslationLanguagesFeature
import mihon.entry.interactions.translation.ENTRY_TRANSLATION_LANGUAGES_FEATURE_OWNER
import mihon.entry.interactions.translation.EntryTranslationLanguagesBehaviorContract
import mihon.entry.interactions.translation.EntryTranslationLanguagesSnapshot
import mihon.entry.interactions.translation.EntryTranslationLanguagesSnapshotResult
import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.FeatureExecutionParticipantId
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.execution.FeatureExecutionHandler
import mihon.feature.graph.execution.FeatureExecutionParticipantBinding
import mihon.feature.graph.execution.FeatureExecutionParticipantDefinition

internal const val ENTRY_TRANSLATION_LANGUAGES_BACKUP_STATE_ID = "entry.translation-languages.backup"
internal const val ENTRY_TRANSLATION_LANGUAGES_BACKUP_SCHEMA_VERSION = 1

internal val ENTRY_TRANSLATION_LANGUAGES_BACKUP_SNAPSHOT_PARTICIPANT = FeatureExecutionParticipantDefinition(
    id = FeatureExecutionParticipantId("entry.translation-languages.backup-snapshot"),
    owner = ENTRY_TRANSLATION_LANGUAGES_FEATURE_OWNER,
    point = ENTRY_BACKUP_SNAPSHOT_EXECUTION_POINT,
    prerequisites = CapabilityExpression.Provided(EntryTranslationLanguagesCapability.definition),
    behavioralContracts = listOf(EntryTranslationLanguagesBehaviorContract),
)

internal val ENTRY_TRANSLATION_LANGUAGES_BACKUP_RESTORE_PARTICIPANT = FeatureExecutionParticipantDefinition(
    id = FeatureExecutionParticipantId("entry.translation-languages.backup-restore"),
    owner = ENTRY_TRANSLATION_LANGUAGES_FEATURE_OWNER,
    point = ENTRY_BACKUP_RESTORE_EXECUTION_POINT,
    prerequisites = CapabilityExpression.Provided(EntryTranslationLanguagesCapability.definition),
    behavioralContracts = listOf(EntryTranslationLanguagesBehaviorContract),
)

internal object EntryTranslationLanguagesBackupContributor : FeatureGraphContributor {
    override val owner = ENTRY_TRANSLATION_LANGUAGES_FEATURE_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(ENTRY_TRANSLATION_LANGUAGES_BACKUP_SNAPSHOT_PARTICIPANT)
        sink.add(ENTRY_TRANSLATION_LANGUAGES_BACKUP_RESTORE_PARTICIPANT)
    }
}

internal fun entryTranslationLanguagesBackupSnapshotBinding(
    feature: () -> DefaultEntryTranslationLanguagesFeature,
): FeatureExecutionParticipantBinding<EntryBackupSnapshotEvent> = FeatureExecutionParticipantBinding(
    definition = ENTRY_TRANSLATION_LANGUAGES_BACKUP_SNAPSHOT_PARTICIPANT,
    handler = FeatureExecutionHandler { event ->
        when (val result = feature().snapshot(event.entry)) {
            is EntryTranslationLanguagesSnapshotResult.Captured -> event.contributions.add(
                entryBackupStateEnvelope(
                    ENTRY_TRANSLATION_LANGUAGES_BACKUP_STATE_ID,
                    ENTRY_TRANSLATION_LANGUAGES_BACKUP_SCHEMA_VERSION,
                    EntryTranslationLanguagesSnapshot.serializer(),
                    result.snapshot,
                ),
            )
            EntryTranslationLanguagesSnapshotResult.NoLanguages,
            is EntryTranslationLanguagesSnapshotResult.Inapplicable,
            -> Unit
        }
    },
)

internal fun entryTranslationLanguagesBackupRestoreBinding(
    feature: () -> DefaultEntryTranslationLanguagesFeature,
): FeatureExecutionParticipantBinding<EntryBackupRestoreEvent> = FeatureExecutionParticipantBinding(
    definition = ENTRY_TRANSLATION_LANGUAGES_BACKUP_RESTORE_PARTICIPANT,
    handler = FeatureExecutionHandler { event ->
        val snapshot = event.states.decodeEntryBackupState(
            ENTRY_TRANSLATION_LANGUAGES_BACKUP_STATE_ID,
            ENTRY_TRANSLATION_LANGUAGES_BACKUP_SCHEMA_VERSION,
            EntryTranslationLanguagesSnapshot.serializer(),
        ) ?: return@FeatureExecutionHandler
        feature().restore(event.entry, snapshot)
    },
)
