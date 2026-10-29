package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import mihon.desktop.library.NoEntryLibraryMembershipHost
import mihon.desktop.library.NoLibraryCustomCoverCleanup
import mihon.entry.interactions.library.membership.host.EntryLibraryCustomCoverHost
import mihon.entry.interactions.library.membership.host.EntryLibraryMembershipHost

@BindingContainer
interface LibraryBindings {
    @Binds
    val NoEntryLibraryMembershipHost.entryLibraryMembershipHost: EntryLibraryMembershipHost

    @Binds
    val NoLibraryCustomCoverCleanup.entryLibraryCustomCoverHost: EntryLibraryCustomCoverHost
}
