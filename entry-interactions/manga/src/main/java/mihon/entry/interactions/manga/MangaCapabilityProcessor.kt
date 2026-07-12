package mihon.entry.interactions.manga

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.EntryCapabilityProcessor
import tachiyomi.domain.entry.model.Entry

internal class MangaCapabilityProcessor : EntryCapabilityProcessor {
    override val type: EntryType = EntryType.MANGA

    override fun supportsMigration(entry: Entry): Boolean = true

    override fun supportsMerge(entry: Entry): Boolean = true
}
