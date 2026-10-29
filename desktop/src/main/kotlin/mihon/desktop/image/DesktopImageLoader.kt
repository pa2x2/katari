package mihon.desktop.image

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.disk.DiskCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.coil.BufferedSourceFetcher
import eu.kanade.tachiyomi.data.coil.CoverRequestInterceptor
import eu.kanade.tachiyomi.data.coil.EntryCoverFetcher
import eu.kanade.tachiyomi.data.coil.EntryCoverKeyer
import eu.kanade.tachiyomi.data.coil.EntryKeyer
import eu.kanade.tachiyomi.data.coil.SourceImageFetcherFactory
import eu.kanade.tachiyomi.data.coil.SourceImageKeyer
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.file.InstalledFileExtensionIcons
import eu.kanade.tachiyomi.extension.util.ExtensionLoader
import eu.kanade.tachiyomi.network.NetworkHelper
import mihon.desktop.storage.DesktopDirectories
import mihon.entry.interactions.source.EntryCoverNetworkFeature
import okio.Path.Companion.toOkioPath

/**
 * The image loader every desktop window draws with: network images through the app's HTTP client, so they carry its
 * user agent, DNS and cookies, cached on disk in the cache directory and in memory as Coil sizes it by default. Entry
 * covers and source images, such as background art, load as on Android, library entries' covers through [CoverCache].
 * Installed extensions' icons come from their apks or, for file extensions, the icons their stores listed.
 */
fun desktopImageLoader(
    networkHelper: NetworkHelper,
    directories: DesktopDirectories,
    coverCache: Lazy<CoverCache>,
    coverNetworkFeature: Lazy<EntryCoverNetworkFeature>,
    extensionManager: Lazy<ExtensionManager>,
    extensionLoader: Lazy<ExtensionLoader>,
    fileExtensionIcons: Lazy<InstalledFileExtensionIcons>,
): ImageLoader {
    val callFactory = lazy { networkHelper.client }
    return ImageLoader.Builder(PlatformContext.INSTANCE)
        .components {
            add(OkHttpNetworkFetcherFactory(callFactory = callFactory::value))
            add(BufferedSourceFetcher.Factory())
            add(EntryCoverFetcher.EntryCoverFactory(callFactory, coverCache, coverNetworkFeature))
            add(EntryCoverFetcher.EntryFactory(callFactory, coverCache, coverNetworkFeature))
            add(SourceImageFetcherFactory(callFactory, coverNetworkFeature))
            add(EntryCoverKeyer(coverCache.value))
            add(EntryKeyer(coverCache.value))
            add(SourceImageKeyer())
            add(ExtensionIconFetcher.ExtensionFactory(extensionLoader, fileExtensionIcons))
            add(ExtensionIconFetcher.SourceFactory(extensionManager, extensionLoader, fileExtensionIcons))
            add(InstalledExtensionIconKeyer())
            add(InstalledSourceIconKeyer())
            add(CoverRequestInterceptor())
        }
        .diskCache {
            DiskCache.Builder()
                .directory(directories.cache.resolve("image_cache").toOkioPath())
                .build()
        }
        .build()
}
