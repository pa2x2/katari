package mihon.entry.interactions.anime

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.EntryMediaSessionEvent
import mihon.entry.interactions.EntryMediaSessionEventSink
import mihon.entry.interactions.EntryMediaSessionProcessor
import mihon.entry.interactions.EntryMediaSessionResult

internal class AnimeMediaSessionProcessor(
    private val sink: EntryMediaSessionEventSink,
) : EntryMediaSessionProcessor {
    override val type = EntryType.ANIME

    override suspend fun onEvent(event: EntryMediaSessionEvent): EntryMediaSessionResult {
        require(event.visibleEntry.type == type) {
            "Anime media-session processor cannot emit ${event.visibleEntry.type} events"
        }
        return sink.onEvent(event)
    }
}
