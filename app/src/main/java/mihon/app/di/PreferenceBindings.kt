package mihon.app.di

import android.app.Application
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.domain.base.BasePreferences
import mihon.app.storage.AndroidPreferenceFiles
import mihon.app.storage.AndroidStorageFolderProvider
import mihon.core.metro.BasePreferenceStore
import mihon.feature.appupdate.AppUpdatePreferences
import mihon.feature.appupdate.mpvaddon.MpvAddonUpdatePreferences
import mihon.feature.profiles.core.ProfileAwareStore
import mihon.feature.profiles.core.ProfileStore
import mihon.feature.profiles.core.ProfileStoreImpl
import mihon.feature.profiles.core.ProfilesPreferences
import tachiyomi.core.common.preference.PlatformPreferenceFiles
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.domain.storage.service.StoragePreferences

/** Android's preference stores, and the preference classes that need more of Android than a store. */
@BindingContainer
interface PreferenceBindings {
    @Binds
    val ProfileStoreImpl.profileStore: ProfileStore

    @Binds
    val ProfileStoreImpl.profileAwareStore: ProfileAwareStore

    @Binds
    val ProfileStoreImpl.activeProfileProvider: ActiveProfileProvider

    @Binds
    val AndroidPreferenceFiles.preferenceFiles: PlatformPreferenceFiles

    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun profileStoreImpl(application: Application, profilesPreferences: ProfilesPreferences): ProfileStoreImpl =
            ProfileStoreImpl(application, profilesPreferences)

        @Provides
        @SingleIn(AppScope::class)
        internal fun appUpdatePreferences(
            @BasePreferenceStore store: PreferenceStore,
        ): AppUpdatePreferences = AppUpdatePreferences(store)

        @Provides
        @SingleIn(AppScope::class)
        internal fun mpvAddonUpdatePreferences(
            @BasePreferenceStore store: PreferenceStore,
        ): MpvAddonUpdatePreferences = MpvAddonUpdatePreferences(store)

        @Provides
        @SingleIn(AppScope::class)
        fun storagePreferences(
            folderProvider: AndroidStorageFolderProvider,
            preferenceStore: PreferenceStore,
        ): StoragePreferences = StoragePreferences(folderProvider = folderProvider, preferenceStore = preferenceStore)

        @Provides
        @SingleIn(AppScope::class)
        fun basePreferences(application: Application, @BasePreferenceStore store: PreferenceStore): BasePreferences =
            BasePreferences(application, store)
    }
}
