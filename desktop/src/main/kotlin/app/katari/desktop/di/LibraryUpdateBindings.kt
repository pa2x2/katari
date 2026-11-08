package app.katari.desktop.di

import app.katari.desktop.library.DesktopLibraryUpdateNotifications
import app.katari.desktop.library.InProcessLibraryUpdateSchedule
import app.katari.desktop.library.InProcessLibraryUpdateWorkChain
import app.katari.desktop.library.InProcessMetadataUpdateRunner
import app.katari.desktop.work.ResumableWork
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.IntoSet
import eu.kanade.tachiyomi.data.library.notification.LibraryUpdateNotifications
import eu.kanade.tachiyomi.data.library.queue.LibraryUpdateWorkChain
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
    val DesktopLibraryUpdateNotifications.libraryUpdateNotifications: LibraryUpdateNotifications
}
