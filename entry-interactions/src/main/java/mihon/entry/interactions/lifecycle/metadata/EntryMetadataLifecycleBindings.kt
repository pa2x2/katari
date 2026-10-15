package mihon.entry.interactions.lifecycle.metadata

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import mihon.entry.interactions.download.EntryDownloadMaintenanceFeature

@ContributesTo(AppScope::class)
@BindingContainer
object EntryMetadataLifecycleBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun feature(downloads: () -> EntryDownloadMaintenanceFeature): EntryMetadataLifecycleFeature =
        EntryMetadataLifecycleCoordinator(downloads)
}
