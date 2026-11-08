package app.katari.entry.interactions.lifecycle.removal

import app.katari.entry.interactions.download.EntryDownloadMaintenanceFeature
import app.katari.entry.interactions.lifecycle.removal.host.EntryDestructiveRemovalCustomCoverHost
import app.katari.entry.interactions.lifecycle.removal.host.EntryDestructiveRemovalHost
import app.katari.entry.interactions.merge.EntryMergeLibraryLifecycleFeature
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(AppScope::class)
@BindingContainer
object EntryDestructiveRemovalBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun feature(
        destructiveRemovalHost: EntryDestructiveRemovalHost,
        mergeLibraryLifecycle: () -> EntryMergeLibraryLifecycleFeature,
        downloads: () -> EntryDownloadMaintenanceFeature,
        customCoverHost: EntryDestructiveRemovalCustomCoverHost,
    ): EntryDestructiveRemovalFeature = EntryDestructiveRemovalCoordinator(
        host = destructiveRemovalHost,
        mergeLibraryLifecycle = mergeLibraryLifecycle,
        downloads = downloads,
        customCoverHost = customCoverHost,
    )
}
