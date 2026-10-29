package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.IntoSet
import eu.kanade.tachiyomi.data.library.notification.LibraryUpdateNotifications
import eu.kanade.tachiyomi.data.library.queue.LibraryUpdateWorkChain
import mihon.desktop.library.InProcessLibraryUpdateSchedule
import mihon.desktop.library.InProcessLibraryUpdateWorkChain
import mihon.desktop.library.InProcessMetadataUpdateRunner
import mihon.desktop.library.NoLibraryUpdateNotifications
import mihon.desktop.work.ResumableWork
import tachiyomi.domain.library.service.LibraryUpdateSchedule
import tachiyomi.domain.library.service.MetadataUpdateRunner

@BindingContainer
interface LibraryUpdateBindings {
    @Binds
    val InProcessLibraryUpdateWorkChain.libraryUpdateWorkChain: LibraryUpdateWorkChain

    @Binds
    @IntoSet
    val InProcessLibraryUpdateWorkChain.resumableLibraryUpdates: ResumableWork

    @Binds
    val InProcessLibraryUpdateSchedule.libraryUpdateSchedule: LibraryUpdateSchedule

    @Binds
    @IntoSet
    val InProcessLibraryUpdateSchedule.resumableLibraryUpdateSchedules: ResumableWork

    @Binds
    val InProcessMetadataUpdateRunner.metadataUpdateRunner: MetadataUpdateRunner

    @Binds
    @IntoSet
    val InProcessMetadataUpdateRunner.resumableMetadataUpdates: ResumableWork

    @Binds
    val NoLibraryUpdateNotifications.libraryUpdateNotifications: LibraryUpdateNotifications
}
