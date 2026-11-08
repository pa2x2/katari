package app.katari.desktop.presence

import kotlinx.coroutines.flow.Flow

/** Whether the session's screen is locked or blanked, so the user is away from the app. */
interface ScreenLock {
    val isLocked: Flow<Boolean>
}
