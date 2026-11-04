package mihon.desktop.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import mihon.core.metro.AppCoroutineScope
import mihon.core.platform.storage.AppPrivateDirectories
import mihon.desktop.storage.DesktopAppPrivateDirectories
import mihon.desktop.storage.DesktopStorageFolderProvider
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.storage.MediaScanner
import tachiyomi.core.common.storage.NoMediaScanner
import tachiyomi.core.common.storage.PathStorageFileResolver
import tachiyomi.core.common.storage.StorageFileResolver
import tachiyomi.domain.storage.service.StorageManager
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

        @Provides
        fun storageFileResolver(): StorageFileResolver = PathStorageFileResolver()

        @Provides
        fun mediaScanner(): MediaScanner = NoMediaScanner

        @Provides
        @SingleIn(AppScope::class)
        fun storageManager(
            storageFiles: StorageFileResolver,
            mediaScanner: MediaScanner,
            storagePreferences: StoragePreferences,
            @AppCoroutineScope scope: CoroutineScope,
        ): StorageManager = StorageManager(storageFiles, mediaScanner, storagePreferences, scope)
    }
}
