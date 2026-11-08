package app.katari.entry.interactions.manga.runtime

import app.katari.core.platform.power.ScreenIdleInhibitor
import app.katari.entry.interactions.manga.media.MangaMediaCacheProvider
import app.katari.entry.interactions.manga.media.MangaPageImageCache
import app.katari.entry.interactions.manga.media.MangaPreviewProcessor
import app.katari.entry.interactions.manga.media.MangaPreviews
import app.katari.entry.interactions.manga.media.immersive.DesktopMangaImmersivePlatform
import app.katari.entry.interactions.manga.media.immersive.MangaImmersiveChapters
import app.katari.entry.interactions.manga.media.immersive.MangaImmersivePlatform
import app.katari.entry.interactions.manga.media.immersive.MangaImmersiveProcessor
import app.katari.entry.interactions.media.EntryMediaCacheProvider
import app.katari.entry.interactions.runtime.EntryImmersiveProcessor
import app.katari.entry.interactions.runtime.EntryPreviewConfigurationProvider
import app.katari.entry.interactions.runtime.EntryPreviewProcessor
import app.katari.entry.interactions.runtime.EntryTypeKey
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.source.entry.EntryType

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
