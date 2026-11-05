package mihon.entry.interactions.manga.runtime

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.serialization.json.Json
import mihon.core.common.image.progressive.ProgressiveImageEngine
import mihon.core.platform.storage.AppPrivateDirectories
import mihon.entry.interactions.manga.page.MangaPageStore
import mihon.entry.interactions.manga.page.acquisition.MangaPageAcquisitionCoordinator
import mihon.entry.interactions.runtime.EntryPageImageCache

/** The chapter page cache the reader loads pages through and downloads take already-read pages from. */
@ContributesTo(AppScope::class)
@BindingContainer
object MangaPageBindings {
    @Provides
    @SingleIn(AppScope::class)
    internal fun pageStore(directories: AppPrivateDirectories, json: Json): MangaPageStore =
        MangaPageStore(directories, json)

    @Provides
    @SingleIn(AppScope::class)
    internal fun pageAcquisitionCoordinator(
        store: MangaPageStore,
        progressiveImageEngine: ProgressiveImageEngine,
    ): MangaPageAcquisitionCoordinator = MangaPageAcquisitionCoordinator(store, progressiveImageEngine)

    @Provides
    internal fun pageImageCache(store: MangaPageStore): EntryPageImageCache = store
}
