package eu.kanade.domain.base

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

/** Incognito mode, which records no history while on. Kept on the base store, so it covers every profile. */
class IncognitoPreferences(preferenceStore: PreferenceStore) {
    val incognitoMode: Preference<Boolean> = preferenceStore.getBoolean(Preference.appStateKey("incognito_mode"), false)
}
