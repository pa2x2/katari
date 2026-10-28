package eu.kanade.tachiyomi.ui.browse.source.preferences

import mihon.entry.interactions.source.EntrySourceSettingsFeature

/** Graph services the source preferences screen reads outside a view model. */
interface SourcePreferencesGraph {
    val entrySourceSettingsFeature: EntrySourceSettingsFeature
}
