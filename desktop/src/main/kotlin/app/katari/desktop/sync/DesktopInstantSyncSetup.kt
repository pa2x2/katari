package app.katari.desktop.sync

import androidx.compose.runtime.Composable
import app.katari.sync.ui.instant.InstantSyncSetup
import dev.zacsweers.metro.Inject

/** The app stays connected for as long as it runs in the tray, so there is nothing to turn on. */
@Inject
class DesktopInstantSyncSetup : InstantSyncSetup {
    override val isOffered: Boolean = false

    @Composable
    override fun Setup(onTurnOn: () -> Unit, onClose: () -> Unit) = Unit
}
