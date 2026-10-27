package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import mihon.desktop.download.NoEntryDownloadActionFeature
import mihon.desktop.download.NoEntryDownloadLifecycleEventSink
import mihon.desktop.download.NoEntryDownloadRuntimeFeature
import mihon.entry.interactions.download.EntryDownloadActionFeature
import mihon.entry.interactions.download.EntryDownloadLifecycleEventSink
import mihon.entry.interactions.download.EntryDownloadRuntimeFeature

@BindingContainer
interface DownloadBindings {
    @Binds
    val NoEntryDownloadRuntimeFeature.entryDownloadRuntimeFeature: EntryDownloadRuntimeFeature

    @Binds
    val NoEntryDownloadActionFeature.entryDownloadActionFeature: EntryDownloadActionFeature

    @Binds
    val NoEntryDownloadLifecycleEventSink.entryDownloadLifecycleEventSink: EntryDownloadLifecycleEventSink
}
