package tachiyomi.domain.entry.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.entry.model.EntryTranslationLanguages

/** Stores translation languages per entry; an entry whose languages are all cleared has no record. */
interface EntryTranslationLanguagesRepository {

    suspend fun getByEntryId(entryId: Long): EntryTranslationLanguages?

    fun subscribeByEntryId(entryId: Long): Flow<EntryTranslationLanguages?>

    /** Records of the profile's entries, ordered by entry title. */
    fun subscribeByProfile(profileId: Long): Flow<List<EntryTranslationLanguages>>

    /** Replaces both languages of an entry; [languages] must set at least one. */
    suspend fun upsert(languages: EntryTranslationLanguages)

    suspend fun setContentLanguage(entryId: Long, language: String?, updatedAt: Long)

    suspend fun setTargetLanguage(entryId: Long, language: String?, updatedAt: Long)

    suspend fun delete(entryId: Long)

    suspend fun deleteByProfile(profileId: Long)
}
