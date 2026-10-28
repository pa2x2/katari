package mihon.entry.interactions.translation

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import mihon.entry.interactions.durable.EntryDurableParticipant
import mihon.entry.interactions.migration.consequence.EntryMigrationDurableEvent
import mihon.entry.interactions.translation.migration.EntryTranslationLanguagesMigrationParticipant

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
