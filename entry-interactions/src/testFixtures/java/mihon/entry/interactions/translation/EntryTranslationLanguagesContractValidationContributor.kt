package mihon.entry.interactions.translation

import kotlinx.coroutines.flow.first
import mihon.entry.interactions.migration.consequence.EntryMigrationDurableEvent
import mihon.entry.interactions.persistence.backup.addEntryBackupParticipationContract
import mihon.entry.interactions.state.EntryMigrationCapability
import mihon.entry.interactions.state.EntryTranslationLanguagesCapability
import mihon.entry.interactions.translation.backup.ENTRY_TRANSLATION_LANGUAGES_BACKUP_RESTORE_PARTICIPANT
import mihon.entry.interactions.translation.backup.ENTRY_TRANSLATION_LANGUAGES_BACKUP_SNAPSHOT_PARTICIPANT
import mihon.entry.interactions.translation.migration.ENTRY_TRANSLATION_LANGUAGES_MIGRATION_PARTICIPANT
import mihon.entry.interactions.translation.migration.EntryTranslationLanguagesMigrationDurableBehaviorContract
import mihon.entry.interactions.translation.migration.entryTranslationLanguagesMigrationBinding
import mihon.entry.interactions.validation.contractExpectation
import mihon.entry.interactions.validation.productionSubjectEvaluation
import mihon.entry.interactions.validation.verifyFeatureContract
import mihon.feature.graph.validation.FeatureContractReference
import mihon.feature.graph.validation.FeatureContractVerifier
import mihon.feature.graph.validation.FeatureExecutionContractReference
import mihon.feature.graph.validation.FeatureExecutionContractVerifier
import mihon.feature.graph.validation.FeatureValidationContributionSink
import mihon.feature.graph.validation.FeatureValidationContributor
import mihon.language.api.tag.LanguageTag
import tachiyomi.domain.entry.model.Entry

class EntryTranslationLanguagesContractValidationContributor : FeatureValidationContributor {
    override val owner = EntryTranslationLanguagesFeatureContributor.owner

    override fun contributeTo(sink: FeatureValidationContributionSink) {
        val example = EntryTranslationLanguagesSnapshot(contentLanguage = "ja", targetLanguage = "es", updatedAt = 1L)
        sink.addEntryBackupParticipationContract(
            ENTRY_TRANSLATION_LANGUAGES_BACKUP_SNAPSHOT_PARTICIPANT,
            EntryTranslationLanguagesBehaviorContract,
            EntryTranslationLanguagesSnapshot.serializer(),
            example,
        )
        sink.addEntryBackupParticipationContract(
            ENTRY_TRANSLATION_LANGUAGES_BACKUP_RESTORE_PARTICIPANT,
            EntryTranslationLanguagesBehaviorContract,
            EntryTranslationLanguagesSnapshot.serializer(),
            example,
        )
        sink.add(
            FeatureExecutionContractVerifier(
                FeatureExecutionContractReference(
                    ENTRY_TRANSLATION_LANGUAGES_MIGRATION_PARTICIPANT.id,
                    EntryTranslationLanguagesMigrationDurableBehaviorContract,
                ),
            ) { input ->
                verifyFeatureContract {
                    val provider = input.provider(EntryTranslationLanguagesCapability.definition)
                    val repository = InMemoryEntryTranslationLanguagesRepository()
                    val feature = DefaultEntryTranslationLanguagesFeature(
                        evaluation = productionSubjectEvaluation(
                            listOf(
                                EntryTranslationLanguagesCapability.bind(provider),
                                EntryMigrationCapability.bind(input.provider(EntryMigrationCapability.definition)),
                            ),
                            EntryTranslationLanguagesFeatureContributor,
                        ),
                        repository = repository,
                    )
                    val source = Entry.create().copy(id = 61L, type = provider.type)
                    val target = source.copy(id = 62L)
                    feature.setTargetLanguage(source, LanguageTag.require("es"))
                    feature.setTranslateDownloads(source, enabled = true)
                    val binding = entryTranslationLanguagesMigrationBinding { feature }
                    val prepared = binding.preparer.prepare(
                        EntryMigrationDurableEvent("contract", source, target, emptySet(), emptyList(), emptyList()),
                    )
                    contractExpectation(
                        prepared != null,
                        "Translation languages must prepare a durable Migration payload",
                    )
                    binding.deliveryHandler.deliver(requireNotNull(prepared))
                    val carried = feature.observe(target).first()
                    contractExpectation(
                        carried.targetLanguage == LanguageTag.require("es") && carried.translateDownloads,
                        "Translation-language Migration must carry the target language and translated downloads",
                    )
                }
            },
        )
        sink.add(
            FeatureContractVerifier(
                FeatureContractReference(
                    ENTRY_TRANSLATION_LANGUAGES_FEATURE_ID,
                    EntryTranslationLanguagesBehaviorContract,
                ),
            ) { input ->
                verifyFeatureContract {
                    val provider = input.provider(EntryTranslationLanguagesCapability.definition)
                    val feature = DefaultEntryTranslationLanguagesFeature(
                        evaluation = productionSubjectEvaluation(
                            EntryTranslationLanguagesCapability.bind(provider),
                            EntryTranslationLanguagesFeatureContributor,
                        ),
                        repository = InMemoryEntryTranslationLanguagesRepository(),
                    )
                    val entry = Entry.create().copy(id = 61L, type = provider.type)
                    val restored = entry.copy(id = 62L)

                    contractExpectation(
                        feature.isApplicable(provider.type),
                        "Translation languages must be applicable",
                    )
                    feature.setContentLanguage(entry, LanguageTag.require("ja"))
                    feature.setTargetLanguage(entry, LanguageTag.require("es"))
                    feature.setTranslateDownloads(entry, enabled = true)
                    val snapshot = feature.snapshot(entry)
                    contractExpectation(
                        snapshot is EntryTranslationLanguagesSnapshotResult.Captured,
                        "Translation languages must capture a backup snapshot",
                    )
                    feature.restore(
                        restored,
                        (snapshot as EntryTranslationLanguagesSnapshotResult.Captured).snapshot,
                    )
                    contractExpectation(
                        feature.observe(restored).first() == feature.observe(entry).first(),
                        "Translation languages must restore what they captured",
                    )
                }
            },
        )
    }
}
