package mihon.desktop.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import mihon.core.platform.storage.AppPrivateDirectories
import mihon.desktop.storage.DesktopAppPrivateDirectories
import mihon.desktop.storage.DesktopStorageFolderProvider
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.domain.storage.service.StoragePreferences

@BindingContainer
interface StorageBindings {
    @Binds
    val DesktopAppPrivateDirectories.appPrivateDirectories: AppPrivateDirectories

    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun storagePreferences(
            folderProvider: DesktopStorageFolderProvider,
            preferenceStore: PreferenceStore,
        ): StoragePreferences = StoragePreferences(folderProvider = folderProvider, preferenceStore = preferenceStore)
    }
}
