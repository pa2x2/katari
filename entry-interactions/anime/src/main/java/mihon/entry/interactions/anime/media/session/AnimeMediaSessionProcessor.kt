package mihon.entry.interactions.anime.media.session

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.media.EntryMediaSessionProcessor
import mihon.entry.interactions.media.session.EntryMediaSessionEvent
import mihon.entry.interactions.media.session.EntryMediaSessionEventSink
import mihon.entry.interactions.media.session.EntryMediaSessionResult

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
