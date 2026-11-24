package eu.kanade.domain.source.service

import kotlinx.serialization.json.Json
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.domain.library.model.LibraryDisplayMode

/**
 * Turns the display mode every source remembered into Browse's layout and the sources that keep their own. Before
 * Browse had a layout, each source the person changed kept a mode, so the mode most of them share becomes Browse's and
 * only the sources that differ keep theirs. A store with the old modes replaces Browse's mode, so a restored backup's
 * choice wins.
 */
object LegacySourceDisplayModesConversion {

    fun convert(store: PreferenceStore) {
        val prefix = SourcePreferences.LEGACY_SOURCE_DISPLAY_MODE_KEY_FAMILY.value
        val legacy = store.getAll().keys
            .mapNotNull { key ->
                key.removePrefix(prefix).takeIf { key.startsWith(prefix) }?.toLongOrNull()
            }
            .associateWith { sourceKey -> LibraryDisplayMode.deserialize(legacyMode(store, sourceKey).get()) }
        if (legacy.isEmpty()) return

        val preferences = SourcePreferences(store, Json)
        val browseMode = legacy.values.groupingBy { it }.eachCount().maxBy { it.value }.key
        preferences.sourceDisplayMode.set(browseMode)
        legacy.forEach { (sourceKey, mode) ->
            if (mode != browseMode) preferences.sourceDisplayMode(sourceKey).set(mode)
            legacyMode(store, sourceKey).delete()
        }
    }

    private fun legacyMode(store: PreferenceStore, sourceKey: Long) =
        store.getString(SourcePreferences.LEGACY_SOURCE_DISPLAY_MODE_KEY_FAMILY.key(sourceKey))
}
