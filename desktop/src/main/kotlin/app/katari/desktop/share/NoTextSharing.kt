package app.katari.desktop.share

import app.katari.core.platform.PlatformContext
import app.katari.core.platform.share.TextSharing

/**
 * Linux desktops have no share sheet in common that hands text to another app, so sharing text is not offered and
 * share actions stay hidden.
 */
class NoTextSharing : TextSharing {
    override val isAvailable: Boolean = false

    override fun share(context: PlatformContext, text: String) {
        error("Sharing is not offered on this desktop")
    }
}
