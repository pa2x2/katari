package mihon.webview.jcef.cookie

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.future.future
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import logcat.LogPriority
import mihon.core.common.network.cookie.CookieChange
import mihon.core.common.network.cookie.DesktopCookieStore
import okhttp3.Cookie
import okhttp3.HttpUrl
import tachiyomi.core.common.util.system.logcat
import java.util.concurrent.CompletableFuture
import kotlin.time.Duration.Companion.seconds

/**
 * Keeps Chromium's cookies in step with the app's cookie [store], which stays the one that counts: requests read the
 * store and never wait for Chromium, and the store is what survives a restart.
 *
 * - As Chromium starts, it loses every cookie its profile kept and gets the store's. CEF has no setting that keeps a
 *   profile's cookies in memory while its cache and site storage stay on disk (an in-memory profile loses those too,
 *   and JCEF reaches no profile's cookies but the global one), so Chromium is emptied as it starts and as it ends: a
 *   cookie the store removed or let expire never comes back from it.
 * - From then on every store change reaches Chromium, in order ([MirroredCookies]).
 * - Chromium's own changes come back on [readBack], since JCEF tells of none.
 */
class ChromiumCookieMirror(private val store: DesktopCookieStore) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Store changes and reads take turns, so a read is compared with what the mirror knew when it began
    private val lock = Mutex()
    private val readRequests = Channel<Unit>(Channel.CONFLATED)

    // Guarded by lock; set while Chromium runs
    private var chromium: ChromiumCookieManager? = null
    private val mirrored = MirroredCookies()
    private var following: Job? = null

    /**
     * Brings the cookies pages set or deleted in Chromium into the store, without waiting. The web view calls it after a
     * page loads, after a frame navigates and as a browser closes; nothing happens while Chromium isn't running.
     */
    fun readBack() {
        readRequests.trySend(Unit)
    }

    /**
     * The cookies a request to [url] carries once Chromium's have been read back, such as the clearance a challenge
     * page left; the store's alone while Chromium isn't running.
     */
    suspend fun cookiesFor(url: HttpUrl): List<Cookie> {
        lock.withLock { read() }
        return store.matching(url)
    }

    /** Hands Chromium the store's cookies and keeps it in step from then on; completes once Chromium has them. */
    internal fun attach(manager: ChromiumCookieManager): CompletableFuture<Unit> = scope.future {
        val seeded = CompletableDeferred<Unit>()
        following = scope.launch {
            store.changes
                .onSubscription {
                    lock.withLock { seed(manager) }
                    seeded.complete(Unit)
                }
                .collect { change ->
                    val set = lock.withLock { chromium?.let { mirrored.storeChanged(change, it) } ?: false }
                    // Whether Chromium took the cookie, and kept it, shows in the next read
                    if (set) readBack()
                }
        }.apply {
            invokeOnCompletion { cause -> if (cause != null) seeded.completeExceptionally(cause) }
        }
        scope.launch {
            readRequests.consumeEach { lock.withLock { read() } }
        }
        seeded.await()
    }

    /**
     * Brings Chromium's last changes into the store, empties Chromium and stops following the store, waiting a bounded
     * time; called as Chromium shuts down.
     */
    internal fun detach() {
        val detached = runBlocking { withTimeoutOrNull(DETACH_TIMEOUT) { lock.withLock { detachChromium() } } }
        if (detached == null) {
            logcat(LogPriority.WARN) { "Chromium's cookies weren't read back and removed in $DETACH_TIMEOUT" }
        }
        // Where the process is ending, the store's shutdown hook may have written its file before this read
        store.flush()
    }

    /**
     * Stops following the store without reading Chromium back, as its server has ended and its cookies with it. The
     * store keeps what Chromium last handed back.
     */
    internal fun forget() {
        scope.launch {
            lock.withLock {
                following?.cancel()
                chromium = null
            }
        }
    }

    private suspend fun detachChromium() {
        val manager = chromium ?: return
        read()
        following?.cancel()
        chromium = null
        manager.removeAll()
        // Until the removal is applied, before Chromium writes its profile and ends, and then the read's own marker
        manager.read()
        manager.removeAll()
    }

    private suspend fun seed(manager: ChromiumCookieManager) {
        chromium = manager
        manager.removeAll()
        store.all().forEach { cookie -> mirrored.storeChanged(CookieChange.Stored(cookie), manager) }
        read()
    }

    private suspend fun read() {
        val manager = chromium ?: return
        val seen = manager.read()
        if (seen == null) {
            logcat(LogPriority.WARN) { "Chromium's cookies couldn't be read" }
            return
        }
        mirrored.read(seen, store)
    }

    private companion object {
        val DETACH_TIMEOUT = 3.seconds
    }
}
