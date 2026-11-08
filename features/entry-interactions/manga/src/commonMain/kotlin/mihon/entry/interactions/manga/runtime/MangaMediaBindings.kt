package mihon.entry.interactions.manga.runtime

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.ui.reader.loader.ReaderPageSessionLoader
import mihon.entry.interactions.manga.media.MangaPageImageCache
import mihon.entry.interactions.manga.media.MangaPreviews
import mihon.entry.interactions.manga.media.immersive.MangaImmersiveChapters
import mihon.entry.interactions.manga.media.immersive.MangaImmersivePlatform
import mihon.entry.interactions.manga.media.session.MangaMediaSessionProcessor
import mihon.entry.interactions.reader.settings.MangaReaderSettings
import mihon.entry.interactions.runtime.EntryPageImageCache
import mihon.entry.interactions.settings.EntryInteractionPreferences
import mihon.entry.viewer.settings.ViewerSettingBinder
import tachiyomi.domain.entry.repository.EntryProgressRepository

/**
 * Manga's pages outside the reader, alike on every platform: previews of a chapter's first pages, chapters in the
 * immersive feed and the page cache they fill. The processors the app looks these up through are platform facilities,
 * so each platform binds its own over these, with the [MangaImmersivePlatform] the feed keeps the screen awake through.
 */
@ContributesTo(AppScope::class)
@BindingContainer
object MangaMediaBindings {
    @Provides
    @SingleIn(AppScope::class)
    internal fun previews(
        entryInteractionPreferences: EntryInteractionPreferences,
        pageSessionLoader: Lazy<ReaderPageSessionLoader>,
    ): MangaPreviews = MangaPreviews(entryInteractionPreferences, pageSessionLoader)

    @Provides
    @SingleIn(AppScope::class)
    internal fun immersiveChapters(
        entryProgressRepository: EntryProgressRepository,
        mediaSession: MangaMediaSessionProcessor,
        pageSessionLoader: Lazy<ReaderPageSessionLoader>,
        readerSettings: MangaReaderSettings,
        viewerSettingBinder: ViewerSettingBinder,
        platform: MangaImmersivePlatform,
    ): MangaImmersiveChapters = MangaImmersiveChapters(
        entryProgressRepository = entryProgressRepository,
        mediaSession = mediaSession,
        loadPageSession = { context, entry, chapter -> pageSessionLoader.value.load(context, entry, chapter) },
        bindKeepScreenOn = { entryId -> viewerSettingBinder.bind(readerSettings.display.keepScreenOn, entryId) },
        platform = platform,
    )

    @Provides
    @SingleIn(AppScope::class)
    internal fun pageImageCache(cache: () -> EntryPageImageCache): MangaPageImageCache = MangaPageImageCache(cache)
}
