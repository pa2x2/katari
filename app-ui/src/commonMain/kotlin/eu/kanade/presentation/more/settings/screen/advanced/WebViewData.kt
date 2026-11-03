package eu.kanade.presentation.more.settings.screen.advanced

import mihon.core.common.platform.PlatformFacility
import mihon.core.platform.PlatformContext

/**
 * What the system web view keeps for the app, such as its cache, form data and site storage. Where the platform has no
 * web view, the rows for it are hidden.
 */
@PlatformFacility
interface WebViewData {
    val isOffered: Boolean

    /**
     * Clears the web view's caches, form data, history, SSL choices and site storage. Throws when the web view can't
     * be created.
     */
    fun clearCaches(context: PlatformContext)

    /** Deletes the files the web view keeps in the app's data. */
    suspend fun deleteStorage(context: PlatformContext)

    /** The installed web view's version. */
    suspend fun version(context: PlatformContext): String
}
