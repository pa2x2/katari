package eu.kanade.tachiyomi.di

import android.app.Application
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.source.service.GlobalSourcePreferences
import eu.kanade.domain.source.service.ProfileSourcePreferences
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.domain.track.service.GlobalTrackPreferences
import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.tachiyomi.core.security.PrivacyPreferences
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.network.NetworkPreferences
import eu.kanade.tachiyomi.util.system.DeviceUtil
import eu.kanade.tachiyomi.util.system.isDebugBuildType
import eu.kanade.tachiyomi.util.system.isDynamicColorAvailable
import mihon.core.common.CustomPreferences
import mihon.core.common.GlobalCustomPreferences
import mihon.feature.profiles.core.ProfileAwareStore
import mihon.feature.profiles.core.ProfileStore
import mihon.feature.profiles.core.ProfileStoreImpl
import mihon.feature.profiles.core.ProfilesPreferences
import tachiyomi.core.common.preference.AndroidPreferenceStore
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.storage.AndroidStorageFolderProvider
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.domain.backup.service.BackupPreferences
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.library.service.DuplicatePreferences
import tachiyomi.domain.library.service.GlobalLibraryPreferences
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.storage.service.StoragePreferences
import tachiyomi.domain.updates.service.UpdatesPreferences
import uy.kohesive.injekt.api.InjektModule
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

class PreferenceModule(val app: Application) : InjektModule {

    override fun InjektRegistrar.registerInjectables() {
        addSingletonFactory<PreferenceStore> {
            AndroidPreferenceStore(app)
        }
        addSingletonFactory { ProfilesPreferences(get()) }
        addSingletonFactory { ProfileStoreImpl(app, get()) }
        addSingletonFactory<ProfileStore> { get<ProfileStoreImpl>() }
        addSingletonFactory<ProfileAwareStore> { get<ProfileStoreImpl>() }
        addSingletonFactory<ActiveProfileProvider> { get<ProfileStoreImpl>() }
        addSingletonFactory {
            NetworkPreferences(
                preferenceStore = get<ProfileStore>().basePreferenceStore(),
                verboseLoggingDefault = isDebugBuildType,
            )
        }
        addSingletonFactory {
            SourcePreferences(
                preferenceStore = get<ProfileStore>().profileStore(),
                json = get(),
            )
        }
        addSingletonFactory { ProfileSourcePreferences(get(), get()) }
        addSingletonFactory { GlobalSourcePreferences(get<ProfileStore>().basePreferenceStore()) }
        addSingletonFactory {
            SecurityPreferences(get<ProfileStore>().profileStore())
        }
        addSingletonFactory {
            PrivacyPreferences(get<ProfileStore>().basePreferenceStore())
        }
        addSingletonFactory {
            LibraryPreferences(get<ProfileStore>().profileStore())
        }
        addSingletonFactory { GlobalLibraryPreferences(get<ProfileStore>().basePreferenceStore()) }
        addSingletonFactory { DuplicatePreferences(get<ProfileStore>().profileStore()) }
        addSingletonFactory {
            UpdatesPreferences(get<ProfileStore>().profileStore())
        }
        addSingletonFactory {
            TrackPreferences(get<ProfileStore>().privateStore())
        }
        addSingletonFactory { GlobalTrackPreferences(get<ProfileStore>().basePreferenceStore()) }
        addSingletonFactory {
            DownloadPreferences(get<ProfileStore>().basePreferenceStore())
        }
        addSingletonFactory {
            BackupPreferences(get<ProfileStore>().basePreferenceStore())
        }
        addSingletonFactory {
            StoragePreferences(
                folderProvider = get<AndroidStorageFolderProvider>(),
                preferenceStore = get(),
            )
        }
        addSingletonFactory {
            UiPreferences(
                preferenceStore = get<ProfileStore>().profileStore(),
                dynamicColorAvailable = DeviceUtil.isDynamicColorAvailable,
            )
        }
        addSingletonFactory {
            BasePreferences(app, get<ProfileStore>().basePreferenceStore())
        }
        addSingletonFactory {
            CustomPreferences(get<ProfileStore>().profileStore())
        }
        addSingletonFactory { GlobalCustomPreferences(get<ProfileStore>().basePreferenceStore()) }
    }
}
