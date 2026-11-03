package mihon.entry.interactions.state

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import mihon.entry.interactions.persistence.backup.EntryBackupStateParticipant
import mihon.entry.interactions.state.backup.EntryProgressBackupParticipant

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
