package mihon.entry.interactions.anime

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.EntryMigrationProvider

internal class AnimeMigrationProvider : EntryMigrationProvider {
    override val type: EntryType = EntryType.ANIME
}
