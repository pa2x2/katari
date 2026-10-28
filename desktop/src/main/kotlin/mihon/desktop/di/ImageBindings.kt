package mihon.desktop.di

import coil3.ImageLoader
import coil3.PlatformContext
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.network.NetworkHelper
import kotlinx.coroutines.CoroutineScope
import mihon.core.metro.AppCoroutineScope
import mihon.desktop.image.DesktopCoverCacheDirectories
import mihon.desktop.image.desktopImageLoader
import mihon.desktop.storage.DesktopDirectories
import mihon.entry.interactions.source.EntryCoverNetworkFeature

@BindingContainer
object ImageBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun imageLoader(
        networkHelper: NetworkHelper,
        directories: DesktopDirectories,
        coverCache: Lazy<CoverCache>,
        coverNetworkFeature: Lazy<EntryCoverNetworkFeature>,
    ): ImageLoader = desktopImageLoader(networkHelper, directories, coverCache, coverNetworkFeature)

    @Provides
    @SingleIn(AppScope::class)
    fun coverCache(@AppCoroutineScope scope: CoroutineScope, directories: DesktopDirectories): CoverCache =
        CoverCache(scope, DesktopCoverCacheDirectories(directories))

    // Coil builds image requests against this; desktop has one for the whole application.
    @Provides
    fun imageRequestContext(): PlatformContext = PlatformContext.INSTANCE
}
