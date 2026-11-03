package eu.kanade.tachiyomi.extension.platform

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.extension.kind.ExtensionFamily

/** Android loads every family's extensions. */
@Inject
class AndroidHostedExtensionFamilies : HostedExtensionFamilies {
    override val families: Set<ExtensionFamily> = ExtensionFamily.entries.toSet()
}
