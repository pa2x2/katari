package mihon.entry.interactions.download

import android.app.Application
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import kotlinx.coroutines.CoroutineScope
import mihon.core.metro.AppCoroutineScope
import mihon.core.metro.StartupWarmup
import mihon.core.platform.network.NetworkConnectivity
import mihon.entry.interactions.download.host.EntryDownloadSourceAccessResolver
import mihon.entry.interactions.download.host.SourceManagerEntryDownloadSourceAccessResolver
import mihon.entry.interactions.download.notification.AndroidEntryDownloadNotifier
import mihon.entry.interactions.merge.EntryMergeDownloadOwnershipProjection
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.source.service.SourceManager

@ContributesTo(AppScope::class)
@BindingContainer
object AndroidEntryDownloadBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun workController(application: Application): EntryDownloadWorkController =
        DefaultEntryDownloadWorkController(application)

    @Provides
    @SingleIn(AppScope::class)
    internal fun sourceAccessResolver(sourceManager: SourceManager): EntryDownloadSourceAccessResolver =
        SourceManagerEntryDownloadSourceAccessResolver(sourceManager)

    @Provides
    @SingleIn(AppScope::class)
    internal fun notificationManager(
        application: Application,
        downloads: EntryDownloadRuntimeCoordinator,
        actions: EntryDownloadNotificationActions,
        ownership: EntryMergeDownloadOwnershipProjection,
        securityPreferences: () -> SecurityPreferences,
        @AppCoroutineScope scope: CoroutineScope,
    ): EntryDownloadNotificationManager = EntryDownloadNotificationManager(
        context = application,
        downloads = downloads,
        actions = actions,
        ownership = ownership,
        presenter = AndroidEntryDownloadNotifier(
            context = application,
            actions = actions,
            hideNotificationContent = { securityPreferences().hideNotificationContent.get() },
        ),
        scope = scope,
    )

    @Provides
    internal fun workerDependencies(
        downloads: EntryDownloadRuntimeCoordinator,
        downloadPreferences: DownloadPreferences,
        notificationProvider: EntryDownloadForegroundNotificationProvider,
        network: NetworkConnectivity,
    ): EntryDownloadWorkerDependencies = EntryDownloadWorkerDependencies(
        downloads = downloads,
        downloadPreferences = downloadPreferences,
        notificationProvider = notificationProvider,
        network = network,
    )

    @Provides
    internal fun foregroundNotificationProvider(
        manager: EntryDownloadNotificationManager,
    ): EntryDownloadForegroundNotificationProvider = manager

    @Provides
    @IntoSet
    internal fun notificationManagerWarmup(manager: () -> EntryDownloadNotificationManager): StartupWarmup =
        StartupWarmup { manager().start() }
}
