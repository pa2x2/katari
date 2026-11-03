package tachiyomi.domain.storage.service

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.storage.FolderProvider

class StoragePreferences(
    folderProvider: FolderProvider,
    preferenceStore: PreferenceStore,
) {

    /** Holds the platform's default folder until the user picks one, which the settings show as no location set. */
    val baseStorageDirectory: Preference<String> = preferenceStore.getString(
        Preference.appStateKey("storage_dir"),
        folderProvider.path(),
    )

    /**
     * The storage location the user picked, the only folder the app may write into; null until they pick one. Work that
     * runs before onboarding's storage step, such as the first automatic backup, would otherwise fill the default
     * folder wherever it happens to exist, although the user goes on to pick another.
     */
    fun pickedStorageDirectory(): String? = baseStorageDirectory.takeIf { it.isSet() }?.get()
}
