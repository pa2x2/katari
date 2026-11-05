package app.katari.sync.server.attemptlimit

import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * Slows down guessing of secrets people type: passwords, link codes and the setup code. Each key (an account, a
 * remote address) gets [freeFailures] failed attempts; every failure after that blocks the key for [firstBlock],
 * doubling each time up to [longestBlock], and a success clears it, as does a day without failures. A blocked attempt
 * is refused before its secret is checked, so guessing also costs the server no hashing.
 *
 * State is in memory: a restart clears it, which gives an attacker a handful of guesses per restart at most.
 */
class AttemptLimiter(
    private val clock: Clock,
    private val freeFailures: Int = 5,
    private val firstBlock: Duration = 1.minutes,
    private val longestBlock: Duration = 1.hours,
) {
    private class Failures(var count: Int, var blockedUntil: Instant, var last: Instant)

    private val failures = HashMap<String, Failures>()

    /** How long until any of [keys] may try again, or null when none is blocked. */
    @Synchronized
    fun blockedFor(keys: Collection<String>): Duration? {
        val now = clock.now()
        return keys.mapNotNull { failures[it]?.blockedUntil?.minus(now) }
            .filter { it.isPositive() }
            .maxOrNull()
    }

    @Synchronized
    fun failed(keys: Collection<String>) {
        val now = clock.now()
        if (failures.size >= MAX_TRACKED_KEYS) forgetIdle(now)
        keys.forEach { key ->
            val entry = failures.getOrPut(key) { Failures(count = 0, blockedUntil = now, last = now) }
            if (now - entry.last > FORGET_AFTER) entry.count = 0
            entry.count++
            entry.last = now
            val beyondFree = entry.count - freeFailures
            if (beyondFree > 0) {
                val block = firstBlock * (1 shl (beyondFree - 1).coerceAtMost(MAX_DOUBLINGS))
                entry.blockedUntil = now + minOf(block, longestBlock)
            }
        }
    }

    @Synchronized
    fun succeeded(keys: Collection<String>) {
        keys.forEach(failures::remove)
    }

    // Many addresses guessing would otherwise grow the map without bound.
    private fun forgetIdle(now: Instant) {
        failures.values.removeIf { now - it.last > FORGET_AFTER && now >= it.blockedUntil }
    }

    companion object {
        fun account(username: String): String = "account:${username.trim().lowercase()}"

        fun address(remoteAddress: String): String = "address:$remoteAddress"

        private const val MAX_TRACKED_KEYS = 100_000
        private const val MAX_DOUBLINGS = 20
        private val FORGET_AFTER = 24.hours
    }
}
