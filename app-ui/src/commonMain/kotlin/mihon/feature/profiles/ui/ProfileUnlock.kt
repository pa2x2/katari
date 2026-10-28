package mihon.feature.profiles.ui

import mihon.core.platform.PlatformContext

/**
 * Asking the user to authenticate before a locked profile opens. Where the platform cannot ask, profiles cannot be
 * locked: the lock options are hidden, and a profile that is locked anyway, such as one restored from a backup, stays
 * shut.
 */
interface ProfileUnlock {
    /** Whether [context]'s device can ask the user to authenticate. */
    fun isAvailable(context: PlatformContext): Boolean

    /**
     * Asks once to open the profiles named in [profileNames]; every profile shares that unlock. False when the user
     * did not authenticate or could not be asked.
     */
    suspend fun unlock(context: PlatformContext, profileNames: List<String>): Boolean

    /**
     * Asks before entries are moved into the locked profile [profileName]. False when the user did not authenticate or
     * could not be asked.
     */
    suspend fun unlockToMoveEntries(context: PlatformContext, profileName: String): Boolean
}
