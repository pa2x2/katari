package app.katari.extension.compat.webkit

import android.webkit.WebView
import app.katari.core.common.web.ExtensionPage
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.lang.ref.Cleaner
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeSource

/**
 * Closes the page of a web view its extension forgot, one no longer referenced that was never destroyed, so it doesn't
 * keep a Chromium renderer for the rest of the run. Android frees such a view's page once the view is collected; here
 * the view's [Cleaner] closes it, which needs nothing of the page to refer to the view, as the page's events refer to
 * the view's page only weakly ([WebViewPage]).
 *
 * The collector may leave an unreferenced view alone for as long as the heap has room, so a page nothing has used for
 * [IDLE] asks for one collection: an unreferenced view then goes, and its page closes within about [IDLE] and the
 * [check interval][CHECK_INTERVAL]. A page still in use is asked for again only once used and left idle once more.
 */
internal object ForgottenPages {
    private val cleaner = Cleaner.create()
    private val pages = ConcurrentHashMap<ExtensionPage, Activity>()
    private val checks by lazy {
        Executors.newSingleThreadScheduledExecutor { task ->
            Thread(task, "Forgotten web views").apply { isDaemon = true }
        }.also { it.scheduleWithFixedDelay(::check, CHECK_INTERVAL, CHECK_INTERVAL, TimeUnit.MILLISECONDS) }
    }

    /** Closes [page] once [view] is collected, unless it was [closed][untrack] before. */
    fun track(view: WebView, page: ExtensionPage): Activity {
        val activity = Activity()
        pages[page] = activity
        cleaner.register(view, Forgotten(page))
        checks
        return activity
    }

    /** [page] was closed as its view was destroyed or its renderer went. */
    fun untrack(page: ExtensionPage) {
        pages.remove(page)
    }

    private fun check() {
        val idle = pages.values.filter { it.isIdle() }
        if (idle.isEmpty()) return
        idle.forEach(Activity::asked)
        logcat(LogPriority.DEBUG) { "${idle.size} web view pages idle for $IDLE; collecting forgotten views" }
        System.gc()
    }

    /** When a web view or its page last did anything, as its calls and callbacks [tell][used]. */
    class Activity {
        @Volatile
        private var lastUse = TimeSource.Monotonic.markNow()

        @Volatile
        private var asked = false

        fun used() {
            lastUse = TimeSource.Monotonic.markNow()
            asked = false
        }

        internal fun isIdle(): Boolean = !asked && lastUse.elapsedNow() >= IDLE

        internal fun asked() {
            asked = true
        }
    }

    // Refers to the page only: a reference to the view would keep it from being collected
    private class Forgotten(private val page: ExtensionPage) : Runnable {
        override fun run() {
            if (pages.remove(page) == null) return
            logcat(LogPriority.INFO) { "Closing the page of a web view its extension forgot" }
            page.close()
        }
    }

    private val IDLE = 2.minutes
    private val CHECK_INTERVAL = 1.minutes.inWholeMilliseconds
}
