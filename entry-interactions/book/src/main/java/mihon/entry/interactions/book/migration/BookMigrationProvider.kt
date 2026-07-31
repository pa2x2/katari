package mihon.entry.interactions.book.migration

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.state.EntryMigrationProvider

internal class BookMigrationProvider : EntryMigrationProvider {
    override val type: EntryType = EntryType.BOOK
}
