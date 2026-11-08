package app.katari.desktop.profile

import app.katari.profiles.core.ProfileManager
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Whether the open profile is held locked, as Android's app lock holds it: from the app's start until a profile
 * opens, and again once the open profile has been left alone past its "Lock when idle" time. While it is held and the
 * profile asks for an unlock, the window's profile gate covers the profile's screens.
 */
@SingleIn(AppScope::class)
@Inject
class ActiveProfileLock(private val profileManager: ProfileManager) {
    private val held = MutableStateFlow(true)

    val isHeld: StateFlow<Boolean> = held.asStateFlow()

    /**
     * Whether opening [profileId] must ask for the unlock now: the open profile while it is held, any other once its
     * own "Lock when idle" time has run out, as the profile switch decides it.
     */
    fun requiresUnlock(profileId: Long): Boolean = if (profileId == profileManager.activeProfileId) {
        held.value && profileManager.profileRequiresUnlock(profileId)
    } else {
        profileManager.profileRequiresAuthNow(profileId)
    }

    fun release() {
        held.value = false
    }

    fun hold() {
        held.value = true
    }
}
