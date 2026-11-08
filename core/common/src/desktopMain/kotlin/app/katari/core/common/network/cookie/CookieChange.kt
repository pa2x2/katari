package app.katari.core.common.network.cookie

import okhttp3.Cookie

/** A change to a [DesktopCookieStore]'s cookies, in the order the store made them. */
sealed interface CookieChange {
    /** [cookie] was added, or replaced the cookie with its name, domain and path. */
    data class Stored(val cookie: Cookie) : CookieChange

    /** [cookie] is gone: removed, replaced by an expired cookie, or found expired. */
    data class Removed(val cookie: Cookie) : CookieChange

    /**
     * Every cookie was removed, as "Clear cookies" asks; a copy of the store removes all of its own, also those it
     * hasn't handed to the store yet. Sent even when the store held none.
     */
    data object Cleared : CookieChange
}
