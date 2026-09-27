package mihon.entry.interactions.translation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import tachiyomi.domain.entry.model.EntryTranslationLanguages
import tachiyomi.domain.entry.repository.EntryTranslationLanguagesRepository

/** Keeps records in memory with the database's rule that an entry without languages has no record. */
internal class InMemoryEntryTranslationLanguagesRepository : EntryTranslationLanguagesRepository {
    private val records = MutableStateFlow(emptyMap<Long, EntryTranslationLanguages>())

    override suspend fun getByEntryId(entryId: Long): EntryTranslationLanguages? = records.value[entryId]

    override fun subscribeByEntryId(entryId: Long): Flow<EntryTranslationLanguages?> = records.map { it[entryId] }

    override fun subscribeByProfile(profileId: Long): Flow<List<EntryTranslationLanguages>> =
        error("Profile listing needs entry ownership, which this fake does not model")

    override suspend fun upsert(languages: EntryTranslationLanguages) {
        require(languages.contentLanguage != null || languages.targetLanguage != null)
        records.update { it + (languages.entryId to languages) }
    }

    override suspend fun setContentLanguage(entryId: Long, language: String?, updatedAt: Long) =
        write(entryId) { it.copy(contentLanguage = language, updatedAt = updatedAt) }

    override suspend fun setTargetLanguage(entryId: Long, language: String?, updatedAt: Long) =
        write(entryId) { it.copy(targetLanguage = language, updatedAt = updatedAt) }

    override suspend fun delete(entryId: Long) = records.update { it - entryId }

    override suspend fun deleteByProfile(profileId: Long) =
        error("Profile deletion needs entry ownership, which this fake does not model")

    private fun write(entryId: Long, change: (EntryTranslationLanguages) -> EntryTranslationLanguages) {
        records.update { current ->
            val updated = change(current[entryId] ?: EntryTranslationLanguages(entryId, null, null, 0L))
            if (updated.contentLanguage == null && updated.targetLanguage == null) {
                current - entryId
            } else {
                current + (entryId to updated)
            }
        }
    }
}
