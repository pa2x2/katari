package mihon.entry.interactions.anime.media.immersive

import eu.kanade.tachiyomi.source.entry.UnifiedSource
import mihon.core.platform.PlatformContext
import mihon.entry.interactions.media.EntryImmersiveHandle
import mihon.entry.interactions.media.EntryImmersiveProgress
import mihon.entry.interactions.media.EntryImmersiveRenderer
import mihon.entry.interactions.runtime.EntryImmersiveProcessor
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

    override suspend fun persistProgress(handle: EntryImmersiveHandle, progress: EntryImmersiveProgress) =
        clips.persistProgress(handle, progress)

    override fun release(handle: EntryImmersiveHandle) = Unit
}
