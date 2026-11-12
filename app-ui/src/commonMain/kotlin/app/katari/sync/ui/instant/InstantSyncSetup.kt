package app.katari.sync.ui.instant

import androidx.compose.runtime.Composable
import app.katari.core.common.platform.PlatformFacility

/**
 * Turning instant sync on ([app.katari.sync.client.preference.SyncPreferences.instant]) where the platform has to keep
 * the app running for it ([app.katari.sync.client.trigger.SyncBackgroundWork.keepConnected]): on Android, what that
 * costs, the exemption from battery optimisation, and the offer to hide the notification it runs under. A desktop app
 * keeps running in the tray and offers no instant sync of its own.
 */
@PlatformFacility
interface InstantSyncSetup {
    val isOffered: Boolean

    /** Asks the person through turning it on: [onTurnOn] once they agree, [onClose] when the setup is over. */
    @Composable
    fun Setup(onTurnOn: () -> Unit, onClose: () -> Unit)
}
