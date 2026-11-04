package mihon.entry.interactions.download

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import mihon.core.metro.AppCoroutineScope
import mihon.core.metro.StartupWarmup
import mihon.core.platform.PlatformContext
import mihon.entry.interactions.merge.EntryMergeDownloadOwnershipProjection

@ContributesTo(AppScope::class)
@BindingContainer
object EntryDownloadNotificationBindings {
    @Provides
    @SingleIn(AppScope::class)
    internal fun notificationManager(
        context: PlatformContext,
        downloads: EntryDownloadRuntimeCoordinator,
        ownership: EntryMergeDownloadOwnershipProjection,
        presenter: EntryDownloadNotificationPresenter,
        @AppCoroutineScope scope: CoroutineScope,
    ): EntryDownloadNotificationManager = EntryDownloadNotificationManager(
        context = context,
        downloads = downloads,
        ownership = ownership,
        presenter = presenter,
        scope = scope,
    )

    @Provides
    @IntoSet
    internal fun notificationManagerWarmup(manager: () -> EntryDownloadNotificationManager): StartupWarmup =
        StartupWarmup { manager().start() }
}
