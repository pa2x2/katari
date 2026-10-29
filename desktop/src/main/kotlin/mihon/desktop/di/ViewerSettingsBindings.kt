package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.presentation.more.settings.screen.animeplayer.PlayerEngineAddon
import eu.kanade.presentation.more.settings.screen.viewer.ViewerDeviceControls
import mihon.desktop.viewer.DesktopViewerDeviceControls
import mihon.desktop.viewer.NoPlayerEngineAddon

@BindingContainer
interface ViewerSettingsBindings {
    @Binds
    val DesktopViewerDeviceControls.viewerDeviceControls: ViewerDeviceControls

    @Binds
    val NoPlayerEngineAddon.playerEngineAddon: PlayerEngineAddon
}
