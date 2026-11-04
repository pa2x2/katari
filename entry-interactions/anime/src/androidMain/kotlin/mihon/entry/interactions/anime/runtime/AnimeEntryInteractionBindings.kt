package mihon.entry.interactions.anime.runtime

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import mihon.entry.interactions.anime.media.session.AnimeMediaSessionProcessor
import mihon.entry.interactions.media.session.EntryMediaSessionEventSink

@ContributesTo(AppScope::class)
@BindingContainer
object AnimeEntryInteractionBindings {
    @Provides
    @SingleIn(AppScope::class)
    internal fun mediaSession(sink: EntryMediaSessionEventSink): AnimeMediaSessionProcessor =
        AnimeMediaSessionProcessor(sink)
}
