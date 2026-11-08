package app.katari.entry.interactions.state

import app.katari.entry.interactions.persistence.backup.EntryBackupStateParticipant
import app.katari.entry.interactions.state.backup.EntryProgressBackupParticipant
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides

/**
 * Reading progress's part in backups, apart from the state features so a host without backups can take the features
 * alone.
 */
@ContributesTo(AppScope::class)
@BindingContainer
object EntryStateBackupParticipantBindings {
    @Provides
    @IntoSet
    internal fun progressBackupState(
        feature: () -> EntryProgressFeature,
    ): EntryBackupStateParticipant = EntryProgressBackupParticipant(feature)
}
