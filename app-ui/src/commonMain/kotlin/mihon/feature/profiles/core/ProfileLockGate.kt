package mihon.feature.profiles.core

import eu.kanade.tachiyomi.core.security.SecurityPreferences
import java.util.concurrent.ConcurrentHashMap

/**
 * Decides whether entering a profile must authenticate, honoring that profile's "Lock when idle" delay.
 *
 * A locked profile re-arms when it stops being the active profile, so switching away and back within the
 * configured delay reuses the authentication of the session the profile was left in. Only profiles that
 * proved presence through authentication in this process get an idle clock, and the clocks live in memory
 * only, so a process start leaves every profile locked - matching the app-level rule that a killed app
 * always requires unlock.
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
        return when (val lockDelay = security.lockAppAfter.get()) {
            LOCK_NEVER -> false
            LOCK_ALWAYS -> true
            else -> {
                // Without a recorded deactivation the profile was never unlocked in this process; fail closed.
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
        const val LOCK_ALWAYS = 0
        const val LOCK_NEVER = -1
        const val MINUTE_MILLIS = 60_000L
    }
}
