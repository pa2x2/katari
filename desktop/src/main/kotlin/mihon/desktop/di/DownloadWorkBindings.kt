package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.IntoSet
import mihon.desktop.download.InProcessEntryDownloadWorkController
import mihon.desktop.work.ResumableWork
import mihon.entry.interactions.download.EntryDownloadWorkController

@BindingContainer
interface DownloadWorkBindings {
    @Binds
    val InProcessEntryDownloadWorkController.entryDownloadWorkController: EntryDownloadWorkController

    @Binds
    @IntoSet
    val InProcessEntryDownloadWorkController.resumableEntryDownloads: ResumableWork
}
