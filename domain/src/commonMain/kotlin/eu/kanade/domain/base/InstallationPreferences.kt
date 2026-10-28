package eu.kanade.domain.base

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

/**
 * The ID that tells this installation's bug reports apart, which the debug info shows. Kept on the base store, so it
 * covers every profile. Empty until it is generated.
 */
class InstallationPreferences(preferenceStore: PreferenceStore) {
    val installationId: Preference<String> = preferenceStore.getString(Preference.appStateKey("installation_id"), "")
}
