package app.katari.desktop.di

import app.katari.core.archive.ArchiveOpener
import app.katari.core.archive.PathArchiveOpener
import app.katari.core.metro.AppCoroutineScope
import app.katari.core.platform.storage.AppPrivateDirectories
import app.katari.core.platform.storage.UserPicturesFolder
import app.katari.desktop.storage.DesktopAppPrivateDirectories
import app.katari.desktop.storage.DesktopDirectories
import app.katari.desktop.storage.DesktopStorageFolderProvider
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
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
        fun archiveOpener(): ArchiveOpener = PathArchiveOpener

        @Provides
        fun userPicturesFolder(directories: DesktopDirectories): UserPicturesFolder =
            UserPicturesFolder { directories.pictures }

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
