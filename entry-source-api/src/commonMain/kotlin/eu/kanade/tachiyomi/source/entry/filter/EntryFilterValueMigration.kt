package eu.kanade.tachiyomi.source.entry.filter

/** Optional source-owned migration of encoded text/paged state. Return null for incompatible saved values. */
interface EntryFilterValueMigration {
    val filterStateVersion: Int
    fun migrateFilterValue(value: String, previousVersion: Int): String?
}
