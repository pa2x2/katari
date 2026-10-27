package mihon.desktop.profile

import dev.zacsweers.metro.Inject
import mihon.core.platform.PlatformContext
import mihon.feature.profiles.ui.ProfileUnlock

/**
 * Desktop cannot ask the user to authenticate yet, so profiles cannot be locked there: the lock options are hidden, and
 * a profile locked on Android stays shut instead of opening without its lock. Stands in until desktop authenticates
 * through the system.
 */
@Inject
class NoProfileUnlock : ProfileUnlock {
    override fun isAvailable(context: PlatformContext): Boolean = false

    override suspend fun unlock(context: PlatformContext, profileNames: List<String>): Boolean = profileNames.isEmpty()
}
