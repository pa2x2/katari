package mihon.core.migration.migrations

import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import tachiyomi.core.common.util.lang.withIOContext

class CategoryPreferencesCleanupMigration : Migration {
    override val version: Float = 10f

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withIOContext {
        val libraryPreferences = migrationContext.libraryPreferences
        val downloadPreferences = migrationContext.downloadPreferences

        val getCategories = migrationContext.getCategories
        val allCategories = getCategories.await().map { it.id.toString() }.toSet()

        val defaultCategory = libraryPreferences.defaultCategory.get()
        if (defaultCategory.toString() !in allCategories) {
            libraryPreferences.defaultCategory.delete()
        }

        val categoryPreferences = listOf(
            downloadPreferences.removeExcludeCategories,
            downloadPreferences.downloadNewEntryChapterCategories,
            downloadPreferences.downloadNewEntryChapterCategoriesExclude,
        )
        categoryPreferences.forEach { preference ->
            val ids = preference.get()
            val garbageIds = ids.minus(allCategories)
            if (garbageIds.isEmpty()) return@forEach
            preference.set(ids.minus(garbageIds))
        }
        return@withIOContext true
    }
}
