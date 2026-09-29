package tachiyomi.domain.entry.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.entry.model.EntryTranslationLanguages

/**
 * Stores translation languages per entry and whether its downloads are translated; an entry that chooses none of them
 * has no record.
 */
interface EntryTranslationLanguagesRepository {

    suspend fun getByEntryId(entryId: Long): EntryTranslationLanguages?

    fun subscribeByEntryId(entryId: Long): Flow<EntryTranslationLanguages?>

    /** Records of the profile's entries that choose a language, ordered by entry title. */
    fun subscribeByProfile(profileId: Long): Flow<List<EntryTranslationLanguages>>

    /** Replaces the entry's record; [languages] must choose at least one language or translate downloads. */
    suspend fun upsert(languages: EntryTranslationLanguages)

    suspend fun setContentLanguage(entryId: Long, language: String?, updatedAt: Long)

    suspend fun setTargetLanguage(entryId: Long, language: String?, updatedAt: Long)

    suspend fun setTranslateDownloads(entryId: Long, enabled: Boolean, updatedAt: Long)

    /** Makes the entry follow the default languages again; whether its downloads are translated is kept. */
    suspend fun clearLanguages(entryId: Long, updatedAt: Long)

    /** [clearLanguages] for every entry of the profile. */
    suspend fun clearLanguagesByProfile(profileId: Long, updatedAt: Long)
}
