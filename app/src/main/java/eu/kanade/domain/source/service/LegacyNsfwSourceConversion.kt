package eu.kanade.domain.source.service

import mihon.domain.extension.model.ContentWarning
import tachiyomi.core.common.preference.PreferenceStore

/**
 * Turns the old switch for showing NSFW extensions into the content warnings that are allowed to load.
 *
 * The switch also hid extensions flagged as mixed, so turning it off allows only safe ones. A store with the switch
 * replaces whatever it allows already, so a restored backup's choice wins.
 */
object LegacyNsfwSourceConversion {

    fun convert(store: PreferenceStore) {
        val legacy = store.getBoolean(SourcePreferences.LEGACY_SHOW_NSFW_SOURCE_KEY, true)
        if (!legacy.isSet()) return

        SourcePreferences.enabledContentWarnings(store).set(
            if (legacy.get()) ContentWarning.entries.toSet() else setOf(ContentWarning.SAFE),
        )
        legacy.delete()
    }
}
