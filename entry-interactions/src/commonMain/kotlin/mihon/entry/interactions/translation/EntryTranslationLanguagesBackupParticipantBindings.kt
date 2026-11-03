package mihon.entry.interactions.translation

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import mihon.entry.interactions.persistence.backup.EntryBackupStateParticipant
import mihon.entry.interactions.translation.backup.EntryTranslationLanguagesBackupParticipant

/**
 * Per-series translation languages' part in backups, apart from the feature so a host without backups can take the
 * feature alone.
 */
@ContributesTo(AppScope::class)
@BindingContainer
object EntryTranslationLanguagesBackupParticipantBindings {
    @Provides
    @IntoSet
    internal fun backupState(
        feature: () -> DefaultEntryTranslationLanguagesFeature,
    ): EntryBackupStateParticipant = EntryTranslationLanguagesBackupParticipant(feature)
}
