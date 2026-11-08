package app.katari.desktop.share

import androidx.compose.ui.graphics.ImageBitmap
import app.katari.core.platform.PlatformContext
import app.katari.core.platform.share.ImageSharing

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
