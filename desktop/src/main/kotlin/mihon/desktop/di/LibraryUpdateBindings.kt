package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import mihon.desktop.library.NoLibraryUpdateSchedule
import tachiyomi.domain.library.service.LibraryUpdateSchedule

@BindingContainer
interface LibraryUpdateBindings {
    @Binds
    val NoLibraryUpdateSchedule.libraryUpdateSchedule: LibraryUpdateSchedule
}
