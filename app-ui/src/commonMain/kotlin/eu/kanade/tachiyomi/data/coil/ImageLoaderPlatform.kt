package eu.kanade.tachiyomi.data.coil

import coil3.ComponentRegistry
import coil3.ImageLoader
import coil3.PlatformContext

/** What the app's image loader ([AppImageLoaderBindings]) takes from the platform it runs on. */
interface ImageLoaderPlatform {
    val context: PlatformContext

    /** How much slower than normal the system plays animations, 0 when it turns them off. */
    val animatorDurationScale: Float

    /** The platform's own fetchers and decoders, such as an entry type's reader pages', added before the keyers. */
    fun addComponents(components: ComponentRegistry.Builder)

    /** What only the platform sets, such as Android's smaller bitmaps on a low-memory device or a disk cache folder. */
    fun configure(builder: ImageLoader.Builder)
}
