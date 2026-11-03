package eu.kanade.tachiyomi.network.interceptor

import okhttp3.HttpUrl
import java.util.concurrent.ConcurrentHashMap

/**
 * Lets one caller at a time solve the challenge of a zone, the site's registrable domain, whose clearance covers every
 * host in it: callers that meet the challenge meanwhile wait, and find the clearance the first one got. A caller that
 * fails or is interrupted lets the next one in.
 *
 * @param onRegistered called once a caller is counted for its zone, before it waits; for tests that order callers.
 */
internal class CloudflareChallengeCoordinator(
    private val zoneFor: (HttpUrl) -> String = { it.topPrivateDomain() ?: it.host },
    private val onRegistered: (HttpUrl) -> Unit = {},
) {
    private val locks = ConcurrentHashMap<String, LockEntry>()

    fun solve(url: HttpUrl, block: () -> Unit) {
        val zone = zoneFor(url)
        val entry = locks.compute(zone) { _, current ->
            (current ?: LockEntry()).also { it.users++ }
        }!!

        try {
            onRegistered(url)
            synchronized(entry.monitor) {
                block()
            }
        } finally {
            locks.computeIfPresent(zone) { _, current ->
                check(current === entry)
                current.users--
                current.takeIf { it.users > 0 }
            }
        }
    }

    private class LockEntry(
        val monitor: Any = Any(),
        var users: Int = 0,
    )
}
