package app.katari.entry.interactions.manga.runtime

import app.katari.entry.interactions.manga.media.MangaMediaCacheProvider
import app.katari.entry.interactions.manga.media.MangaPageImageCache
import app.katari.entry.interactions.manga.media.MangaPreviewProcessor
import app.katari.entry.interactions.manga.media.MangaPreviews
import app.katari.entry.interactions.manga.media.immersive.AndroidMangaImmersivePlatform
import app.katari.entry.interactions.manga.media.immersive.MangaImmersiveChapters
import app.katari.entry.interactions.manga.media.immersive.MangaImmersivePlatform
import app.katari.entry.interactions.manga.media.immersive.MangaImmersiveProcessor
import app.katari.entry.interactions.manga.navigation.MangaOpenProcessor
import app.katari.entry.interactions.media.EntryMediaCacheProvider
import app.katari.entry.interactions.navigation.EntryOpenProcessor
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
 * Manga's contributions that need Android: opening chapters in the reader activity, and Android's processors for
 * previews, the immersive feed (with how it keeps the screen on) and the page cache over the shared parts in
 * [MangaMediaBindings]. The rest is in [MangaEntryTypeBindings], [MangaDownloadBindings] and [MangaTranslateBindings],
 * which every platform has.
 */
@ContributesTo(AppScope::class)
@BindingContainer
object MangaEntryInteractionProviderBindings {
    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.MANGA)
    internal fun open(): EntryOpenProcessor = MangaOpenProcessor()

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
