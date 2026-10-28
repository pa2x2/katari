package mihon.desktop.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.presentation.more.settings.screen.animeplayer.PlayerEngineAddon
import eu.kanade.presentation.more.settings.screen.viewer.ViewerDeviceControls
import mihon.desktop.viewer.DesktopViewerDeviceControls
import mihon.desktop.viewer.NoPlayerEngineAddon
import mihon.desktop.viewer.NoViewerSettingsScreenProjections
import mihon.entry.interactions.media.EntryViewerSettingsScreenProjectionResolver
import mihon.entry.viewer.settings.shared.ReaderSharedSettingsRegistry

@BindingContainer
interface ViewerSettingsBindings {
    @Binds
    val DesktopViewerDeviceControls.viewerDeviceControls: ViewerDeviceControls

    @Binds
    val NoPlayerEngineAddon.playerEngineAddon: PlayerEngineAddon

    @Binds
    val NoViewerSettingsScreenProjections.viewerSettingsScreenProjectionResolver:
        EntryViewerSettingsScreenProjectionResolver

    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun readerSharedSettingsRegistry(): ReaderSharedSettingsRegistry = ReaderSharedSettingsRegistry(emptyList())
    }
}
