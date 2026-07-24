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
import tachiyomi.core.common.preference.ProfilePreferenceOwnerId
import tachiyomi.core.common.preference.ProfilePreferenceOwnerInstaller
import tachiyomi.core.common.preference.ProfilePreferenceOwnerRegistry
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
import uy.kohesive.injekt.api.addSingleton
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
        val profilePreferenceOwners = ProfilePreferenceOwnerRegistry()
        addSingleton(profilePreferenceOwners)
        val profilePreferenceOwnerInstaller = ProfilePreferenceOwnerInstaller(profilePreferenceOwners) {
            get<ProfileStore>().profileStore()
        }
        val privatePreferenceOwnerInstaller = ProfilePreferenceOwnerInstaller(profilePreferenceOwners) {
            get<ProfileStore>().privateStore()
        }

        val sourcePreferencesOwner = profilePreferenceOwnerInstaller.register(
            id = ProfilePreferenceOwnerId("app.source"),
            keyPatterns = SourcePreferences.profileKeyPatterns,
        ) { store -> SourcePreferences(store, get()) }
        val securityPreferencesOwner = profilePreferenceOwnerInstaller.register(
            ProfilePreferenceOwnerId("app.security"),
            factory = ::SecurityPreferences,
        )
        val libraryPreferencesOwner = profilePreferenceOwnerInstaller.register(
            id = ProfilePreferenceOwnerId("app.library"),
            keyPatterns = LibraryPreferences.profileKeyPatterns,
            factory = ::LibraryPreferences,
        )
        val duplicatePreferencesOwner = profilePreferenceOwnerInstaller.register(
            ProfilePreferenceOwnerId("app.duplicate-detection"),
            factory = ::DuplicatePreferences,
        )
        val updatesPreferencesOwner = profilePreferenceOwnerInstaller.register(
            ProfilePreferenceOwnerId("app.updates"),
            factory = ::UpdatesPreferences,
        )
        val trackPreferencesOwner = privatePreferenceOwnerInstaller.register(
            id = ProfilePreferenceOwnerId("app.tracking"),
            keyPatterns = TrackPreferences.profileKeyPatterns,
            factory = ::TrackPreferences,
        )
        val uiPreferencesOwner = profilePreferenceOwnerInstaller.register(
            ProfilePreferenceOwnerId("app.ui"),
        ) { store -> UiPreferences(store, DeviceUtil.isDynamicColorAvailable) }
        val customPreferencesOwner = profilePreferenceOwnerInstaller.register(
            ProfilePreferenceOwnerId("app.custom"),
            factory = ::CustomPreferences,
        )
        addSingletonFactory {
            NetworkPreferences(
                preferenceStore = get<ProfileStore>().basePreferenceStore(),
                verboseLoggingDefault = isDebugBuildType,
            )
        }
        addSingletonFactory { sourcePreferencesOwner.create() }
        addSingletonFactory { ProfileSourcePreferences(get(), get()) }
        addSingletonFactory { GlobalSourcePreferences(get<ProfileStore>().basePreferenceStore()) }
        addSingletonFactory { securityPreferencesOwner.create() }
        addSingletonFactory {
            PrivacyPreferences(get<ProfileStore>().basePreferenceStore())
        }
        addSingletonFactory { libraryPreferencesOwner.create() }
        addSingletonFactory { GlobalLibraryPreferences(get<ProfileStore>().basePreferenceStore()) }
        addSingletonFactory { duplicatePreferencesOwner.create() }
        addSingletonFactory { updatesPreferencesOwner.create() }
        addSingletonFactory { trackPreferencesOwner.create() }
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
        addSingletonFactory { uiPreferencesOwner.create() }
        addSingletonFactory {
            BasePreferences(app, get<ProfileStore>().basePreferenceStore())
        }
        addSingletonFactory { customPreferencesOwner.create() }
        addSingletonFactory { GlobalCustomPreferences(get<ProfileStore>().basePreferenceStore()) }
    }
}
