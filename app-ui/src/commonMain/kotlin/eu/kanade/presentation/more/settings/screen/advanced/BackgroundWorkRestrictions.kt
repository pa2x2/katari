package eu.kanade.presentation.more.settings.screen.advanced

import mihon.core.common.platform.PlatformFacility
import mihon.core.platform.PlatformContext

/**
 * The limits the system puts on the app's background work, such as Android's battery optimisation, which can stop
 * library updates and downloads. Where the system puts none, the advanced settings hide the background activity group.
 */
@PlatformFacility
interface BackgroundWorkRestrictions {
    val isOffered: Boolean

    /** Asks the system to stop restricting the app. Only called while [isOffered]. */
    fun requestUnrestricted(context: PlatformContext): UnrestrictedBackgroundWorkRequest
}

enum class UnrestrictedBackgroundWorkRequest {
    /** The system's prompt was opened. */
    Asked,

    /** The app already works unrestricted, so nothing was asked. */
    AlreadyUnrestricted,

    /** The device has no screen to ask on. */
    SettingMissing,
}
