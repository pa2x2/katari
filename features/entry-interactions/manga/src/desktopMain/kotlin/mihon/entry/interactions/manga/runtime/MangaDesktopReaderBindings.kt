package mihon.entry.interactions.manga.runtime

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.reader.window.DesktopMangaReader
import mihon.entry.interactions.manga.navigation.MangaOpenProcessor
import mihon.entry.interactions.navigation.EntryOpenProcessor
import mihon.entry.interactions.reader.window.MangaReader
import mihon.entry.interactions.reader.window.MangaReaderDestination
import mihon.entry.interactions.runtime.EntryTypeKey

/**
 * The desktop's manga reader: chapters open in the screen the app's [MangaReaderDestination] shows, read in the shared
 * reader.
 */
@ContributesTo(AppScope::class)
@BindingContainer
object MangaDesktopReaderBindings {
    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.MANGA)
    internal fun open(destination: MangaReaderDestination): EntryOpenProcessor = MangaOpenProcessor(destination)

    @Provides
    internal fun reader(reader: DesktopMangaReader): MangaReader = reader
}
