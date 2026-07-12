package mihon.entry.interactions.anime

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.EntryCapabilityProcessor
import tachiyomi.domain.entry.model.Entry

internal class AnimeCapabilityProcessor : EntryCapabilityProcessor {
    override val type: EntryType = EntryType.ANIME

    override fun supportsMerge(entry: Entry): Boolean = true
}
