package mihon.entry.interactions.anime.migration

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.state.EntryMigrationProvider

internal class AnimeMigrationProvider : EntryMigrationProvider {
    override val type: EntryType = EntryType.ANIME
}
