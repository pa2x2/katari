package mihon.core.platform.share

import androidx.compose.ui.graphics.ImageBitmap
import mihon.core.platform.PlatformContext

/** Hands an image to another app the user picks, through the platform's share sheet. */
interface ImageSharing {
    /** False on platforms without a share sheet; share actions are hidden there. */
    val isAvailable: Boolean

    /**
     * Shares [image] from [context] as a PNG file named [fileName]. Throws when the image can't be written or handed
     * over, for the caller to report.
     */
    suspend fun share(context: PlatformContext, image: ImageBitmap, fileName: String)
}
