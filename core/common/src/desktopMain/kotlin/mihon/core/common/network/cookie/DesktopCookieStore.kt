package mihon.core.common.network.cookie

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
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
internal class DesktopCookieStore(directory: Path) {
    private val file = CookieFile(directory.resolve(FILE_NAME))
    private val cookies: MutableList<Cookie> = file.read().filterNot { it.hasExpired() }.toMutableList()
    private val unwritten = AtomicBoolean(false)
    private val writeRequests = Channel<Unit>(Channel.CONFLATED)
    private val writeLock = Any()

    init {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            writeRequests.consumeEach { flush() }
        }
        Runtime.getRuntime().addShutdownHook(Thread({ flush() }, "Cookie writer"))
    }

    fun save(saved: List<Cookie>) {
        var changed = false
        synchronized(cookies) {
            saved.forEach { cookie ->
                val index = cookies.indexOfFirst { it.isReplacedBy(cookie) }
                val previous = if (index >= 0) cookies.removeAt(index) else null
                // An expired cookie only removes the one it replaces, which is how a server deletes a cookie
                val kept = cookie.takeUnless { it.hasExpired() }
                if (kept != null) cookies += kept
                if (previous != kept) changed = true
            }
        }
        if (changed) requestWrite()
    }

    fun matching(url: HttpUrl): List<Cookie> = synchronized(cookies) {
        cookies.removeAll { it.hasExpired() }
        cookies.filter { it.matches(url) }
    }

    /** Removes the cookies [url] gets, or only those named in [names]; returns how many. */
    fun remove(url: HttpUrl, names: List<String>?): Int {
        val removed = synchronized(cookies) {
            val removed = cookies.filter { it.matches(url) && (names == null || it.name in names) }
            cookies.removeAll(removed)
            removed.size
        }
        if (removed > 0) requestWrite()
        return removed
    }

    fun removeAll() {
        synchronized(cookies) { cookies.clear() }
        requestWrite()
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
