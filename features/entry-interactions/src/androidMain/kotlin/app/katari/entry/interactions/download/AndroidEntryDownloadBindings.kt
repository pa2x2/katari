package app.katari.entry.interactions.download

import android.app.Application
import android.app.Notification
import androidx.work.ListenableWorker
import app.katari.entry.interactions.download.notification.AndroidEntryDownloadNotifier
import app.katari.entry.interactions.download.notification.entryDownloadForegroundNotification
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.StringKey
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import kotlin.reflect.KClass

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

    @Provides
    @IntoMap
    @StringKey("mihon.entry.interactions.download.EntryDownloadJob")
    fun retiredEntryDownloadJob(): KClass<out ListenableWorker> = EntryDownloadJob::class

    @Provides
    @IntoMap
    @StringKey("mihon.entry.interactions.manga.download.DownloadJob")
    fun retiredMangaDownloadJob(): KClass<out ListenableWorker> = LegacyDownloadJob::class

    @Provides
    @IntoMap
    @StringKey("mihon.entry.interactions.book.download.BookDownloadJob")
    fun retiredBookDownloadJob(): KClass<out ListenableWorker> = LegacyDownloadJob::class

    // The download worker's foreground notification, under the progress notification's id so the progress replaces it
    @Provides
    internal fun foregroundNotificationProvider(application: Application): EntryDownloadForegroundNotificationProvider =
        object : EntryDownloadForegroundNotificationProvider {
            override val notificationId: Int = EntryDownloadNotifications.ID_PROGRESS

            override fun notification(): Notification = application.entryDownloadForegroundNotification()
        }
}
