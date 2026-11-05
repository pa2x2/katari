package eu.kanade.presentation.more.settings.screen.viewer

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.util.system.hasDisplayCutout

/** Android offers every control on every device; only the display cutout depends on the window's screen. */
@Inject
class AndroidViewerDeviceControls : ViewerDeviceControls {
    override val volumeKeys: Boolean = true
    override val systemBars: Boolean = true
    override val screenOrientation: Boolean = true
    override val eInkFlashes: Boolean = true
    override val pictureInPicture: Boolean = true
    override val backgroundPlayback: Boolean = true
    override val screenBrightness: Boolean = true

    @Composable
    override fun hasDisplayCutout(): Boolean = LocalView.current.hasDisplayCutout()
}
