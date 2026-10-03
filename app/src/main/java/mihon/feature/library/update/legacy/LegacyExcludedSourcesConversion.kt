package mihon.feature.library.update.legacy

import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.library.update.model.SourceUpdatePause

/**
 * Turns the old list of switched-off sources into pauses that last until the source is resumed.
 *
 * A source on the old list replaces any pause the store already has for it, so a restored backup's switches win.
 */
object LegacyExcludedSourcesConversion {

    fun convert(store: PreferenceStore) {
        val legacy = store.getStringSet(LibraryPreferences.LEGACY_UPDATE_EXCLUDED_SOURCES_PREF_KEY)
        if (!legacy.isSet()) return

        val sourceIds = legacy.get().mapNotNull(String::toLongOrNull).toSet()
        val pauses = LibraryPreferences(store).updatePausedSources
        pauses.set(
            pauses.get().filterNot { it.sourceId in sourceIds }.toSet() +
                sourceIds.map { SourceUpdatePause(it, until = null) },
        )
        legacy.delete()
    }
}
