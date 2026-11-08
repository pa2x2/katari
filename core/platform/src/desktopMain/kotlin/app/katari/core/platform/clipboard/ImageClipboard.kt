package app.katari.core.platform.clipboard

import app.katari.core.common.platform.PlatformFacility
import java.awt.image.BufferedImage

/** The desktop's clipboard, for images. */
@PlatformFacility
interface ImageClipboard {
    /** Puts [image] on the clipboard in place of what was there. */
    fun copy(image: BufferedImage)
}
