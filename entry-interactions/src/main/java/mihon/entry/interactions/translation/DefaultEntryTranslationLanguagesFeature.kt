package mihon.entry.interactions.translation

import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import mihon.entry.interactions.runtime.applicableProviderTypes
import mihon.entry.interactions.state.EntryTranslationLanguagesProvider
import mihon.feature.graph.FeatureGraphEvaluation
import mihon.language.api.tag.LanguageTag
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryTranslationLanguages
import tachiyomi.domain.entry.repository.EntryTranslationLanguagesRepository

internal class DefaultEntryTranslationLanguagesFeature(
    evaluation: FeatureGraphEvaluation,
    private val repository: EntryTranslationLanguagesRepository,
    private val clock: () -> Long = System::currentTimeMillis,
) : EntryTranslationLanguagesFeature {
    private val applicableTypes = EntryTranslationLanguagesBehavior.entries
        .map { behavior ->
            evaluation.applicableProviderTypes<EntryTranslationLanguagesProvider>(
                feature = ENTRY_TRANSLATION_LANGUAGES_FEATURE_ID,
                integration = ENTRY_TRANSLATION_LANGUAGES_INTEGRATION_ID,
                behaviorProjection = behavior.id,
            )
        }
        .also { selected ->
            check(selected.distinct().size <= 1) {
                "Translation-language behaviors selected different provider sets: $selected"
            }
        }
        .firstOrNull()
        .orEmpty()
    private val migrationTypes = evaluation.applicableProviderTypes<EntryTranslationLanguagesProvider>(
        feature = ENTRY_TRANSLATION_LANGUAGES_FEATURE_ID,
        integration = ENTRY_TRANSLATION_LANGUAGES_MIGRATION_INTEGRATION_ID,
        behaviorProjection = EntryTranslationLanguagesMigrationBehavior.id,
    )

    override fun isApplicable(type: EntryType): Boolean = type in applicableTypes

    override fun observe(entry: Entry): Flow<EntryTranslationLanguageChoices> {
        if (!isApplicable(entry.type)) return flowOf(EntryTranslationLanguageChoices())
        return repository.subscribeByEntryId(entry.id)
            .map { stored -> stored?.toChoices() ?: EntryTranslationLanguageChoices() }
            .distinctUntilChanged()
    }

    override suspend fun setContentLanguage(
        entry: Entry,
        language: LanguageTag?,
    ): EntryTranslationLanguagesWriteResult {
        if (!isApplicable(entry.type)) return EntryTranslationLanguagesWriteResult.Inapplicable(entry.type)
        repository.setContentLanguage(entry.id, language?.value, clock())
        return EntryTranslationLanguagesWriteResult.Applied
    }

    override suspend fun setTargetLanguage(
        entry: Entry,
        language: LanguageTag?,
    ): EntryTranslationLanguagesWriteResult {
        if (!isApplicable(entry.type)) return EntryTranslationLanguagesWriteResult.Inapplicable(entry.type)
        repository.setTargetLanguage(entry.id, language?.value, clock())
        return EntryTranslationLanguagesWriteResult.Applied
    }

    override suspend fun setTranslateDownloads(entry: Entry, enabled: Boolean): EntryTranslationLanguagesWriteResult {
        if (!isApplicable(entry.type)) return EntryTranslationLanguagesWriteResult.Inapplicable(entry.type)
        repository.setTranslateDownloads(entry.id, enabled, clock())
        return EntryTranslationLanguagesWriteResult.Applied
    }

    suspend fun snapshot(entry: Entry): EntryTranslationLanguagesSnapshotResult {
        if (!isApplicable(entry.type)) return EntryTranslationLanguagesSnapshotResult.Inapplicable(entry.type)
        val stored = repository.getByEntryId(entry.id) ?: return EntryTranslationLanguagesSnapshotResult.NoLanguages
        return EntryTranslationLanguagesSnapshotResult.Captured(
            EntryTranslationLanguagesSnapshot(
                contentLanguage = stored.contentLanguage,
                targetLanguage = stored.targetLanguage,
                updatedAt = stored.updatedAt,
                translateDownloads = stored.translateDownloads,
            ),
        )
    }

    /** Replaces the entry's record with the snapshot's; tags that no longer parse are dropped. */
    suspend fun restore(
        entry: Entry,
        snapshot: EntryTranslationLanguagesSnapshot,
    ): EntryTranslationLanguagesWriteResult {
        if (!isApplicable(entry.type)) return EntryTranslationLanguagesWriteResult.Inapplicable(entry.type)
        val contentLanguage = snapshot.contentLanguage?.let(LanguageTag::parse)
        val targetLanguage = snapshot.targetLanguage?.let(LanguageTag::parse)
        if (contentLanguage != null || targetLanguage != null || snapshot.translateDownloads) {
            repository.upsert(
                EntryTranslationLanguages(
                    entryId = entry.id,
                    contentLanguage = contentLanguage?.value,
                    targetLanguage = targetLanguage?.value,
                    updatedAt = snapshot.updatedAt,
                    translateDownloads = snapshot.translateDownloads,
                ),
            )
        }
        return EntryTranslationLanguagesWriteResult.Applied
    }

    /**
     * Captures the target language and whether downloads are translated, not the content language: that describes
     * the source's text, so the Migration target follows the language its own source declares.
     */
    suspend fun prepareMigration(source: Entry, target: Entry): EntryTranslationLanguagesMigrationPreparation {
        if (source.type != target.type) {
            return EntryTranslationLanguagesMigrationPreparation.TypeMismatch(source.type, target.type)
        }
        val inapplicableTypes = setOf(source.type, target.type) - migrationTypes
        if (inapplicableTypes.isNotEmpty()) {
            return EntryTranslationLanguagesMigrationPreparation.Inapplicable(inapplicableTypes)
        }
        val stored = repository.getByEntryId(source.id)
        val targetLanguage = stored?.targetLanguage?.let(LanguageTag::parse)
        val translateDownloads = stored?.translateDownloads == true
        if (targetLanguage == null && !translateDownloads) {
            return EntryTranslationLanguagesMigrationPreparation.NothingToCarry
        }
        return EntryTranslationLanguagesMigrationPreparation.Prepared(
            EntryTranslationLanguagesMigrationPayload(target, targetLanguage?.value, translateDownloads),
        )
    }

    suspend fun applyMigration(
        payload: EntryTranslationLanguagesMigrationPayload,
    ): EntryTranslationLanguagesWriteResult {
        payload.targetLanguage?.let(LanguageTag::parse)?.let { language ->
            val result = setTargetLanguage(payload.target, language)
            if (result != EntryTranslationLanguagesWriteResult.Applied) return result
        }
        if (!payload.translateDownloads) return EntryTranslationLanguagesWriteResult.Applied
        return setTranslateDownloads(payload.target, enabled = true)
    }
}

private fun EntryTranslationLanguages.toChoices() = EntryTranslationLanguageChoices(
    contentLanguage = contentLanguage?.let(LanguageTag::parse),
    targetLanguage = targetLanguage?.let(LanguageTag::parse),
    translateDownloads = translateDownloads,
)
