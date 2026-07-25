package mihon.entry.interactions.book

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.EntryMigrationProvider

internal class BookMigrationProvider : EntryMigrationProvider {
    override val type: EntryType = EntryType.BOOK
}
