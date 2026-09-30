package mihon.entry.interactions.translation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import tachiyomi.domain.entry.model.EntryTranslationLanguages
import tachiyomi.domain.entry.repository.EntryTranslationLanguagesRepository

/** Keeps records in memory with the database's rule that an entry that chooses nothing has no record. */
internal class InMemoryEntryTranslationLanguagesRepository : EntryTranslationLanguagesRepository {
    private val records = MutableStateFlow(emptyMap<Long, EntryTranslationLanguages>())

    override suspend fun getByEntryId(entryId: Long): EntryTranslationLanguages? = records.value[entryId]

    override fun subscribeByEntryId(entryId: Long): Flow<EntryTranslationLanguages?> = records.map { it[entryId] }

    override fun subscribeByProfile(profileId: Long): Flow<List<EntryTranslationLanguages>> =
        error("Profile listing needs entry ownership, which this fake does not model")

    override suspend fun upsert(languages: EntryTranslationLanguages) {
        require(languages.contentLanguage != null || languages.targetLanguage != null || languages.translateDownloads)
        records.update { it + (languages.entryId to languages) }
    }

    override suspend fun setContentLanguage(entryId: Long, language: String?, updatedAt: Long) =
        write(entryId) { it.copy(contentLanguage = language, updatedAt = updatedAt) }

    override suspend fun setTargetLanguage(entryId: Long, language: String?, updatedAt: Long) =
        write(entryId) { it.copy(targetLanguage = language, updatedAt = updatedAt) }

    override suspend fun setTranslateDownloads(entryId: Long, enabled: Boolean, updatedAt: Long) =
        write(entryId) { it.copy(translateDownloads = enabled, updatedAt = updatedAt) }

    override suspend fun clearLanguages(entryId: Long, updatedAt: Long) =
        write(entryId) { it.copy(contentLanguage = null, targetLanguage = null, updatedAt = updatedAt) }

    override suspend fun clearLanguagesByProfile(profileId: Long, updatedAt: Long) =
        error("Profile listing needs entry ownership, which this fake does not model")

    private fun write(entryId: Long, change: (EntryTranslationLanguages) -> EntryTranslationLanguages) {
        records.update { current ->
            val updated = change(current[entryId] ?: EntryTranslationLanguages(entryId, null, null, 0L))
            if (updated.contentLanguage == null && updated.targetLanguage == null && !updated.translateDownloads) {
                current - entryId
            } else {
                current + (entryId to updated)
            }
        }
    }
}
