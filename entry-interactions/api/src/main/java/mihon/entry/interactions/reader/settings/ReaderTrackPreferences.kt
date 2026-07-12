package mihon.entry.interactions.reader.settings

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

class ReaderTrackPreferences(
    preferenceStore: PreferenceStore,
) {
    val autoUpdateTrack: Preference<Boolean> = preferenceStore.getBoolean("pref_auto_update_manga_sync_key", true)
}
