package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import mihon.desktop.entry.NoEntryDestructiveRemovalFeature
import mihon.entry.interactions.lifecycle.removal.EntryDestructiveRemovalFeature

@BindingContainer
interface EntryRemovalBindings {
    @Binds
    val NoEntryDestructiveRemovalFeature.entryDestructiveRemovalFeature: EntryDestructiveRemovalFeature
}
