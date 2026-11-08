package mihon.entry.interactions.runtime

import coil3.ComponentRegistry
import dev.zacsweers.metro.Inject

/** The entry types' decoders and fetchers, which the application's image loader installs. */
@Inject
class EntryInteractionImageComponents(private val installers: EntryImageComponentInstallers) {
    fun installInto(components: ComponentRegistry.Builder) {
        installers.values.forEach { it.install(components) }
    }
}
