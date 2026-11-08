package app.katari.android.share

import androidx.core.net.toUri
import app.katari.core.platform.PlatformContext
import app.katari.core.platform.share.LinkSharing
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.util.system.toShareIntent
import eu.kanade.tachiyomi.util.system.toast

/** Offers the link as plain text through the system share sheet, and toasts why when the sheet can't open. */
@Inject
class AndroidLinkSharing : LinkSharing {
    override val isAvailable: Boolean = true

    override fun share(context: PlatformContext, url: String) {
        try {
            context.startActivity(url.toUri().toShareIntent(context, type = "text/plain"))
        } catch (e: Exception) {
            context.toast(e.message)
        }
    }
}
