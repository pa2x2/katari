package mihon.entry.interactions.runtime.production

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import mihon.entry.interactions.runtime.EntryInteractions
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** Entry type modules still assemble [EntryInteractions] while installing into Injekt; the graph reads it from there. */
@ContributesTo(AppScope::class)
@BindingContainer
object EntryInteractionsInjektBindings {
    @Provides
    fun entryInteractions(): EntryInteractions = Injekt.get()
}
