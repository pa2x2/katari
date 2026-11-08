package app.katari.profiles.core

import eu.kanade.tachiyomi.core.security.SecurityPreferences
import java.util.concurrent.ConcurrentHashMap

/**
 * Decides whether entering a profile must authenticate, honoring that profile's "Lock when idle" delay.
 *
 * A locked profile asks for its unlock the first time it opens in each process, whatever its delay; "Never" only keeps
 * it unlocked for the rest of the process after that. An unlocked profile re-arms when it stops being the active
 * profile, so switching away and back within the configured delay reuses the authentication of the session the
 * profile was left in. Authentications and idle clocks live in memory only, so a process start leaves every profile
 * locked - matching the app-level rule that a killed app always requires unlock.
 */
internal class ProfileLockGate(
    private val profileStore: ProfileStore,
    private val now: () -> Long = System::currentTimeMillis,
) {

    private val authenticated = ConcurrentHashMap.newKeySet<Long>()
    private val inactiveSince = ConcurrentHashMap<Long, Long>()

    fun requiresAuthNow(profileId: Long): Boolean {
        if (profileId == profileStore.currentProfileId) return false
        val security = SecurityPreferences(profileStore.profileStore(profileId))
        if (!security.useAuthenticator.get()) return false
        if (profileId !in authenticated) return true
        return when (val lockDelay = security.lockAppAfter.get()) {
            SecurityPreferences.LOCK_NEVER -> false
            SecurityPreferences.LOCK_ALWAYS -> true
            else -> {
                // Unlocked but never left, as when the switch that followed the unlock didn't happen; fail closed.
                val deactivatedAt = inactiveSince[profileId] ?: return true
                now() - deactivatedAt >= lockDelay * MINUTE_MILLIS
            }
        }
    }

    fun markAuthenticated(profileId: Long) {
        authenticated += profileId
    }

    fun markInactive(profileId: Long) {
        if (profileId in authenticated) {
            inactiveSince[profileId] = now()
        }
    }

    private companion object {
        const val MINUTE_MILLIS = 60_000L
    }
}
