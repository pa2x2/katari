package mihon.entry.interactions.host.library

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import mihon.entry.interactions.EntryLibraryMembershipCommit
import mihon.entry.interactions.EntryLibraryMembershipHost
import mihon.entry.interactions.EntryLibraryMembershipPreparation
import mihon.feature.profiles.core.ProfileStore
import tachiyomi.data.DatabaseHandler
import tachiyomi.data.entry.EntryMapper
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.library.service.LibraryPreferences

class AppEntryLibraryMembershipHost(
    private val handler: DatabaseHandler,
    private val profileStore: ProfileStore,
    private val clockMillis: () -> Long = System::currentTimeMillis,
) : EntryLibraryMembershipHost {

    override suspend fun prepareAddition(entry: Entry): EntryLibraryMembershipPreparation {
        val preferences = LibraryPreferences(profileStore.profileStore(entry.profileId))
        val (categories, selectedCategoryIds) = handler.await {
            val categories = categoriesQueries.getCategories(entry.profileId, ::mapCategory).awaitAsList()
            val selected = categoriesQueries.getCategoriesByEntryId(
                entry.profileId,
                entry.id,
                ::mapCategory,
            ).awaitAsList().mapTo(mutableSetOf(), Category::id)
            categories to selected
        }
        return EntryLibraryMembershipPreparation(
            categories = categories,
            defaultCategoryId = preferences.defaultCategory.get().toLong(),
            selectedCategoryIds = selectedCategoryIds,
            defaultChildFlags = Entry.SHOW_ALL or
                preferences.sortChapterBySourceOrNumber.get() or
                preferences.displayChapterByNameOrNumber.get() or
                preferences.sortChapterByAscendingOrDescending.get(),
        )
    }

    override suspend fun add(
        entry: Entry,
        categoryIds: List<Long>,
        defaultChildFlags: Long,
    ): EntryLibraryMembershipCommit {
        return handler.await(inTransaction = true) {
            val persisted = entriesQueries.getEntryById(entry.id, entry.profileId, EntryMapper::mapEntry)
                .awaitAsOneOrNull()
                ?: return@await EntryLibraryMembershipCommit.Conflict
            if (persisted.favorite) return@await EntryLibraryMembershipCommit.NoChange
            if (persisted.type != entry.type) return@await EntryLibraryMembershipCommit.Conflict

            val distinctCategoryIds = categoryIds.distinct()
            if (distinctCategoryIds.any { categoryId ->
                    categoriesQueries.getCategory(categoryId, entry.profileId).awaitAsOneOrNull()?.id != categoryId
                }
            ) {
                return@await EntryLibraryMembershipCommit.Conflict
            }

            entriesQueries.prepareForLibrary(
                dateAdded = clockMillis(),
                chapterFlags = defaultChildFlags,
                profileId = entry.profileId,
                entryId = entry.id,
            )
            entries_categoriesQueries.deleteByEntryId(entry.profileId, entry.id)
            distinctCategoryIds.forEach { categoryId ->
                entries_categoriesQueries.insert(entry.profileId, entry.id, categoryId)
            }
            val added = entriesQueries.getEntryById(entry.id, entry.profileId, EntryMapper::mapEntry)
                .awaitAsOneOrNull()
                ?: return@await EntryLibraryMembershipCommit.Conflict
            EntryLibraryMembershipCommit.Applied(listOf(added))
        }
    }

    override suspend fun remove(
        entries: List<Entry>,
        beforeCommit: suspend (persistedEntries: List<Entry>) -> Unit,
    ): EntryLibraryMembershipCommit {
        return handler.await(inTransaction = true) {
            val persisted = buildList {
                entries.distinctBy { it.profileId to it.id }.forEach { requested ->
                    val actual = entriesQueries.getEntryById(
                        requested.id,
                        requested.profileId,
                        EntryMapper::mapEntry,
                    ).awaitAsOneOrNull() ?: return@await EntryLibraryMembershipCommit.Conflict
                    if (actual.type != requested.type) return@await EntryLibraryMembershipCommit.Conflict
                    if (actual.favorite) add(actual)
                }
            }
            if (persisted.isEmpty()) return@await EntryLibraryMembershipCommit.NoChange

            beforeCommit(persisted)
            persisted.forEach { entry ->
                entriesQueries.removeFromLibrary(entry.profileId, entry.id)
            }
            EntryLibraryMembershipCommit.Applied(
                persisted.map { entry -> entry.copy(favorite = false, dateAdded = 0L) },
            )
        }
    }
}

private fun mapCategory(
    id: Long,
    name: String,
    order: Long,
    flags: Long,
): Category = Category(id, name, order, flags)
