package app.katari.desktop.share

import app.katari.core.platform.PlatformContext
import app.katari.core.platform.share.LinkSharing

/**
 * Linux desktops have no share sheet in common that hands a link to another app, so sharing links is not offered and
 * share actions stay hidden.
 */
class NoLinkSharing : LinkSharing {
    override val isAvailable: Boolean = false

    override fun share(context: PlatformContext, url: String) {
        error("Sharing is not offered on this desktop")
    }
}
