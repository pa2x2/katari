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

    /**
     * Package names of extensions libraries need that the Library already suggested installing here, or that were
     * uninstalled here, so the suggestion only comes back for others.
     */
    val suggestedMissingExtensions = preferenceStore.getStringSet(
        Preference.appStateKey("suggested_missing_extensions"),
        emptySet(),
    )

    /**
     * The installs and uninstalls on the account's other devices the Library already suggested doing here, each as
     * its package name, whether it was an install and when, so the card only comes back for newer ones.
     */
    val suggestedOtherDeviceChanges = preferenceStore.getStringSet(
        Preference.appStateKey("suggested_other_device_changes"),
        emptySet(),
    )

    /** Package names of extensions the account's other devices installed that are never to be suggested here. */
    val declinedOtherDeviceInstalls = preferenceStore.getStringSet(
        Preference.appStateKey("declined_other_device_installs"),
        emptySet(),
    )

    /** Package names of extensions the account's other devices uninstalled that are never to be suggested here. */
    val declinedOtherDeviceUninstalls = preferenceStore.getStringSet(
        Preference.appStateKey("declined_other_device_uninstalls"),
        emptySet(),
    )
}
