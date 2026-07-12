package eu.kanade.domain.source.service

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

class GlobalSourcePreferences(
    preferenceStore: PreferenceStore,
) {
    val extensionRepos = preferenceStore.getStringSet("extension_repos", emptySet())

    val extensionUpdatesCount = preferenceStore.getInt("ext_updates_count", 0)

    val trustedExtensions = preferenceStore.getStringSet(
        Preference.appStateKey("trusted_extensions"),
        emptySet(),
    )
}
