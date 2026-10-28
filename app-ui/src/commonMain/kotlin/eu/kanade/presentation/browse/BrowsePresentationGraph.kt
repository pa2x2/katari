package eu.kanade.presentation.browse

import eu.kanade.tachiyomi.extension.ExtensionSystemPackages
import eu.kanade.tachiyomi.extension.kind.ExtensionCompatibility
import mihon.entry.interactions.catalogue.EntryCatalogueFeature
import mihon.entry.interactions.navigation.EntryOpenFeature
import mihon.feature.profiles.core.ProfileManager

/** Graph entry points of the browse composables. */
interface BrowsePresentationGraph {
    val entryCatalogueFeature: EntryCatalogueFeature
    val entryOpenFeature: EntryOpenFeature
    val extensionCompatibility: ExtensionCompatibility
    val extensionSystemPackages: ExtensionSystemPackages
    val profileManager: ProfileManager
}
