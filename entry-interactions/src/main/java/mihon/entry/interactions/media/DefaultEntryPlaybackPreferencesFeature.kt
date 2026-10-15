package mihon.entry.interactions.media

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.state.EntryPlaybackPreferencesInteraction
import tachiyomi.domain.entry.model.Entry

internal class DefaultEntryPlaybackPreferencesFeature(
    private val applicableTypes: Set<EntryType>,
    private val migrationTypes: Set<EntryType>,
    private val interaction: EntryPlaybackPreferencesInteraction,
) : EntryPlaybackPreferencesFeature {
    override fun isApplicable(type: EntryType): Boolean = type in applicableTypes

    override suspend fun snapshot(entry: Entry): EntryPlaybackPreferencesSnapshotResult {
        if (!isApplicable(entry.type)) return EntryPlaybackPreferencesSnapshotResult.Inapplicable(entry.type)
        return interaction.snapshot(entry)
            ?.let(EntryPlaybackPreferencesSnapshotResult::Captured)
            ?: EntryPlaybackPreferencesSnapshotResult.NoPreferences
    }

    override suspend fun restore(
        entry: Entry,
        snapshot: EntryPlaybackPreferencesSnapshot,
    ): EntryPlaybackPreferencesRestoreResult {
        if (!isApplicable(entry.type)) return EntryPlaybackPreferencesRestoreResult.Inapplicable(entry.type)
        interaction.restore(entry, snapshot)
        return EntryPlaybackPreferencesRestoreResult.Applied
    }

    override suspend fun copy(
        sourceEntry: Entry,
        targetEntry: Entry,
    ): EntryPlaybackPreferencesCopyResult {
        if (sourceEntry.type != targetEntry.type) {
            return EntryPlaybackPreferencesCopyResult.TypeMismatch(sourceEntry.type, targetEntry.type)
        }
        val inapplicableTypes = setOf(sourceEntry.type, targetEntry.type) - migrationTypes
        if (inapplicableTypes.isNotEmpty()) {
            return EntryPlaybackPreferencesCopyResult.Inapplicable(inapplicableTypes)
        }
        return if (interaction.copy(sourceEntry, targetEntry)) {
            EntryPlaybackPreferencesCopyResult.Copied
        } else {
            EntryPlaybackPreferencesCopyResult.NoPreferences
        }
    }

    override suspend fun prepareMigration(
        sourceEntry: Entry,
        targetEntry: Entry,
    ): EntryPlaybackPreferencesMigrationPreparation {
        if (sourceEntry.type != targetEntry.type) {
            return EntryPlaybackPreferencesMigrationPreparation.TypeMismatch(sourceEntry.type, targetEntry.type)
        }
        val inapplicableTypes = setOf(sourceEntry.type, targetEntry.type) - migrationTypes
        if (inapplicableTypes.isNotEmpty()) {
            return EntryPlaybackPreferencesMigrationPreparation.Inapplicable(inapplicableTypes)
        }
        return interaction.snapshot(sourceEntry)
            ?.let {
                EntryPlaybackPreferencesMigrationPreparation.Prepared(
                    EntryPlaybackPreferencesMigrationPayload(targetEntry, it),
                )
            }
            ?: EntryPlaybackPreferencesMigrationPreparation.NoPreferences
    }

    override suspend fun applyMigration(
        payload: EntryPlaybackPreferencesMigrationPayload,
    ): EntryPlaybackPreferencesRestoreResult = restore(payload.target, payload.snapshot)
}
