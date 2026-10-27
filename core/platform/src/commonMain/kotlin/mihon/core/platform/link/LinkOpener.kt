package mihon.core.platform.link

import mihon.core.platform.PlatformContext

/** Opens web links outside the app, in whatever the user has set to handle them. */
interface LinkOpener {
    /**
     * Opens [url] from [context]. [forceDefaultBrowser] passes over apps that claim the link's site, such as an
     * extension's verified links reopening Katari, on platforms that let apps claim sites. Never throws: the
     * implementation deals with a link that can't be opened.
     */
    fun open(context: PlatformContext, url: String, forceDefaultBrowser: Boolean = false)
}
