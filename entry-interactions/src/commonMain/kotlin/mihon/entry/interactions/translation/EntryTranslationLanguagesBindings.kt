package mihon.entry.interactions.translation

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.state.EntryMigrationProvider
import mihon.entry.interactions.state.EntryTranslationLanguagesProvider
import tachiyomi.domain.entry.repository.EntryTranslationLanguagesRepository

@ContributesTo(AppScope::class)
@BindingContainer
object EntryTranslationLanguagesBindings {
    @Provides
    @SingleIn(AppScope::class)
    internal fun defaultFeature(
        translationLanguagesProviders: Map<EntryType, EntryTranslationLanguagesProvider>,
        migrationProviders: Map<EntryType, EntryMigrationProvider>,
        repository: EntryTranslationLanguagesRepository,
    ): DefaultEntryTranslationLanguagesFeature = DefaultEntryTranslationLanguagesFeature(
        applicableTypes = translationLanguagesProviders.keys,
        migrationTypes = translationLanguagesProviders.keys intersect migrationProviders.keys,
        repository = repository,
    )

    @Provides
    internal fun feature(impl: DefaultEntryTranslationLanguagesFeature): EntryTranslationLanguagesFeature = impl
}
