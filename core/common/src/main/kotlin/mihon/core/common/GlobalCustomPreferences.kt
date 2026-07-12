package mihon.core.common

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

class GlobalCustomPreferences(
    preferenceStore: PreferenceStore,
) {
    val extensionsAutoUpdates: Preference<Boolean> = preferenceStore.getBoolean(
        Preference.appStateKey("extensions_auto_updates"),
        false,
    )
}
