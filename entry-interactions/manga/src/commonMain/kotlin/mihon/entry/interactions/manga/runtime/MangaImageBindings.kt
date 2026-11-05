package mihon.entry.interactions.manga.runtime

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import mihon.entry.interactions.manga.reader.addMangaReaderImageComponents
import mihon.entry.interactions.runtime.EntryImageComponentInstaller

/** The image loader's decoder for the formats platforms lack, and its fetcher for reader pages. */
@ContributesTo(AppScope::class)
@BindingContainer
object MangaImageBindings {
    @Provides
    @IntoSet
    fun imageComponents(): EntryImageComponentInstaller = EntryImageComponentInstaller(::addMangaReaderImageComponents)
}
