package app.katari.entry.interactions.state

import app.katari.entry.interactions.durable.EntryDurableParticipant
import app.katari.entry.interactions.migration.consequence.EntryMigrationDurableEvent
import app.katari.entry.interactions.state.migration.EntryProgressMigrationParticipant
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides

/**
 * Reading progress's part in migrations, apart from the state features so a host without migrations can take the
 * features alone.
 */
@ContributesTo(AppScope::class)
@BindingContainer
object EntryStateMigrationParticipantBindings {
    @Provides
    @IntoSet
    internal fun progressMigrationParticipant(
        feature: () -> EntryProgressFeature,
    ): EntryDurableParticipant<EntryMigrationDurableEvent> = EntryProgressMigrationParticipant(feature)
}
