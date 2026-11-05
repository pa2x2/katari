package mihon.core.platform.clipboard

import mihon.core.common.platform.PlatformFacility
import java.awt.image.BufferedImage

/** The desktop's clipboard, for images. */
@PlatformFacility
interface ImageClipboard {
    /** Puts [image] on the clipboard in place of what was there. */
    fun copy(image: BufferedImage)
}
