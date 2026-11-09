package app.katari.entry.interactions.anime.media.immersive

import app.katari.core.platform.PlatformContext
import app.katari.entry.interactions.media.EntryImmersiveHandle
import app.katari.entry.interactions.media.EntryImmersiveRenderer
import app.katari.entry.interactions.runtime.EntryImmersiveProcessor
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

/** Android's anime clips in the immersive feed: the shared [AnimeImmersiveClips], on ExoPlayer with touch gestures. */
internal class AnimeImmersiveProcessor(private val clips: AnimeImmersiveClips) : EntryImmersiveProcessor {
    override val loadMode get() = clips.loadMode
    override val preloadRadius get() = clips.preloadRadius

    override suspend fun load(
        context: PlatformContext,
        entry: Entry,
        chapter: EntryChapter?,
        source: UnifiedSource,
    ): EntryImmersiveHandle = clips.load(context, entry, chapter, source)

    override fun renderer(handle: EntryImmersiveHandle): EntryImmersiveRenderer = clips.renderer(handle)

    override fun release(handle: EntryImmersiveHandle) = Unit
}
