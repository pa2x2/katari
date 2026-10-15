package mihon.entry.interactions.manga.media.session

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.media.EntryMediaSessionProcessor
import mihon.entry.interactions.media.session.EntryMediaSessionEvent
import mihon.entry.interactions.media.session.EntryMediaSessionEventSink
import mihon.entry.interactions.media.session.EntryMediaSessionResult

internal class MangaMediaSessionProcessor(
    private val sink: EntryMediaSessionEventSink,
) : EntryMediaSessionProcessor {

    override suspend fun onEvent(event: EntryMediaSessionEvent): EntryMediaSessionResult {
        require(event.visibleEntry.type == EntryType.MANGA) {
            "Manga media-session processor cannot emit ${event.visibleEntry.type} events"
        }
        return sink.onEvent(event)
    }
}
