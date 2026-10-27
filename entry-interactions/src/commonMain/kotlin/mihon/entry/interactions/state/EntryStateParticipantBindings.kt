package mihon.entry.interactions.state

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import mihon.entry.interactions.durable.EntryDurableParticipant
import mihon.entry.interactions.migration.consequence.EntryMigrationDurableEvent
import mihon.entry.interactions.persistence.backup.EntryBackupStateParticipant
import mihon.entry.interactions.state.backup.EntryProgressBackupParticipant
import mihon.entry.interactions.state.migration.EntryProgressMigrationParticipant

/**
 * Reading progress's part in migrations and backups, apart from the state features so a host without them can take
 * the features alone.
 */
@ContributesTo(AppScope::class)
@BindingContainer
object EntryStateParticipantBindings {
    @Provides
    @IntoSet
    internal fun progressMigrationParticipant(
        feature: () -> EntryProgressFeature,
    ): EntryDurableParticipant<EntryMigrationDurableEvent> = EntryProgressMigrationParticipant(feature)

    @Provides
    @IntoSet
    internal fun progressBackupState(
        feature: () -> EntryProgressFeature,
    ): EntryBackupStateParticipant = EntryProgressBackupParticipant(feature)
}
