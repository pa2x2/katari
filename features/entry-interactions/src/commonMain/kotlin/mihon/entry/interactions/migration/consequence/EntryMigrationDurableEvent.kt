package mihon.entry.interactions.migration.consequence

import mihon.entry.interactions.migration.EntryMigrationChildMatch
import mihon.entry.interactions.migration.EntryMigrationOption
import mihon.entry.interactions.migration.matchMigrationChildren
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

internal data class EntryMigrationDurableEvent(
    val operationId: String,
    val source: Entry,
    val target: Entry,
    val selectedOptions: Set<EntryMigrationOption>,
    val sourceChildren: List<EntryChapter>,
    val targetChildren: List<EntryChapter>,
) {
    val childMatches: List<EntryMigrationChildMatch> by lazy {
        matchMigrationChildren(sourceChildren, targetChildren)
    }
}
