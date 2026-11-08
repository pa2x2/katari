package app.katari.entry.interactions.anime.media.session

import app.katari.entry.interactions.media.EntryMediaSessionProcessor
import app.katari.entry.interactions.media.session.EntryMediaSessionEvent
import app.katari.entry.interactions.media.session.EntryMediaSessionEventSink
import app.katari.entry.interactions.media.session.EntryMediaSessionResult
import eu.kanade.tachiyomi.source.entry.EntryType

internal class AnimeMediaSessionProcessor(
    private val sink: EntryMediaSessionEventSink,
) : EntryMediaSessionProcessor {

    override suspend fun onEvent(event: EntryMediaSessionEvent): EntryMediaSessionResult {
        require(event.visibleEntry.type == EntryType.ANIME) {
            "Anime media-session processor cannot emit ${event.visibleEntry.type} events"
        }
        return sink.onEvent(event)
    }
}
