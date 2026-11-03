package eu.kanade.tachiyomi.ui.security

import mihon.core.common.platform.PlatformFacility
import mihon.core.platform.PlatformContext

/**
 * Asks the user to authenticate before a setting that guards the app changes, such as loosening which content shows or
 * changing the app lock.
 */
@PlatformFacility
interface SettingChangeAuthentication {
    /**
     * Asks with [title] from [context]'s screen. True when the user authenticated, and where the device can't ask: the
     * change then goes through unasked.
     */
    suspend fun authenticate(context: PlatformContext, title: String): Boolean
}
