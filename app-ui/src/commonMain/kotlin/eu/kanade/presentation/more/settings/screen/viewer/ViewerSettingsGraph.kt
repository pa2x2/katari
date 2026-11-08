package eu.kanade.presentation.more.settings.screen.viewer

import app.katari.entry.interactions.media.EntryViewerSettingsFeature
import app.katari.entry.viewer.settings.ViewerSettingBinder
import app.katari.entry.viewer.settings.shared.ReaderSharedSettingsRegistry
import app.katari.entry.viewer.settings.ui.device.ViewerDeviceControls

/**
 * Graph entry points of the Readers and Players settings and the settings screens of each viewer. A viewer's own
 * settings come with its destination in [entryViewerSettingsFeature], from the entry type that ships the viewer.
 */
interface ViewerSettingsGraph {
    val entryViewerSettingsFeature: EntryViewerSettingsFeature
    val readerSharedSettingsRegistry: ReaderSharedSettingsRegistry
    val viewerSettingBinder: ViewerSettingBinder
    val viewerDeviceControls: ViewerDeviceControls
}
