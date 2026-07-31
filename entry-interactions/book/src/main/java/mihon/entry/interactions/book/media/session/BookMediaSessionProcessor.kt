package mihon.entry.interactions.book.media.session

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.media.EntryMediaSessionProcessor
import mihon.entry.interactions.media.session.EntryMediaSessionEvent
import mihon.entry.interactions.media.session.EntryMediaSessionEventSink
import mihon.entry.interactions.media.session.EntryMediaSessionResult

internal class BookMediaSessionProcessor(
    private val sink: EntryMediaSessionEventSink,
) : EntryMediaSessionProcessor {
    override val type = EntryType.BOOK

    override suspend fun onEvent(event: EntryMediaSessionEvent): EntryMediaSessionResult {
        require(event.visibleEntry.type == type) {
            "Book media-session processor cannot emit ${event.visibleEntry.type} events"
        }
        return sink.onEvent(event)
    }
}
