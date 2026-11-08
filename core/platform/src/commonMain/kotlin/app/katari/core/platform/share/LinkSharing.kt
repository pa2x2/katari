package app.katari.core.platform.share

import app.katari.core.common.platform.PlatformFacility
import app.katari.core.platform.PlatformContext

/** Hands a link to another app the user picks, through the platform's share sheet. */
@PlatformFacility
interface LinkSharing {
    /** False on platforms without a share sheet; share actions are hidden there. */
    val isAvailable: Boolean

    /** Shares [url] from [context]; a failure to open the share sheet is reported by the implementation. */
    fun share(context: PlatformContext, url: String)
}
