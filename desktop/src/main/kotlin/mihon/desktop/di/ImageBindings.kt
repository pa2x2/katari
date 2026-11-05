package mihon.desktop.di

import coil3.PlatformContext
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.coil.ExtensionIconSource
import eu.kanade.tachiyomi.data.coil.ImageLoaderPlatform
import kotlinx.coroutines.CoroutineScope
import mihon.core.metro.AppCoroutineScope
import mihon.desktop.image.ApkExtensionIconSource
import mihon.desktop.image.DesktopCoverCacheDirectories
import mihon.desktop.image.DesktopImageLoaderPlatform
import mihon.desktop.storage.DesktopDirectories

@BindingContainer
interface ImageBindings {
    @Binds
    val DesktopImageLoaderPlatform.imageLoaderPlatform: ImageLoaderPlatform

    @Binds
    val ApkExtensionIconSource.extensionIconSource: ExtensionIconSource

    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun coverCache(@AppCoroutineScope scope: CoroutineScope, directories: DesktopDirectories): CoverCache =
            CoverCache(scope, DesktopCoverCacheDirectories(directories))

        // Coil builds image requests against this; desktop has one for the whole application.
        @Provides
        fun imageRequestContext(): PlatformContext = PlatformContext.INSTANCE
    }
}
