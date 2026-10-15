package mihon.entry.interactions.lifecycle.removal

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import mihon.entry.interactions.download.EntryDownloadMaintenanceFeature
import mihon.entry.interactions.lifecycle.removal.host.EntryDestructiveRemovalCustomCoverHost
import mihon.entry.interactions.lifecycle.removal.host.EntryDestructiveRemovalHost
import mihon.entry.interactions.merge.EntryMergeLibraryLifecycleFeature

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
