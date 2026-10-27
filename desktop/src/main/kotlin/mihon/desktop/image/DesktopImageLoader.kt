package mihon.desktop.image

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.disk.DiskCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import eu.kanade.tachiyomi.network.NetworkHelper
import mihon.desktop.storage.DesktopDirectories
import okio.Path.Companion.toOkioPath

/**
 * The image loader every desktop window draws with: network images through the app's HTTP client, so they carry its
 * user agent, DNS and cookies, cached on disk in the cache directory and in memory as Coil sizes it by default.
 */
fun desktopImageLoader(networkHelper: NetworkHelper, directories: DesktopDirectories): ImageLoader =
    ImageLoader.Builder(PlatformContext.INSTANCE)
        .components {
            add(OkHttpNetworkFetcherFactory(callFactory = { networkHelper.client }))
        }
        .diskCache {
            DiskCache.Builder()
                .directory(directories.cache.resolve("image_cache").toOkioPath())
                .build()
        }
        .build()
