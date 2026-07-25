package mihon.entry.interactions.book.reader

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.EntryChildWebViewHostAdapter

internal object BookChildWebViewHostAdapter : EntryChildWebViewHostAdapter {
    override val type = EntryType.BOOK
}
