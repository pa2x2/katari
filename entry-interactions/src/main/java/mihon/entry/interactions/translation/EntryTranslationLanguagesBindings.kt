package mihon.entry.interactions.translation

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import mihon.entry.interactions.persistence.backup.EntryBackupStateParticipant
import mihon.entry.interactions.translation.backup.EntryTranslationLanguagesBackupParticipant
import mihon.entry.interactions.translation.migration.EntryTranslationLanguagesMigrationContributor
import mihon.entry.interactions.translation.migration.entryTranslationLanguagesMigrationBinding
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.execution.FeatureDurableExecutionParticipantBinding
import mihon.feature.runtime.FeatureRuntimeComposition
import tachiyomi.domain.entry.repository.EntryTranslationLanguagesRepository

@ContributesTo(AppScope::class)
@BindingContainer
object EntryTranslationLanguagesBindings {
    @Provides
    @SingleIn(AppScope::class)
    internal fun defaultFeature(
        composition: FeatureRuntimeComposition,
        repository: EntryTranslationLanguagesRepository,
    ): DefaultEntryTranslationLanguagesFeature = DefaultEntryTranslationLanguagesFeature(
        evaluation = composition.evaluation,
        repository = repository,
    )

    @Provides
    internal fun feature(impl: DefaultEntryTranslationLanguagesFeature): EntryTranslationLanguagesFeature = impl

    @Provides
    @IntoSet
    fun graphContributor(): FeatureGraphContributor = EntryTranslationLanguagesFeatureContributor

    @Provides
    @IntoSet
    fun migrationGraphContributor(): FeatureGraphContributor = EntryTranslationLanguagesMigrationContributor

    @Provides
    @IntoSet
    internal fun migrationDurableParticipant(
        feature: () -> DefaultEntryTranslationLanguagesFeature,
    ): FeatureDurableExecutionParticipantBinding<*> = entryTranslationLanguagesMigrationBinding(feature)

    @Provides
    @IntoSet
    internal fun backupState(
        feature: () -> DefaultEntryTranslationLanguagesFeature,
    ): EntryBackupStateParticipant = EntryTranslationLanguagesBackupParticipant(feature)
}
