package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import mihon.desktop.download.NoEntryDownloadActionFeature
import mihon.desktop.download.NoEntryDownloadLifecycleEventSink
import mihon.desktop.download.NoEntryDownloadMaintenanceFeature
import mihon.desktop.download.NoEntryDownloadOptionsFeature
import mihon.desktop.download.NoEntryDownloadRuntimeFeature
import mihon.entry.interactions.download.EntryDownloadActionFeature
import mihon.entry.interactions.download.EntryDownloadLifecycleEventSink
import mihon.entry.interactions.download.EntryDownloadMaintenanceFeature
import mihon.entry.interactions.download.EntryDownloadOptionsFeature
import mihon.entry.interactions.download.EntryDownloadRuntimeFeature

@BindingContainer
interface DownloadBindings {
    @Binds
    val NoEntryDownloadRuntimeFeature.entryDownloadRuntimeFeature: EntryDownloadRuntimeFeature

    @Binds
    val NoEntryDownloadActionFeature.entryDownloadActionFeature: EntryDownloadActionFeature

    @Binds
    val NoEntryDownloadLifecycleEventSink.entryDownloadLifecycleEventSink: EntryDownloadLifecycleEventSink

    @Binds
    val NoEntryDownloadMaintenanceFeature.entryDownloadMaintenanceFeature: EntryDownloadMaintenanceFeature

    @Binds
    val NoEntryDownloadOptionsFeature.entryDownloadOptionsFeature: EntryDownloadOptionsFeature
}
