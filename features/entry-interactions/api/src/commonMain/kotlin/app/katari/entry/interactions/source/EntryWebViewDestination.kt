package app.katari.entry.interactions.source

import app.katari.core.common.platform.PlatformFacility
import app.katari.core.platform.PlatformContext

/**
 * The app's web view as readers and players open it: over the screen the call comes from, which isn't one of the
 * screens the app's own navigation shows.
 */
@PlatformFacility
interface EntryWebViewDestination {
    /** False where the app has no web view; what would open one is then hidden. */
    val isAvailable: Boolean

    /**
     * Opens [url] from [context], with [sourceId]'s web view headers and site storage when given. Only called while
     * [isAvailable]; throws when the web view can't be opened.
     */
    fun open(context: PlatformContext, url: String, title: String?, sourceId: Long?)
}
