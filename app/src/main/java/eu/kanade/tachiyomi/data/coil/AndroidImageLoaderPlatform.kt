package eu.kanade.tachiyomi.data.coil

import android.content.Context
import coil3.ComponentRegistry
import coil3.ImageLoader
import coil3.request.allowRgb565
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.util.system.DeviceUtil
import eu.kanade.tachiyomi.util.system.animatorDurationScale
import mihon.entry.interactions.runtime.addEntryInteractionImageComponents

/**
 * The application as the image loader's context, the system's animator duration scale, the entry types' image
 * components, and smaller bitmaps on a low-memory device. The disk cache is Coil's own, in the cache directory.
 */
@Inject
class AndroidImageLoaderPlatform(override val context: Context) : ImageLoaderPlatform {
    override val animatorDurationScale: Float
        get() = context.animatorDurationScale

    override fun addComponents(components: ComponentRegistry.Builder) {
        components.addEntryInteractionImageComponents(context)
    }

    override fun configure(builder: ImageLoader.Builder) {
        builder.allowRgb565(DeviceUtil.isLowRamDevice(context))
    }
}
