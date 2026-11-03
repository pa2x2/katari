package mihon.feature.appupdate.mpvaddon

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

/** App-wide, as the app's own update preferences: there is one add-on installed for every profile. */
internal class MpvAddonUpdatePreferences(preferenceStore: PreferenceStore) {

    /** The optional mpv add-on version whose prompt the user skipped. Empty for none. */
    val skippedVersion: Preference<String> =
        preferenceStore.getString(Preference.appStateKey("mpv_addon_update_skipped"), "")
}
