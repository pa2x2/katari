package eu.kanade.tachiyomi.data.coil

import coil3.ImageLoader
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import coil3.util.DebugLogger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.file.InstalledFileExtensionIcons
import eu.kanade.tachiyomi.extension.util.ExtensionLoader
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.NetworkPreferences
import kotlinx.coroutines.Dispatchers
import mihon.entry.interactions.source.EntryCoverNetworkFeature

/**
 * The image loader every screen draws with. Network images load through the app's HTTP client, so they carry its user
 * agent, DNS and cookies; library entries' covers through [CoverCache]; installed extensions' icons through
 * [ExtensionIconSource]. Images fade in at the platform's animation speed, and verbose network logging logs every load.
 */
@ContributesTo(AppScope::class)
@BindingContainer
object AppImageLoaderBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun imageLoader(
        platform: ImageLoaderPlatform,
        iconSource: ExtensionIconSource,
        networkHelper: Lazy<NetworkHelper>,
        networkPreferences: NetworkPreferences,
        coverCache: Lazy<CoverCache>,
        coverNetworkFeature: Lazy<EntryCoverNetworkFeature>,
        extensionManager: Lazy<ExtensionManager>,
        extensionLoader: Lazy<ExtensionLoader>,
        fileExtensionIcons: Lazy<InstalledFileExtensionIcons>,
    ): ImageLoader = ImageLoader.Builder(platform.context).apply {
        val callFactory = lazy { networkHelper.value.client }
        components {
            // NetworkFetcher.Factory
            add(OkHttpNetworkFetcherFactory(callFactory::value))
            // Fetcher.Factory
            add(BufferedSourceFetcher.Factory())
            add(EntryCoverFetcher.EntryCoverFactory(callFactory, coverCache, coverNetworkFeature))
            add(EntryCoverFetcher.EntryFactory(callFactory, coverCache, coverNetworkFeature))
            add(ExtensionIconFetcher.ExtensionFactory(iconSource, extensionLoader, fileExtensionIcons))
            add(ExtensionIconFetcher.SourceFactory(iconSource, extensionManager, extensionLoader, fileExtensionIcons))
            add(SourceImageFetcherFactory(callFactory, coverNetworkFeature))
            platform.addComponents(this)
            // Keyer
            add(EntryCoverKeyer(coverCache.value))
            add(EntryKeyer(coverCache.value))
            add(InstalledExtensionIconKeyer())
            add(InstalledSourceIconKeyer())
            add(SourceImageKeyer())
            // Interceptor
            add(CoverRequestInterceptor())
        }

        memoryCache(
            MemoryCache.Builder()
                .maxSizePercent(platform.context)
                .build(),
        )

        crossfade((CROSSFADE_MILLIS * platform.animatorDurationScale).toInt())
        if (networkPreferences.verboseLogging.get()) logger(DebugLogger())

        // Coil spawns a new thread for every image load by default
        fetcherCoroutineContext(Dispatchers.IO.limitedParallelism(8))
        decoderCoroutineContext(Dispatchers.IO.limitedParallelism(3))

        platform.configure(this)
    }
        .build()

    private const val CROSSFADE_MILLIS = 300
}
