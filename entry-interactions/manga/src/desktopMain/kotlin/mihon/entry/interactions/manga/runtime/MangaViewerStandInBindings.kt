package mihon.entry.interactions.manga.runtime

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.manga.reader.image.MangaPageImageExport
import mihon.entry.interactions.manga.reader.image.UnofferedMangaPageImageExport
import mihon.entry.interactions.navigation.EntryViewerStandIn
import mihon.entry.interactions.runtime.EntryTypeKey

/**
 * Desktop has no manga reader yet, so chapters stay openable and opening one says reading isn't here yet, and no page
 * image is handed out of it.
 */
@ContributesTo(AppScope::class)
@BindingContainer
object MangaViewerStandInBindings {
    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.MANGA)
    internal fun viewer(): EntryViewerStandIn = EntryViewerStandIn

    @Provides
    internal fun pageImageExport(): MangaPageImageExport = UnofferedMangaPageImageExport
}
