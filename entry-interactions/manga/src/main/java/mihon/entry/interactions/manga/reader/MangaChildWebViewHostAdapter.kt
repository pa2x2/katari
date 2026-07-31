package mihon.entry.interactions.manga.reader

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.source.EntryChildWebViewHostAdapter

internal object MangaChildWebViewHostAdapter : EntryChildWebViewHostAdapter {
    override val type = EntryType.MANGA
}
