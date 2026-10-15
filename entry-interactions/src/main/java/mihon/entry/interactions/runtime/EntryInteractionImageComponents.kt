package mihon.entry.interactions.runtime

import coil3.ComponentRegistry
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

fun ComponentRegistry.Builder.addEntryInteractionImageComponents(): ComponentRegistry.Builder {
    Injekt.get<EntryImageComponentInstallers>().values.forEach { it.install(this) }
    return this
}
