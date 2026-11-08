package app.katari.android.share

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import app.katari.core.platform.PlatformContext
import app.katari.core.platform.share.ImageSharing
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.util.storage.cacheImageDir
import eu.kanade.tachiyomi.util.storage.getUriCompat
import eu.kanade.tachiyomi.util.system.toShareIntent
import tachiyomi.core.common.util.lang.withIOContext
import java.io.File

/** Writes the image into the cache's shared image folder and offers it through the system share sheet. */
@Inject
class AndroidImageSharing : ImageSharing {
    override val isAvailable: Boolean = true

    override suspend fun share(context: PlatformContext, image: ImageBitmap, fileName: String) {
        val file = withIOContext { writePng(image.asAndroidBitmap(), File(context.cacheImageDir, fileName)) }
        context.startActivity(file.getUriCompat(context).toShareIntent(context, type = "image/png"))
    }

    private fun writePng(bitmap: Bitmap, file: File): File {
        file.parentFile?.mkdirs()
        // A captured layer may come back as a hardware bitmap, which can't be encoded directly.
        val encodable = if (bitmap.config == Bitmap.Config.HARDWARE) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            bitmap
        }
        file.outputStream().use { encodable.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return file
    }
}
