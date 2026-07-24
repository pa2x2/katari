package mihon.entry.interactions.manga

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.EntryMediaSessionEvent
import mihon.entry.interactions.EntryMediaSessionEventSink
import mihon.entry.interactions.EntryMediaSessionProcessor
import mihon.entry.interactions.EntryMediaSessionResult

internal class MangaMediaSessionProcessor(
    private val sink: EntryMediaSessionEventSink,
) : EntryMediaSessionProcessor {
    override val type = EntryType.MANGA

    override suspend fun onEvent(event: EntryMediaSessionEvent): EntryMediaSessionResult {
        require(event.visibleEntry.type == type) {
            "Manga media-session processor cannot emit ${event.visibleEntry.type} events"
        }
        return sink.onEvent(event)
    }
}
