package mihon.app.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.presentation.more.settings.screen.animeplayer.MpvPlayerEngineAddon
import eu.kanade.presentation.more.settings.screen.animeplayer.PlayerEngineAddon
import eu.kanade.presentation.more.settings.screen.viewer.AndroidViewerDeviceControls
import mihon.entry.viewer.settings.ui.device.ViewerDeviceControls

@BindingContainer
interface ViewerSettingsBindings {
    @Binds
    val AndroidViewerDeviceControls.viewerDeviceControls: ViewerDeviceControls

    @Binds
    val MpvPlayerEngineAddon.playerEngineAddon: PlayerEngineAddon
}
