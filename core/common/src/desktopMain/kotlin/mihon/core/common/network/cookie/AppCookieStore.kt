package mihon.core.common.network.cookie

import java.nio.file.Path

/**
 * The app's cookies, one store for the process, as Android's `CookieManager.getInstance()` is: every
 * [eu.kanade.tachiyomi.network.AndroidCookieJar], the HTTP clients' and any an extension builds itself, reads and
 * writes it, extensions' `android.webkit.CookieManager` is over it, and Chromium's cookies are kept in step with it.
 * Start-up opens it before it builds the app's graph, so before anything can build a jar; a jar built earlier fails
 * rather than keep cookies the rest of the app never sees.
 */
object AppCookieStore {
    @Volatile
    private var opened: DesktopCookieStore? = null

    /** Opens the store kept in [directory], the app's own files; once a process. */
    fun open(directory: Path) {
        synchronized(this) {
            check(opened == null) { "The app's cookie store is already open" }
            opened = DesktopCookieStore(directory)
        }
    }

    val store: DesktopCookieStore
        get() = checkNotNull(opened) { "The app's cookie store was used before start-up opened it" }
}
