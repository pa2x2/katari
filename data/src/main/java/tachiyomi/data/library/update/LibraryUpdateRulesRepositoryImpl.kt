package tachiyomi.data.library.update

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.library.update.model.CategoryUpdateOverrides
import tachiyomi.domain.library.update.model.CategoryUpdateRules
import tachiyomi.domain.library.update.model.EntryUpdateMode
import tachiyomi.domain.library.update.model.LibraryUpdateSkipRules
import tachiyomi.domain.library.update.repository.LibraryUpdateRulesRepository

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryUpdateRulesRepositoryImpl(
    private val handler: DatabaseHandler,
    private val profileProvider: ActiveProfileProvider,
) : LibraryUpdateRulesRepository {

    override suspend fun getEntryMode(entryId: Long): EntryUpdateMode {
        return handler.awaitOneOrNull { entry_update_modesQueries.getByEntryId(entryId) }
            .toEntryUpdateMode()
    }

    override fun subscribeEntryMode(entryId: Long): Flow<EntryUpdateMode> {
        return handler.subscribeToOneOrNull { entry_update_modesQueries.getByEntryId(entryId) }
            .map { it.toEntryUpdateMode() }
    }

    override suspend fun getEntryModes(): Map<Long, EntryUpdateMode> {
        return handler.awaitList {
            entry_update_modesQueries.getByProfile(profileProvider.activeProfileId) { entryId, mode ->
                entryId to mode.toEntryUpdateMode()
            }
        }
            .toMap()
    }

    override fun subscribeEntryModes(): Flow<Map<Long, EntryUpdateMode>> {
        return profileProvider.activeProfileIdFlow.flatMapLatest { profileId ->
            handler.subscribeToList {
                entry_update_modesQueries.getByProfile(profileId) { entryId, mode ->
                    entryId to mode.toEntryUpdateMode()
                }
            }
        }
            .map { it.toMap() }
    }

    override suspend fun setEntryMode(entryIds: Collection<Long>, mode: EntryUpdateMode) {
        handler.await(inTransaction = true) {
            entryIds.forEach { entryId ->
                when (mode) {
                    EntryUpdateMode.FOLLOW_RULES -> entry_update_modesQueries.delete(entryId)
                    EntryUpdateMode.ALWAYS, EntryUpdateMode.NEVER ->
                        entry_update_modesQueries.upsert(entryId, mode.name.lowercase())
                }
            }
        }
    }

    override suspend fun getCategoryRules(): Map<Long, CategoryUpdateRules> {
        return getCategoryRules(profileProvider.activeProfileId)
    }

    override suspend fun getCategoryRules(profileId: Long): Map<Long, CategoryUpdateRules> {
        return handler.awaitList { category_update_rulesQueries.getByProfile(profileId, ::mapCategoryRules) }
            .associateBy(CategoryUpdateRules::categoryId)
    }

    override fun subscribeCategoryRules(): Flow<Map<Long, CategoryUpdateRules>> {
        return profileProvider.activeProfileIdFlow.flatMapLatest { profileId ->
            handler.subscribeToList { category_update_rulesQueries.getByProfile(profileId, ::mapCategoryRules) }
        }
            .map { rules -> rules.associateBy(CategoryUpdateRules::categoryId) }
    }

    override suspend fun setCategoryRules(rules: CategoryUpdateRules) {
        setCategoryRules(profileProvider.activeProfileId, rules)
    }

    override suspend fun setCategoryRules(profileId: Long, rules: CategoryUpdateRules) {
        handler.await {
            if (rules.isDefault) {
                category_update_rulesQueries.delete(profileId, rules.categoryId)
                return@await
            }
            val overrides = rules.overrides
            val skipRules = overrides?.skipRules ?: LibraryUpdateSkipRules.None
            category_update_rulesQueries.upsert(
                profileId = profileId,
                categoryId = rules.categoryId,
                autoUpdate = rules.autoUpdate,
                customRules = overrides != null,
                skipCompleted = skipRules.skipCompleted,
                skipUnseen = skipRules.skipUnseen,
                skipNotStarted = skipRules.skipNotStarted,
                skipOutsideReleasePeriod = skipRules.skipOutsideReleasePeriod,
                intervalHours = overrides?.intervalHours?.toLong(),
            )
        }
    }

    private fun mapCategoryRules(
        categoryId: Long,
        autoUpdate: Boolean,
        customRules: Boolean,
        skipCompleted: Boolean,
        skipUnseen: Boolean,
        skipNotStarted: Boolean,
        skipOutsideReleasePeriod: Boolean,
        intervalHours: Long?,
    ): CategoryUpdateRules = CategoryUpdateRules(
        categoryId = categoryId,
        autoUpdate = autoUpdate,
        overrides = if (customRules) {
            CategoryUpdateOverrides(
                skipRules = LibraryUpdateSkipRules(
                    skipCompleted = skipCompleted,
                    skipUnseen = skipUnseen,
                    skipNotStarted = skipNotStarted,
                    skipOutsideReleasePeriod = skipOutsideReleasePeriod,
                ),
                intervalHours = intervalHours?.toInt(),
            )
        } else {
            null
        },
    )
}

private fun String?.toEntryUpdateMode(): EntryUpdateMode {
    return when (this) {
        null -> EntryUpdateMode.FOLLOW_RULES
        else -> EntryUpdateMode.valueOf(uppercase())
    }
}
