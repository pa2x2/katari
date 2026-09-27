package mihon.entry.interactions.translation

import mihon.entry.interactions.runtime.production.EntryFeatureRuntimeArtifacts
import mihon.entry.interactions.runtime.production.EntryFeatureRuntimeModule
import mihon.entry.interactions.runtime.production.entryFeatureRuntimeBoundary
import mihon.entry.interactions.translation.backup.EntryTranslationLanguagesBackupContributor
import mihon.entry.interactions.translation.backup.entryTranslationLanguagesBackupRestoreBinding
import mihon.entry.interactions.translation.backup.entryTranslationLanguagesBackupSnapshotBinding
import mihon.entry.interactions.translation.migration.EntryTranslationLanguagesMigrationContributor
import mihon.entry.interactions.translation.migration.entryTranslationLanguagesMigrationBinding
import mihon.feature.runtime.FeatureRuntimeComposition
import tachiyomi.domain.entry.repository.EntryTranslationLanguagesRepository
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

internal val EntryTranslationLanguagesFeatureRuntimeModule = EntryFeatureRuntimeModule(
    id = "entry.translation-languages",
    contributor = EntryTranslationLanguagesFeatureContributor,
    additionalContributors = listOf(
        EntryTranslationLanguagesBackupContributor,
        EntryTranslationLanguagesMigrationContributor,
    ),
) {
    addSingletonFactory {
        DefaultEntryTranslationLanguagesFeature(
            evaluation = get<FeatureRuntimeComposition>().evaluation,
            repository = get<EntryTranslationLanguagesRepository>(),
        )
    }
    addSingletonFactory<EntryTranslationLanguagesFeature> { get<DefaultEntryTranslationLanguagesFeature>() }
    EntryFeatureRuntimeArtifacts(
        durableExecutionBindings = listOf(
            entryTranslationLanguagesMigrationBinding { get<DefaultEntryTranslationLanguagesFeature>() },
        ),
        executionBindings = listOf(
            entryTranslationLanguagesBackupSnapshotBinding { get<DefaultEntryTranslationLanguagesFeature>() },
            entryTranslationLanguagesBackupRestoreBinding { get<DefaultEntryTranslationLanguagesFeature>() },
        ),
        runtimeBoundaries = listOf(entryFeatureRuntimeBoundary { get<EntryTranslationLanguagesFeature>() }),
    )
}
