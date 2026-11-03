package mihon.core.common.web

import mihon.core.common.platform.PlatformFacility
import okhttp3.HttpUrl

/**
 * Pages nobody sees, in which a site's challenge, such as Cloudflare's, runs its scripts and leaves the cookie that
 * lets the app's requests through. A page shares the app's cookies both ways: what the app's cookie jar holds when it
 * opens, the page sends, and what the page sets is in the jar by the time it reports a finished load.
 */
@PlatformFacility
interface ChallengePage {
    /** Whether this platform has such pages; where it hasn't, a challenge goes back to its caller unsolved. */
    val isOffered: Boolean

    /**
     * Opens a page that loads [url] with the request [headers], as [userAgent], and tells [events] about it, on threads
     * of its own, until it is closed. Null where the calling thread is the one pages run on, which a caller waiting for
     * the page would stop. Called only where [isOffered].
     */
    fun open(url: HttpUrl, headers: Map<String, String>, userAgent: String, events: ChallengePageEvents): Session?

    /** One open page. */
    fun interface Session {
        /** Closes the page, once the caller stopped waiting with [outcome]. */
        fun close(outcome: ChallengeOutcome)
    }
}

/** How the wait for a page ended. */
enum class ChallengeOutcome {
    /** The page got the clearance. */
    SOLVED,

    /** The page got no clearance: the challenge failed, needs a person, wasn't found, or the time ran out. */
    UNSOLVED,

    /** The waiting thread was interrupted. */
    INTERRUPTED,
}
