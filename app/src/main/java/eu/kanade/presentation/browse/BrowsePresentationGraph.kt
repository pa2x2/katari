package eu.kanade.presentation.browse

import mihon.entry.interactions.catalogue.EntryCatalogueFeature
import mihon.entry.interactions.navigation.EntryOpenFeature
import mihon.feature.profiles.core.ProfileManager

/** Graph entry points of the browse composables. */
interface BrowsePresentationGraph {
    val entryCatalogueFeature: EntryCatalogueFeature
    val entryOpenFeature: EntryOpenFeature
    val profileManager: ProfileManager
}
