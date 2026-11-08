package eu.kanade.presentation.browse

import app.katari.entry.interactions.catalogue.EntryCatalogueFeature
import app.katari.entry.interactions.navigation.EntryOpenFeature
import app.katari.profiles.core.ProfileManager
import eu.kanade.tachiyomi.extension.ExtensionSystemPackages
import eu.kanade.tachiyomi.extension.kind.ExtensionCompatibility

/** Graph entry points of the browse composables. */
interface BrowsePresentationGraph {
    val entryCatalogueFeature: EntryCatalogueFeature
    val entryOpenFeature: EntryOpenFeature
    val extensionCompatibility: ExtensionCompatibility
    val extensionSystemPackages: ExtensionSystemPackages
    val profileManager: ProfileManager
}
