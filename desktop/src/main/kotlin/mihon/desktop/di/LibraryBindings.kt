package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides
import eu.kanade.tachiyomi.ui.library.ReleasePeriodFilterAvailability
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

    companion object {
        // Release periods follow the update intervals library updates work out, and desktop runs no library updates.
        @Provides
        fun releasePeriodFilterAvailability(): ReleasePeriodFilterAvailability =
            ReleasePeriodFilterAvailability(isOffered = false)
    }
}
