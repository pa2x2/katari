package tachiyomi.domain.library.update.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.library.update.model.CategoryUpdateRules
import tachiyomi.domain.library.update.model.EntryUpdateMode

/** Update rules of the active profile, unless a profile is passed. */
interface LibraryUpdateRulesRepository {
    suspend fun getEntryMode(entryId: Long): EntryUpdateMode

    fun subscribeEntryMode(entryId: Long): Flow<EntryUpdateMode>

    /** Entries whose mode is not [EntryUpdateMode.FOLLOW_RULES]. */
    suspend fun getEntryModes(): Map<Long, EntryUpdateMode>

    suspend fun setEntryMode(entryIds: Collection<Long>, mode: EntryUpdateMode)

    /** Categories whose rules differ from the default; missing categories are checked and use the library rules. */
    suspend fun getCategoryRules(): Map<Long, CategoryUpdateRules>

    suspend fun getCategoryRules(profileId: Long): Map<Long, CategoryUpdateRules>

    fun subscribeCategoryRules(): Flow<Map<Long, CategoryUpdateRules>>

    suspend fun setCategoryRules(rules: CategoryUpdateRules)
}
