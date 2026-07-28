package mihon.translation.runtime

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

class DeviceTranslationPreferences(
    preferenceStore: PreferenceStore,
) {
    val wifiOnlyModelDownloads: Preference<Boolean> = preferenceStore.getBoolean(
        key = "translation_wifi_only_model_downloads",
        defaultValue = true,
    )

    val mlKitDisclosureAcknowledged: Preference<Boolean> = preferenceStore.getBoolean(
        key = Preference.appStateKey("translation_mlkit_disclosure_acknowledged"),
        defaultValue = false,
    )
}
