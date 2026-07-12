package mihon.feature.profiles.core

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

class ProfilesPreferences(
    preferenceStore: PreferenceStore,
) {
    val legacyPreferenceMigrationVersion: Preference<Int> = preferenceStore.getInt(
        Preference.appStateKey("legacy_profile_preference_migration_version"),
        0,
    )

    val activeProfileId: Preference<Long> = preferenceStore.getLong(
        Preference.appStateKey("active_profile_id"),
        ProfileConstants.DEFAULT_PROFILE_ID,
    )

    val pickerEnabled: Preference<Boolean> = preferenceStore.getBoolean(
        Preference.appStateKey("profiles_picker_enabled"),
        true,
    )

    val pendingIntentUri: Preference<String> = preferenceStore.getString(
        Preference.appStateKey("pending_profile_intent_uri"),
        "",
    )
}
