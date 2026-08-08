package mihon.core.common.image.progressive

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

class ProgressiveImagePreferences(
    preferenceStore: PreferenceStore,
) {
    val enabled: Preference<Boolean> = preferenceStore.getBoolean(
        key = "progressive_image_loading",
        defaultValue = false,
    )
}
