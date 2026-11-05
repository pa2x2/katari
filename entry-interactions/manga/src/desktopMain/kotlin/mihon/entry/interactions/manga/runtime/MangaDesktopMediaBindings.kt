package mihon.entry.interactions.manga.runtime

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.core.platform.power.ScreenIdleInhibitor
import mihon.entry.interactions.manga.media.MangaMediaCacheProvider
import mihon.entry.interactions.manga.media.MangaPageImageCache
import mihon.entry.interactions.manga.media.MangaPreviewProcessor
import mihon.entry.interactions.manga.media.MangaPreviews
import mihon.entry.interactions.manga.media.immersive.DesktopMangaImmersivePlatform
import mihon.entry.interactions.manga.media.immersive.MangaImmersiveChapters
import mihon.entry.interactions.manga.media.immersive.MangaImmersivePlatform
import mihon.entry.interactions.manga.media.immersive.MangaImmersiveProcessor
import mihon.entry.interactions.media.EntryMediaCacheProvider
import mihon.entry.interactions.runtime.EntryImmersiveProcessor
import mihon.entry.interactions.runtime.EntryPreviewConfigurationProvider
import mihon.entry.interactions.runtime.EntryPreviewProcessor
import mihon.entry.interactions.runtime.EntryTypeKey

/**
 * The desktop's processors for manga's pages outside the reader, over the shared parts in [MangaMediaBindings]:
 * previews, the immersive feed, which keeps the screen awake through the session, and the page cache.
 */
@ContributesTo(AppScope::class)
@BindingContainer
object MangaDesktopMediaBindings {
    @Provides
    internal fun immersivePlatform(inhibitor: ScreenIdleInhibitor): MangaImmersivePlatform =
        DesktopMangaImmersivePlatform(inhibitor)

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.MANGA)
    internal fun immersive(
        chapters: MangaImmersiveChapters,
    ): EntryImmersiveProcessor = MangaImmersiveProcessor(chapters)

    @Provides
    @SingleIn(AppScope::class)
    internal fun previewProcessor(previews: MangaPreviews): MangaPreviewProcessor = MangaPreviewProcessor(previews)

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.MANGA)
    internal fun preview(processor: MangaPreviewProcessor): EntryPreviewProcessor = processor

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.MANGA)
    internal fun previewConfiguration(processor: MangaPreviewProcessor): EntryPreviewConfigurationProvider = processor

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.MANGA)
    internal fun mediaCache(cache: MangaPageImageCache): EntryMediaCacheProvider = MangaMediaCacheProvider(cache)
}
