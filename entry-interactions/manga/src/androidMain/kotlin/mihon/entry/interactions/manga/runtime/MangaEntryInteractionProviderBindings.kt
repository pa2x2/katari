package mihon.entry.interactions.manga.runtime

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.manga.media.MangaMediaCacheProvider
import mihon.entry.interactions.manga.media.MangaPageImageCache
import mihon.entry.interactions.manga.media.MangaPreviewProcessor
import mihon.entry.interactions.manga.media.MangaPreviews
import mihon.entry.interactions.manga.media.immersive.AndroidMangaImmersivePlatform
import mihon.entry.interactions.manga.media.immersive.MangaImmersiveChapters
import mihon.entry.interactions.manga.media.immersive.MangaImmersivePlatform
import mihon.entry.interactions.manga.media.immersive.MangaImmersiveProcessor
import mihon.entry.interactions.manga.navigation.MangaOpenProcessor
import mihon.entry.interactions.manga.translation.background.MangaEntryTranslateProcessor
import mihon.entry.interactions.media.EntryMediaCacheProvider
import mihon.entry.interactions.navigation.EntryOpenProcessor
import mihon.entry.interactions.runtime.EntryImmersiveProcessor
import mihon.entry.interactions.runtime.EntryPreviewConfigurationProvider
import mihon.entry.interactions.runtime.EntryPreviewProcessor
import mihon.entry.interactions.runtime.EntryTypeKey
import mihon.entry.interactions.translate.EntryTranslateProcessor

/**
 * Manga's contributions that need Android: opening chapters in the reader activity, translating downloaded chapters,
 * and Android's processors for previews, the immersive feed (with how it keeps the screen on) and the page cache over
 * the shared parts in [MangaMediaBindings]. The rest is in [MangaEntryTypeBindings] and [MangaDownloadBindings], which
 * every platform has.
 */
@ContributesTo(AppScope::class)
@BindingContainer
object MangaEntryInteractionProviderBindings {
    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.MANGA)
    internal fun open(): EntryOpenProcessor = MangaOpenProcessor()

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.MANGA)
    internal fun translate(processor: MangaEntryTranslateProcessor): EntryTranslateProcessor = processor

    @Provides
    internal fun immersivePlatform(): MangaImmersivePlatform = AndroidMangaImmersivePlatform

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
