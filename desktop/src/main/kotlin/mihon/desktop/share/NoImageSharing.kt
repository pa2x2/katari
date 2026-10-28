package mihon.desktop.share

import androidx.compose.ui.graphics.ImageBitmap
import mihon.core.platform.PlatformContext
import mihon.core.platform.share.ImageSharing

/**
 * Linux desktops have no share sheet in common that hands a file to another app, so sharing is not offered and share
 * actions stay hidden.
 */
class NoImageSharing : ImageSharing {
    override val isAvailable: Boolean = false

    override suspend fun share(context: PlatformContext, image: ImageBitmap, fileName: String) {
        error("Sharing is not offered on this desktop")
    }
}
