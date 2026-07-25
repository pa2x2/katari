package mihon.entry.interactions.anime.player

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.EntryChildWebViewHostAdapter

internal object AnimeChildWebViewHostAdapter : EntryChildWebViewHostAdapter {
    override val type = EntryType.ANIME
}
