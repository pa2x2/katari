package mihon.entry.interactions.runtime

import coil3.ComponentRegistry

/** Coil components an Entry type needs to load its images, installed into the app's image loader. */
fun interface EntryImageComponentInstaller {
    fun install(builder: ComponentRegistry.Builder)
}

data class EntryImageComponentInstallers(
    val values: List<EntryImageComponentInstaller>,
)
