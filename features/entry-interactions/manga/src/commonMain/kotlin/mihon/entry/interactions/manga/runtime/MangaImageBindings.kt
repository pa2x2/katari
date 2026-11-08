package mihon.entry.interactions.manga.runtime

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import kotlinx.coroutines.CoroutineScope
import mihon.core.metro.AppCoroutineScope
import mihon.core.metro.StartupWarmup
import mihon.entry.interactions.manga.reader.addMangaReaderImageComponents
import mihon.entry.interactions.manga.reader.image.profile.DisplayProfile
import mihon.entry.interactions.reader.settings.ReaderBasePreferences
import mihon.entry.interactions.runtime.EntryImageComponentInstaller
import tachiyomi.core.common.storage.StorageFileResolver

/**
 * The image loader's decoder for the formats platforms lack, its fetcher for reader pages, and the display profile
 * every page decodes to.
 */
@ContributesTo(AppScope::class)
@BindingContainer
object MangaImageBindings {
    @Provides
    @IntoSet
    fun imageComponents(): EntryImageComponentInstaller = EntryImageComponentInstaller(::addMangaReaderImageComponents)

    // Previews and the immersive view decode pages before any reader opens
    @Provides
    @IntoSet
    fun displayProfileWarmup(
        preferences: ReaderBasePreferences,
        files: StorageFileResolver,
        @AppCoroutineScope scope: CoroutineScope,
    ): StartupWarmup = StartupWarmup { DisplayProfile.follow(preferences.displayProfile, files, scope) }
}
