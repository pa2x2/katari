package mihon.desktop.image

import coil3.ComponentRegistry
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.disk.DiskCache
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.data.coil.ImageLoaderPlatform
import mihon.desktop.storage.DesktopDirectories
import mihon.entry.interactions.runtime.EntryInteractionImageComponents
import okio.Path.Companion.toOkioPath

/**
 * One image loader context for the whole application, the entry types' image components, and the disk cache in the
 * app's cache directory. Desktops have no system animation speed, so images fade in at the normal one.
 */
@Inject
class DesktopImageLoaderPlatform(
    private val directories: DesktopDirectories,
    private val entryImageComponents: EntryInteractionImageComponents,
) : ImageLoaderPlatform {
    override val context: PlatformContext = PlatformContext.INSTANCE

    override val animatorDurationScale: Float = 1f

    override fun addComponents(components: ComponentRegistry.Builder) {
        entryImageComponents.installInto(components)
    }

    override fun configure(builder: ImageLoader.Builder) {
        builder.diskCache {
            DiskCache.Builder()
                .directory(directories.cache.resolve("image_cache").toOkioPath())
                .build()
        }
    }
}
