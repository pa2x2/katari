package mihon.entry.interactions.runtime

import android.content.Context
import coil3.ComponentRegistry
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import mihon.core.metro.metroGraph

/** Graph entry point of the application's image loader, which Coil asks the application to build. */
@ContributesTo(AppScope::class)
interface EntryInteractionImageGraph {
    val entryImageComponentInstallers: EntryImageComponentInstallers
}

fun ComponentRegistry.Builder.addEntryInteractionImageComponents(context: Context): ComponentRegistry.Builder {
    context.metroGraph<EntryInteractionImageGraph>().entryImageComponentInstallers.values.forEach { it.install(this) }
    return this
}
