package mihon.core.common.network.cookie

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import okhttp3.Cookie
import okhttp3.HttpUrl
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Cookies as Android's WebView keeps them for the app: in memory for the requests that read them, and in a file in
 * [directory] that every cookie which hasn't expired survives a restart in, session cookies too, since WebView persists
 * and restores those as well. Expired cookies are dropped as they are read. A change is visible at once and written in
 * the background, so a request never waits on the disk; writes still pending when the JVM exits are written by a
 * shutdown hook, since the background writer runs on daemon threads that do not hold the exit back.
 *
 * A cookie replaces the one with its name, domain and path, as RFC 6265 has a user agent do. A process has one store
 * on the file, [AppCookieStore]'s, since two would overwrite each other's changes.
 */
class DesktopCookieStore(directory: Path) {
    private val file = CookieFile(directory.resolve(FILE_NAME))

    // Guards itself, and the order of changeEvents, which are emitted while it is held
    private val cookies: MutableList<Cookie> = file.read().filterNot { it.hasExpired() }.toMutableList()
    private val unwritten = AtomicBoolean(false)
    private val writeRequests = Channel<Unit>(Channel.CONFLATED)
    private val writeLock = Any()

    // Unbounded, so emitting never waits for a slow listener and never drops a change
    private val changeEvents = MutableSharedFlow<CookieChange>(extraBufferCapacity = Channel.UNLIMITED)

    /**
     * Every change from the moment a collector subscribes, in the order the store made them, delivered on the
     * collector's own dispatcher, so neither a writer nor a request ever waits for a listener. A listener that keeps a
     * copy reads [all] once subscribed (`onSubscription`) and applies the changes after it: a change it receives may
     * already be in what it read, and applying the changes in order still ends with the store's cookies.
     *
     * The desktop web view's cookie mirror (`:sources:web-view:jcef`) listens, to keep Chromium's cookies in step with
     * the store.
     */
    val changes: SharedFlow<CookieChange> = changeEvents.asSharedFlow()

    init {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            writeRequests.consumeEach { flush() }
        }
        Runtime.getRuntime().addShutdownHook(Thread({ flush() }, "Cookie writer"))
    }

    fun save(saved: List<Cookie>) {
        val changed = synchronized(cookies) {
            saved.fold(false) { changed, cookie -> put(cookie) or changed }
        }
        if (changed) requestWrite()
    }

    fun matching(url: HttpUrl): List<Cookie> = synchronized(cookies) {
        dropExpired()
        cookies.filter { it.matches(url) }
    }

    fun all(): List<Cookie> = synchronized(cookies) {
        dropExpired()
        cookies.toList()
    }

    /** Removes the cookies [url] gets, or only those named in [names]; returns how many. */
    fun remove(url: HttpUrl, names: List<String>?): Int =
        removeWhere { it.matches(url) && (names == null || it.name in names) }

    /** Removes the cookies without an expiry; whether there were any. */
    fun removeSessionCookies(): Boolean = removeWhere { !it.persistent } > 0

    /** Removes every cookie; whether there were any. Listeners hear [CookieChange.Cleared] either way. */
    fun removeAll(): Boolean {
        val removed = synchronized(cookies) {
            val any = cookies.isNotEmpty()
            cookies.clear()
            changeEvents.tryEmit(CookieChange.Cleared)
            any
        }
        requestWrite()
        return removed
    }

    /**
     * Replaces [current] with [replacement], or removes it where [replacement] is null, only while the store still holds
     * [current] under that name, domain and path (null: no cookie there); whether the store holds [replacement] now.
     * For a copy of the store that changes on its own, Chromium's: a change it reports late never overwrites a newer
     * one of the store's, whose [changes] event brings the copy up to date instead.
     */
    fun replaceIfUnchanged(current: Cookie?, replacement: Cookie?): Boolean {
        val key = requireNotNull(replacement ?: current) { "Nothing to replace" }
        var changed = false
        val replaced = synchronized(cookies) {
            val held = cookies.firstOrNull { it.isReplacedBy(key) }
            if (held != current) return@synchronized false
            changed = when {
                replacement != null -> put(replacement)
                held != null -> {
                    cookies.remove(held)
                    changeEvents.tryEmit(CookieChange.Removed(held))
                    true
                }
                else -> false
            }
            true
        }
        if (changed) requestWrite()
        return replaced
    }

    /**
     * Writes the current cookies if a change has not reached the file yet, blocking until it has. A change made while
     * a write is under way requests another.
     */
    fun flush() {
        synchronized(writeLock) {
            if (!unwritten.getAndSet(false)) return
            val current = synchronized(cookies) { cookies.filterNot { it.hasExpired() } }
            if (!file.write(current)) unwritten.set(true)
        }
    }

    /** Puts [cookie] in place of the one with its name, domain and path, while [cookies] is held; whether it changed. */
    private fun put(cookie: Cookie): Boolean {
        val index = cookies.indexOfFirst { it.isReplacedBy(cookie) }
        val previous = if (index >= 0) cookies.removeAt(index) else null
        // An expired cookie only removes the one it replaces, which is how a server deletes a cookie
        val kept = cookie.takeUnless { it.hasExpired() }
        if (kept != null) cookies += kept
        if (previous == kept) return false
        val change = if (kept != null) CookieChange.Stored(kept) else CookieChange.Removed(checkNotNull(previous))
        changeEvents.tryEmit(change)
        return true
    }

    private fun removeWhere(predicate: (Cookie) -> Boolean): Int {
        val removed = synchronized(cookies) {
            cookies.filter(predicate).onEach { cookie ->
                cookies.remove(cookie)
                changeEvents.tryEmit(CookieChange.Removed(cookie))
            }
        }
        if (removed.isNotEmpty()) requestWrite()
        return removed.size
    }

    /** Drops the cookies that have expired, while [cookies] is held. */
    private fun dropExpired() {
        cookies.filter { it.hasExpired() }.forEach { cookie ->
            cookies.remove(cookie)
            changeEvents.tryEmit(CookieChange.Removed(cookie))
        }
    }

    private fun requestWrite() {
        unwritten.set(true)
        writeRequests.trySend(Unit)
    }

    private fun Cookie.isReplacedBy(other: Cookie): Boolean =
        name == other.name && domain == other.domain && path == other.path

    private fun Cookie.hasExpired(): Boolean = expiresAt <= System.currentTimeMillis()

    private companion object {
        const val FILE_NAME = "cookies.json"
    }
}
