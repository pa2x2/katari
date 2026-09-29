package tachiyomi.data.entry

import kotlinx.coroutines.flow.Flow
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.entry.model.EntryTranslationLanguages
import tachiyomi.domain.entry.repository.EntryTranslationLanguagesRepository

class EntryTranslationLanguagesRepositoryImpl(
    private val handler: DatabaseHandler,
) : EntryTranslationLanguagesRepository {

    override suspend fun getByEntryId(entryId: Long): EntryTranslationLanguages? {
        return handler.awaitOneOrNull {
            entry_translation_languagesQueries.getByEntryId(entryId, ::mapLanguages)
        }
    }

    override fun subscribeByEntryId(entryId: Long): Flow<EntryTranslationLanguages?> {
        return handler.subscribeToOneOrNull {
            entry_translation_languagesQueries.getByEntryId(entryId, ::mapLanguages)
        }
    }

    override fun subscribeByProfile(profileId: Long): Flow<List<EntryTranslationLanguages>> {
        return handler.subscribeToList {
            entry_translation_languagesQueries.getByProfile(profileId, ::mapLanguages)
        }
    }

    override suspend fun upsert(languages: EntryTranslationLanguages) {
        require(languages.contentLanguage != null || languages.targetLanguage != null || languages.translateDownloads) {
            "Entry translation languages must choose a language or translate downloads"
        }
        handler.await {
            entry_translation_languagesQueries.upsert(
                entryId = languages.entryId,
                contentLanguage = languages.contentLanguage,
                targetLanguage = languages.targetLanguage,
                translateDownloads = languages.translateDownloads,
                updatedAt = languages.updatedAt,
            )
        }
    }

    override suspend fun setContentLanguage(entryId: Long, language: String?, updatedAt: Long) {
        handler.await(inTransaction = true) {
            if (language != null) {
                entry_translation_languagesQueries.upsertContentLanguage(
                    entryId = entryId,
                    language = language,
                    updatedAt = updatedAt,
                )
            } else {
                entry_translation_languagesQueries.deleteIfOnlyContentLanguage(entryId)
                entry_translation_languagesQueries.clearContentLanguage(updatedAt = updatedAt, entryId = entryId)
            }
        }
    }

    override suspend fun setTargetLanguage(entryId: Long, language: String?, updatedAt: Long) {
        handler.await(inTransaction = true) {
            if (language != null) {
                entry_translation_languagesQueries.upsertTargetLanguage(
                    entryId = entryId,
                    language = language,
                    updatedAt = updatedAt,
                )
            } else {
                entry_translation_languagesQueries.deleteIfOnlyTargetLanguage(entryId)
                entry_translation_languagesQueries.clearTargetLanguage(updatedAt = updatedAt, entryId = entryId)
            }
        }
    }

    override suspend fun setTranslateDownloads(entryId: Long, enabled: Boolean, updatedAt: Long) {
        handler.await(inTransaction = true) {
            if (enabled) {
                entry_translation_languagesQueries.enableTranslateDownloads(entryId = entryId, updatedAt = updatedAt)
            } else {
                entry_translation_languagesQueries.deleteIfOnlyTranslateDownloads(entryId)
                entry_translation_languagesQueries.clearTranslateDownloads(updatedAt = updatedAt, entryId = entryId)
            }
        }
    }

    override suspend fun clearLanguages(entryId: Long, updatedAt: Long) {
        handler.await(inTransaction = true) {
            entry_translation_languagesQueries.deleteIfOnlyLanguages(entryId)
            entry_translation_languagesQueries.clearLanguages(updatedAt = updatedAt, entryId = entryId)
        }
    }

    override suspend fun clearLanguagesByProfile(profileId: Long, updatedAt: Long) {
        handler.await(inTransaction = true) {
            entry_translation_languagesQueries.deleteLanguagesOnlyByProfile(profileId)
            entry_translation_languagesQueries.clearLanguagesByProfile(updatedAt = updatedAt, profileId = profileId)
        }
    }

    private fun mapLanguages(
        entryId: Long,
        contentLanguage: String?,
        targetLanguage: String?,
        translateDownloads: Boolean,
        updatedAt: Long,
    ) = EntryTranslationLanguages(
        entryId = entryId,
        contentLanguage = contentLanguage,
        targetLanguage = targetLanguage,
        updatedAt = updatedAt,
        translateDownloads = translateDownloads,
    )
}
