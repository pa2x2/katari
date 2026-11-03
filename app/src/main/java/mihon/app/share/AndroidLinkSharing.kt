package mihon.app.share

import androidx.core.net.toUri
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.util.system.toShareIntent
import eu.kanade.tachiyomi.util.system.toast
import mihon.core.platform.PlatformContext
import mihon.core.platform.share.LinkSharing

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
