package mihon.desktop.image

import coil3.ComponentRegistry
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.disk.DiskCache
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.data.coil.ImageLoaderPlatform
import mihon.desktop.storage.DesktopDirectories
import okio.Path.Companion.toOkioPath

/**
 * One image loader context for the whole application, and the disk cache in the app's cache directory. Desktops have
 * no system animation speed, so images fade in at the normal one. No entry type loads images of its own on desktop yet.
 */
@Inject
class DesktopImageLoaderPlatform(private val directories: DesktopDirectories) : ImageLoaderPlatform {
    override val context: PlatformContext = PlatformContext.INSTANCE

    override val animatorDurationScale: Float = 1f

    override fun addComponents(components: ComponentRegistry.Builder) = Unit

    override fun configure(builder: ImageLoader.Builder) {
        builder.diskCache {
            DiskCache.Builder()
                .directory(directories.cache.resolve("image_cache").toOkioPath())
                .build()
        }
    }
}
