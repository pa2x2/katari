package eu.kanade.presentation.more.settings.screen.viewer

import mihon.entry.interactions.media.EntryViewerSettingsFeature
import mihon.entry.viewer.settings.ViewerSettingBinder
import mihon.entry.viewer.settings.shared.ReaderSharedSettingsRegistry

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
