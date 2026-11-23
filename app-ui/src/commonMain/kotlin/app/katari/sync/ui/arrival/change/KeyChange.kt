package app.katari.sync.ui.arrival.change

import app.katari.sync.client.arrival.ArrivedChange
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.StoredValuesPreferenceStore

/**
 * What other devices changed in the setting [key] of [profileId]'s store, or the shared one when null, as values the
 * store holds ([ArrivedChange]).
 */
internal class KeyChange(val profileId: Long?, val key: String, val change: ArrivedChange) {
    /**
     * The values before and after, read through [preference] the way the app reads them, so its type, defaults and
     * stored form come from the preference that owns them; null when [preference] is another setting's.
     */
    fun <T> of(preference: (PreferenceStore) -> Preference<T>): ChangedValue<T>? {
        if (preference(StoredValuesPreferenceStore(emptyMap())).key() != key) return null
        return ChangedValue(
            before = preference(StoredValuesPreferenceStore(mapOf(key to change.before))).get(),
            after = preference(StoredValuesPreferenceStore(mapOf(key to change.after))).get(),
        )
    }
}

internal data class ChangedValue<T>(val before: T, val after: T)
