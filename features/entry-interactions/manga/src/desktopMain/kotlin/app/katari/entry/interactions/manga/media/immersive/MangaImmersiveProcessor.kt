package app.katari.entry.interactions.manga.media.immersive

import app.katari.core.platform.PlatformContext
import app.katari.entry.interactions.media.EntryImmersiveHandle
import app.katari.entry.interactions.media.EntryImmersiveProgress
import app.katari.entry.interactions.media.EntryImmersiveRenderer
import app.katari.entry.interactions.runtime.EntryImmersiveProcessor
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

/** The desktop's manga chapters in the immersive feed: the shared [MangaImmersiveChapters]. */
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
