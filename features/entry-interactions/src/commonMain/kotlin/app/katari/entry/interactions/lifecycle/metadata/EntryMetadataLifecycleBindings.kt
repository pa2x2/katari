package app.katari.entry.interactions.lifecycle.metadata

import app.katari.entry.interactions.download.EntryDownloadMaintenanceFeature
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(AppScope::class)
@BindingContainer
object EntryMetadataLifecycleBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun feature(downloads: () -> EntryDownloadMaintenanceFeature): EntryMetadataLifecycleFeature =
        EntryMetadataLifecycleCoordinator(downloads)
}
