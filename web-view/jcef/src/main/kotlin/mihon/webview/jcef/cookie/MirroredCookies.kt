package mihon.webview.jcef.cookie

import mihon.core.common.network.cookie.CookieChange
import mihon.core.common.network.cookie.DesktopCookieStore
import okhttp3.Cookie

/**
 * What the mirror knows of Chromium's cookies, which decides the direction of each change: every cookie it set in
 * Chromium or took from there, with the store's cookie it stands for and whether a read has seen Chromium hold it.
 *
 * - A store change reaches Chromium unless Chromium already holds that cookie, as it does for one taken from there, so
 *   nothing echoes back and Chromium keeps the `SameSite` the store never learnt.
 * - A read tells a cookie Chromium changed (a page set or deleted it, or it expired) from one Chromium was set and still
 *   holds, which the store keeps as it is, attributes JCEF can't carry included. Chromium's change reaches the store
 *   only while the store still holds what the mirror last knew; a newer store change is on its way to Chromium and wins.
 * - A cookie Chromium has never been seen to hold was refused by it, as Chromium is stricter than OkHttp (prefixes,
 *   public suffixes); the store keeps it.
 */
internal class MirroredCookies {
    private val entries = HashMap<ChromiumCookie.Key, Entry>()

    /** Passes [change] on to [chromium]; whether anything was set there, which a read then confirms. */
    fun storeChanged(change: CookieChange, chromium: ChromiumCookieManager): Boolean = when (change) {
        is CookieChange.Stored -> stored(change.cookie, chromium)
        is CookieChange.Removed -> {
            val entry = entries.remove(change.cookie.toChromiumCookie().key)
            if (entry != null && entry.state != State.REFUSED) chromium.remove(entry.chromium)
            false
        }
        CookieChange.Cleared -> {
            entries.clear()
            chromium.removeAll()
            false
        }
    }

    /** Brings what Chromium changed since the last read into [store], from [seen], every cookie it holds now. */
    fun read(seen: List<ChromiumCookie>, store: DesktopCookieStore) {
        val held = seen.associateBy { it.key }
        entries.toList().forEach { (key, entry) ->
            val now = held[key]
            when {
                now == null -> when (entry.state) {
                    State.SET -> entries[key] = entry.copy(state = State.REFUSED)
                    State.HELD -> {
                        store.replaceIfUnchanged(entry.stored, null)
                        entries.remove(key)
                    }
                    State.REFUSED -> Unit
                }
                entry.state != State.REFUSED && entry.chromium.isHeldAs(now) ->
                    entries[key] = entry.copy(chromium = now, state = State.HELD)
                else -> takeFromChromium(now, current = entry.stored, store)
            }
        }
        seen.filter { it.key !in entries }.forEach { takeFromChromium(it, current = null, store) }
    }

    private fun stored(cookie: Cookie, chromium: ChromiumCookieManager): Boolean {
        val target = cookie.toChromiumCookie()
        if (entries[target.key]?.stored == cookie) return false
        // The store keeps one cookie per name, domain and path, Chromium one each for host-only and domain cookies
        entries.values
            .filter { it.stored.isSameAs(cookie) && it.chromium.key != target.key }
            .forEach { replaced ->
                entries.remove(replaced.chromium.key)
                if (replaced.state != State.REFUSED) chromium.remove(replaced.chromium)
            }
        chromium.set(target)
        entries[target.key] = Entry(cookie, target, State.SET)
        return true
    }

    /** Takes [seen] into the store in place of [current], the store's cookie the mirror last knew under its key. */
    private fun takeFromChromium(seen: ChromiumCookie, current: Cookie?, store: DesktopCookieStore) {
        val cookie = seen.toCookie()
        val stored = when {
            cookie != null && store.replaceIfUnchanged(current, cookie) -> cookie
            // The store changed since: its change is on its way to Chromium and replaces this cookie there. A cookie
            // OkHttp can't hold stays in Chromium only.
            else -> current ?: cookie ?: return
        }
        entries[seen.key] = Entry(stored, seen, State.HELD)
    }

    private fun Cookie.isSameAs(other: Cookie) = name == other.name && domain == other.domain && path == other.path

    private data class Entry(val stored: Cookie, val chromium: ChromiumCookie, val state: State)

    private enum class State {
        /** Set in Chromium; no read since. */
        SET,

        /** Seen in Chromium by a read. */
        HELD,

        /** Missing from the first read after it was set. */
        REFUSED,
    }
}
