package mihon.app.link

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.util.system.openInBrowser
import mihon.core.platform.PlatformContext
import mihon.core.platform.link.LinkOpener

/** A view intent for the link, started from the caller's context; a failure shows as a toast. */
@Inject
class AndroidLinkOpener : LinkOpener {
    override fun open(context: PlatformContext, url: String, forceDefaultBrowser: Boolean) {
        context.openInBrowser(url, forceDefaultBrowser)
    }
}
