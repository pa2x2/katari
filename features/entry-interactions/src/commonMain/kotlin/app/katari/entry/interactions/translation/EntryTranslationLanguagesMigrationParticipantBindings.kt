package app.katari.entry.interactions.translation

import app.katari.entry.interactions.durable.EntryDurableParticipant
import app.katari.entry.interactions.migration.consequence.EntryMigrationDurableEvent
import app.katari.entry.interactions.translation.migration.EntryTranslationLanguagesMigrationParticipant
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides

/**
 * Per-series translation languages' part in migrations, apart from the feature so a host without migrations can take
 * the feature alone.
 */
@ContributesTo(AppScope::class)
@BindingContainer
object EntryTranslationLanguagesMigrationParticipantBindings {
    @Provides
    @IntoSet
    internal fun migrationDurableParticipant(
        feature: () -> DefaultEntryTranslationLanguagesFeature,
    ): EntryDurableParticipant<EntryMigrationDurableEvent> = EntryTranslationLanguagesMigrationParticipant(feature)
}
