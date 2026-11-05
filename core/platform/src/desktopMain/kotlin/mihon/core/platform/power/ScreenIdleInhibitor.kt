package mihon.core.platform.power

import mihon.core.common.platform.PlatformFacility
import mihon.core.platform.PlatformContext

/**
 * Keeps the desktop's screen from blanking or locking while nobody touches the keyboard or mouse, as a video playing
 * in a visible window needs. Where the desktop offers no way to, the screen locks as it otherwise would.
 */
@PlatformFacility
interface ScreenIdleInhibitor {
    /**
     * Keeps the screen awake for [window] until the returned hold is closed. [reason] is what desktops that list who
     * keeps the screen awake show. Returns at once; closing a hold more than once does nothing.
     */
    fun keepAwake(window: PlatformContext, reason: String): AutoCloseable
}
