package app.katari.entry.interactions.translation

import app.katari.entry.interactions.persistence.backup.EntryBackupStateParticipant
import app.katari.entry.interactions.translation.backup.EntryTranslationLanguagesBackupParticipant
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides

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
