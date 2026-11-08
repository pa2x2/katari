package eu.kanade.tachiyomi.data.coil

import android.graphics.Bitmap
import app.katari.core.image.PlatformBitmap
import coil3.Image
import coil3.asImage
import coil3.request.Options
import coil3.request.bitmapConfig
import tachiyomi.core.common.util.system.ImageUtil

internal actual fun PlatformBitmap.toCoilImage(options: Options): Image {
    if (options.bitmapConfig == Bitmap.Config.HARDWARE && ImageUtil.canUseHardwareBitmap(this)) {
        val hardware = copy(Bitmap.Config.HARDWARE, false)
        if (hardware != null) {
            recycle()
            return hardware.asImage()
        }
    }
    return asImage()
}
