package app.katari.desktop.di

import app.katari.desktop.download.DesktopEntryDownloadNotifications
import app.katari.desktop.download.InProcessEntryDownloadWorkController
import app.katari.desktop.work.ResumableWork
import app.katari.entry.interactions.download.EntryDownloadNotificationPresenter
import app.katari.entry.interactions.download.EntryDownloadWorkController
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.IntoSet

@BindingContainer
interface DownloadWorkBindings {
    @Binds
    val InProcessEntryDownloadWorkController.entryDownloadWorkController: EntryDownloadWorkController

    @Binds
    @IntoSet
    val InProcessEntryDownloadWorkController.resumableEntryDownloads: ResumableWork

    @Binds
    val DesktopEntryDownloadNotifications.entryDownloadNotificationPresenter: EntryDownloadNotificationPresenter
}
