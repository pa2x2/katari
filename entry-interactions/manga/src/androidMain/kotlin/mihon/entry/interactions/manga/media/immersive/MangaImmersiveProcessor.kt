package mihon.entry.interactions.manga.media.immersive

import eu.kanade.tachiyomi.source.entry.UnifiedSource
import mihon.core.platform.PlatformContext
import mihon.entry.interactions.media.EntryImmersiveHandle
import mihon.entry.interactions.media.EntryImmersiveProgress
import mihon.entry.interactions.media.EntryImmersiveRenderer
import mihon.entry.interactions.runtime.EntryImmersiveProcessor
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

/** Android's manga chapters in the immersive feed: the shared [MangaImmersiveChapters]. */
internal class MangaImmersiveProcessor(private val chapters: MangaImmersiveChapters) : EntryImmersiveProcessor {
    override val loadMode get() = chapters.loadMode
    override val preloadRadius get() = chapters.preloadRadius

    override suspend fun load(
        context: PlatformContext,
        entry: Entry,
        chapter: EntryChapter?,
        source: UnifiedSource,
    ): EntryImmersiveHandle = chapters.load(context, entry, chapter, source)

    override fun renderer(handle: EntryImmersiveHandle): EntryImmersiveRenderer = chapters.renderer(handle)

    override suspend fun persistProgress(handle: EntryImmersiveHandle, progress: EntryImmersiveProgress) =
        chapters.persistProgress(handle, progress)

    override fun release(handle: EntryImmersiveHandle) = chapters.release(handle)
}
