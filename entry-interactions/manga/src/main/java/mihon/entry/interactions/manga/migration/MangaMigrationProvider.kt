package mihon.entry.interactions.manga.migration

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.state.EntryMigrationProvider

internal class MangaMigrationProvider : EntryMigrationProvider {
    override val type: EntryType = EntryType.MANGA
}
