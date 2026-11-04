package mihon.entry.interactions.download

import android.app.Application
import android.app.Notification
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import mihon.entry.interactions.download.notification.AndroidEntryDownloadNotifier
import mihon.entry.interactions.download.notification.entryDownloadForegroundNotification

@ContributesTo(AppScope::class)
@BindingContainer
object AndroidEntryDownloadBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun workController(application: Application): EntryDownloadWorkController =
        DefaultEntryDownloadWorkController(application)

    @Provides
    @SingleIn(AppScope::class)
    internal fun notificationPresenter(
        application: Application,
        actions: EntryDownloadNotificationActions,
        securityPreferences: () -> SecurityPreferences,
    ): EntryDownloadNotificationPresenter = AndroidEntryDownloadNotifier(
        context = application,
        actions = actions,
        hideNotificationContent = { securityPreferences().hideNotificationContent.get() },
    )

    @Provides
    internal fun workerDependencies(
        run: EntryDownloadRun,
        notificationProvider: EntryDownloadForegroundNotificationProvider,
    ): EntryDownloadWorkerDependencies = EntryDownloadWorkerDependencies(
        run = run,
        notificationProvider = notificationProvider,
    )

    // The download worker's foreground notification, under the progress notification's id so the progress replaces it
    @Provides
    internal fun foregroundNotificationProvider(application: Application): EntryDownloadForegroundNotificationProvider =
        object : EntryDownloadForegroundNotificationProvider {
            override val notificationId: Int = EntryDownloadNotifications.ID_PROGRESS

            override fun notification(): Notification = application.entryDownloadForegroundNotification()
        }
}
