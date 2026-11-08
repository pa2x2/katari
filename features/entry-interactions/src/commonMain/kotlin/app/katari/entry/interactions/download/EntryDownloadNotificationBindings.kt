package app.katari.entry.interactions.download

import app.katari.core.metro.AppCoroutineScope
import app.katari.core.metro.StartupWarmup
import app.katari.core.platform.PlatformContext
import app.katari.entry.interactions.merge.EntryMergeDownloadOwnershipProjection
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope

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
