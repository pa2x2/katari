package eu.kanade.tachiyomi.core.security

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

class PrivacyPreferences(
    preferenceStore: PreferenceStore,
) {
    // Stored under the name of the crash reporter the app first had, which keeps the choice of those who made it
    val crashReports: Preference<Boolean> = preferenceStore.getBoolean("crashlytics", true)

    // Off until the user turns it on: usage statistics are sent only with the user's consent
    val analytics: Preference<Boolean> = preferenceStore.getBoolean("analytics", false)
}
