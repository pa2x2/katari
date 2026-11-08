package app.katari.desktop.di

import app.katari.desktop.viewer.DesktopViewerDeviceControls
import app.katari.desktop.viewer.NoPlayerEngineAddon
import app.katari.entry.viewer.settings.ui.device.ViewerDeviceControls
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.presentation.more.settings.screen.animeplayer.PlayerEngineAddon

@BindingContainer
interface ViewerSettingsBindings {
    @Binds
    val DesktopViewerDeviceControls.viewerDeviceControls: ViewerDeviceControls

    @Binds
    val NoPlayerEngineAddon.playerEngineAddon: PlayerEngineAddon
}
