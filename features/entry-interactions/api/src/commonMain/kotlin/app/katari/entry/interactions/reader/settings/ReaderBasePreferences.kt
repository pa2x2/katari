package app.katari.entry.interactions.reader.settings

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

class ReaderBasePreferences(
    preferenceStore: PreferenceStore,
) {
    val incognitoMode: Preference<Boolean> = preferenceStore.getBoolean(Preference.appStateKey("incognito_mode"), false)

    val displayProfile: Preference<String> = preferenceStore.getString("pref_display_profile_key", "")
}
