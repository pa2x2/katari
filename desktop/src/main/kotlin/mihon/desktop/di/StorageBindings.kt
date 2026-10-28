package mihon.desktop.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import mihon.desktop.platform.DesktopOperatingSystem
import mihon.desktop.storage.DesktopDirectories
import mihon.desktop.storage.DesktopStorageFolderProvider
import mihon.desktop.storage.systemDesktopDirectories
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.domain.storage.service.StoragePreferences

@BindingContainer
object StorageBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun desktopDirectories(operatingSystem: DesktopOperatingSystem): DesktopDirectories =
        systemDesktopDirectories(operatingSystem)

    @Provides
    @SingleIn(AppScope::class)
    fun storagePreferences(
        folderProvider: DesktopStorageFolderProvider,
        preferenceStore: PreferenceStore,
    ): StoragePreferences = StoragePreferences(folderProvider = folderProvider, preferenceStore = preferenceStore)
}
