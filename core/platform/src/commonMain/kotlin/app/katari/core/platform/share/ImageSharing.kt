package app.katari.core.platform.share

import androidx.compose.ui.graphics.ImageBitmap
import app.katari.core.common.platform.PlatformFacility
import app.katari.core.platform.PlatformContext

/** Hands an image to another app the user picks, through the platform's share sheet. */
@PlatformFacility
interface ImageSharing {
    /** False on platforms without a share sheet; share actions are hidden there. */
    val isAvailable: Boolean

    /**
     * Shares [image] from [context] as a PNG file named [fileName]. Throws when the image can't be written or handed
     * over, for the caller to report.
     */
    suspend fun share(context: PlatformContext, image: ImageBitmap, fileName: String)
}
