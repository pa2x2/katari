package app.katari.sync.server.attemptlimit

import java.net.InetAddress
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * Slows down guessing of secrets people type: passwords, and the setup, link and invite codes. Each key (an account,
 * a remote address) gets [freeFailures] failed attempts; every failure after that blocks the key for [firstBlock],
 * doubling each time up to [longestBlock]. A success clears it unless it is a code key ([codesFrom]), as does a day
 * without failures. A blocked attempt
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

        /** Password guesses from the address. */
        fun passwordsFrom(remoteAddress: String): String = "passwords:${addressGroup(remoteAddress)}"

        /**
         * Code guesses from the address. A code names no account until it is right, so this is their only limit, and
         * callers never clear it on success: someone who can make a link code, or holds one invite they keep from
         * being used up, could otherwise reset it between guesses. Passwords have their own key for the same reason,
         * since any account holder can sign in successfully at will.
         */
        fun codesFrom(remoteAddress: String): String = "codes:${addressGroup(remoteAddress)}"

        // Whoever has one IPv6 address usually has its whole /64, so the /64 counts as one address.
        private fun addressGroup(remoteAddress: String): String {
            val literal = remoteAddress.removePrefix("[").substringBefore(']').substringBefore('%')
            // A name with a colon is never a host name, so this never looks anything up.
            if (':' !in literal ||
                !literal.all { it == ':' || it == '.' || it.isDigit() || it.lowercaseChar() in 'a'..'f' }
            ) {
                return remoteAddress
            }
            val bytes = runCatching { InetAddress.getByName(literal).address }.getOrNull()
            if (bytes == null || bytes.size != IPV6_BYTES) return remoteAddress
            return bytes.copyOf(IPV6_BYTES / 2).toHexString() + "::/64"
        }

        private const val IPV6_BYTES = 16

        private const val MAX_TRACKED_KEYS = 100_000
        private const val MAX_DOUBLINGS = 20
        private val FORGET_AFTER = 24.hours
    }
}
