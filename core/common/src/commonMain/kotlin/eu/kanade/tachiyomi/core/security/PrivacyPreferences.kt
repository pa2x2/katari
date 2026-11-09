package eu.kanade.tachiyomi.core.security

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

class PrivacyPreferences(
    preferenceStore: PreferenceStore,
) {
    // Stored under the name of the crash reporter the app first had, which keeps the choice of those who made it
    val crashReports: Preference<Boolean> = preferenceStore.getBoolean("crashlytics", true)

    val analytics: Preference<Boolean> = preferenceStore.getBoolean("analytics", true)
}
