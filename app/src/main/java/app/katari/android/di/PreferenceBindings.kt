package app.katari.android.di

import android.app.Application
import app.katari.android.storage.AndroidPreferenceFiles
import app.katari.android.storage.AndroidStorageFolderProvider
import app.katari.appupdate.mpvaddon.MpvAddonUpdatePreferences
import app.katari.core.metro.BasePreferenceStore
import app.katari.profiles.core.ProfileAwareStore
import app.katari.profiles.core.ProfileStore
import app.katari.profiles.core.ProfileStoreImpl
import app.katari.profiles.core.ProfilesPreferences
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.domain.base.BasePreferences
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
