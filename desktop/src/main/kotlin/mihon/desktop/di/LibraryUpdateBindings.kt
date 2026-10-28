package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import mihon.desktop.library.NoLibraryUpdateQueue
import mihon.desktop.library.NoLibraryUpdateSchedule
import mihon.desktop.library.NoMetadataUpdateRunner
import tachiyomi.domain.library.service.LibraryUpdateQueue
import tachiyomi.domain.library.service.LibraryUpdateSchedule
import tachiyomi.domain.library.service.MetadataUpdateRunner

@BindingContainer
interface LibraryUpdateBindings {
    @Binds
    val NoLibraryUpdateSchedule.libraryUpdateSchedule: LibraryUpdateSchedule

    @Binds
    val NoLibraryUpdateQueue.libraryUpdateQueue: LibraryUpdateQueue

    @Binds
    val NoMetadataUpdateRunner.metadataUpdateRunner: MetadataUpdateRunner
}
