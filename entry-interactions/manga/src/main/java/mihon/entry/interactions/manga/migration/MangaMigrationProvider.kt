package mihon.entry.interactions.manga

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.EntryMigrationProvider

internal class MangaMigrationProvider : EntryMigrationProvider {
    override val type: EntryType = EntryType.MANGA
}
