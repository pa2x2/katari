package app.katari.profiles.ui

import app.katari.core.common.platform.PlatformFacility
import app.katari.core.platform.PlatformContext

/**
 * Asking the user to authenticate before a locked profile opens. Where the platform cannot ask, profiles cannot be
 * locked: the lock options are hidden, or replaced by [lockSettingsWording]'s note, and a profile that is locked
 * anyway, such as one restored from a backup, stays shut.
 */
@PlatformFacility
interface ProfileUnlock {
    /** Whether [context]'s device can ask the user to authenticate. */
    fun isAvailable(context: PlatformContext): Boolean

    /**
     * What the lock settings say about this platform's unlock, where their titles alone don't tell; null where users
     * know the unlock already, as a phone's own lock screen.
     */
    val lockSettingsWording: ProfileLockWording?

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
