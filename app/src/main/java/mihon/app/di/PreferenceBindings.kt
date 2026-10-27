package mihon.app.di

import android.app.Application
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
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
import eu.kanade.tachiyomi.ui.translator.session.TranslatorLanguagePreferences
import eu.kanade.tachiyomi.util.system.DeviceUtil
import eu.kanade.tachiyomi.util.system.isDebugBuildType
import eu.kanade.tachiyomi.util.system.isDynamicColorAvailable
import kotlinx.serialization.json.Json
import mihon.app.storage.AndroidStorageFolderProvider
import mihon.core.common.CustomPreferences
import mihon.core.common.GlobalCustomPreferences
import mihon.core.common.image.progressive.ProgressiveImagePreferences
import mihon.core.metro.BasePreferenceStore
import mihon.entry.interactions.translate.EntryTranslatePreferences
import mihon.feature.appupdate.AppUpdatePreferences
import mihon.feature.profiles.core.ProfileAwareStore
import mihon.feature.profiles.core.ProfileStore
import mihon.feature.profiles.core.ProfileStoreImpl
import mihon.feature.profiles.core.ProfilesPreferences
import tachiyomi.core.common.preference.AndroidPreferenceStore
import tachiyomi.core.common.preference.InstalledProfilePreferenceOwner
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.ProfilePreferenceOwnerId
import tachiyomi.core.common.preference.ProfilePreferenceOwnerInstaller
import tachiyomi.core.common.preference.ProfilePreferenceOwnerRegistry
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.domain.backup.service.BackupPreferences
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.library.service.DuplicatePreferences
import tachiyomi.domain.library.service.GlobalLibraryPreferences
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.statistics.service.StatisticsPreferences
import tachiyomi.domain.storage.service.StoragePreferences
import tachiyomi.domain.upcoming.service.UpcomingPreferences
import tachiyomi.domain.updates.service.UpdatesPreferences

/**
 * The app's preference stores and the preference classes built on them. Profile-owned preferences register their
 * owner with the registry, which needs every owner before backup or profile code first asks it for ownership;
 * `registerProfilePreferenceOwners` resolves the registration set when the graph is built for that.
 */
@BindingContainer
interface PreferenceBindings {
    @Binds
    val ProfileStoreImpl.profileStore: ProfileStore

    @Binds
    val ProfileStoreImpl.profileAwareStore: ProfileAwareStore

    @Binds
    val ProfileStoreImpl.activeProfileProvider: ActiveProfileProvider

    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun preferenceStore(application: Application): PreferenceStore = AndroidPreferenceStore(application)

        @Provides
        @SingleIn(AppScope::class)
        fun profilesPreferences(preferenceStore: PreferenceStore): ProfilesPreferences =
            ProfilesPreferences(preferenceStore)

        @Provides
        @SingleIn(AppScope::class)
        fun profileStoreImpl(application: Application, profilesPreferences: ProfilesPreferences): ProfileStoreImpl =
            ProfileStoreImpl(application, profilesPreferences)

        @Provides
        @BasePreferenceStore
        fun basePreferenceStore(profileStore: ProfileStore): PreferenceStore = profileStore.basePreferenceStore()

        @Provides
        @SingleIn(AppScope::class)
        fun ownerRegistry(): ProfilePreferenceOwnerRegistry = ProfilePreferenceOwnerRegistry()

        @Provides
        @SingleIn(AppScope::class)
        fun ownerInstaller(
            registry: ProfilePreferenceOwnerRegistry,
            profileStore: ProfileStore,
        ): ProfilePreferenceOwnerInstaller = ProfilePreferenceOwnerInstaller(registry) { profileStore.profileStore() }

        @Provides
        @SingleIn(AppScope::class)
        fun sourcePreferencesOwner(
            owners: ProfilePreferenceOwnerInstaller,
            json: Json,
        ): InstalledProfilePreferenceOwner<SourcePreferences> =
            owners.register(
                id = ProfilePreferenceOwnerId("app.source"),
                keyPatterns = SourcePreferences.profileKeyPatterns,
            ) { store -> SourcePreferences(store, json) }

        @Provides
        @IntoSet
        fun sourcePreferencesOwnerRegistration(
            owner: InstalledProfilePreferenceOwner<SourcePreferences>,
        ): InstalledProfilePreferenceOwner<*> = owner

        @Provides
        @SingleIn(AppScope::class)
        fun sourcePreferences(
            owner: InstalledProfilePreferenceOwner<SourcePreferences>,
        ): SourcePreferences = owner.create()

        @Provides
        @SingleIn(AppScope::class)
        fun securityPreferencesOwner(
            owners: ProfilePreferenceOwnerInstaller,
        ): InstalledProfilePreferenceOwner<SecurityPreferences> =
            owners.register(
                id = ProfilePreferenceOwnerId("app.security"),
                factory = ::SecurityPreferences,
            )

        @Provides
        @IntoSet
        fun securityPreferencesOwnerRegistration(
            owner: InstalledProfilePreferenceOwner<SecurityPreferences>,
        ): InstalledProfilePreferenceOwner<*> = owner

        @Provides
        @SingleIn(AppScope::class)
        fun securityPreferences(
            owner: InstalledProfilePreferenceOwner<SecurityPreferences>,
        ): SecurityPreferences = owner.create()

        @Provides
        @SingleIn(AppScope::class)
        fun libraryPreferencesOwner(
            owners: ProfilePreferenceOwnerInstaller,
        ): InstalledProfilePreferenceOwner<LibraryPreferences> =
            owners.register(
                id = ProfilePreferenceOwnerId("app.library"),
                keyPatterns = LibraryPreferences.profileKeyPatterns,
                factory = ::LibraryPreferences,
            )

        @Provides
        @IntoSet
        fun libraryPreferencesOwnerRegistration(
            owner: InstalledProfilePreferenceOwner<LibraryPreferences>,
        ): InstalledProfilePreferenceOwner<*> = owner

        @Provides
        @SingleIn(AppScope::class)
        fun libraryPreferences(
            owner: InstalledProfilePreferenceOwner<LibraryPreferences>,
        ): LibraryPreferences = owner.create()

        @Provides
        @SingleIn(AppScope::class)
        fun duplicatePreferencesOwner(
            owners: ProfilePreferenceOwnerInstaller,
        ): InstalledProfilePreferenceOwner<DuplicatePreferences> =
            owners.register(
                id = ProfilePreferenceOwnerId("app.duplicate-detection"),
                factory = ::DuplicatePreferences,
            )

        @Provides
        @IntoSet
        fun duplicatePreferencesOwnerRegistration(
            owner: InstalledProfilePreferenceOwner<DuplicatePreferences>,
        ): InstalledProfilePreferenceOwner<*> = owner

        @Provides
        @SingleIn(AppScope::class)
        fun duplicatePreferences(
            owner: InstalledProfilePreferenceOwner<DuplicatePreferences>,
        ): DuplicatePreferences = owner.create()

        @Provides
        @SingleIn(AppScope::class)
        fun updatesPreferencesOwner(
            owners: ProfilePreferenceOwnerInstaller,
        ): InstalledProfilePreferenceOwner<UpdatesPreferences> =
            owners.register(
                id = ProfilePreferenceOwnerId("app.updates"),
                factory = ::UpdatesPreferences,
            )

        @Provides
        @IntoSet
        fun updatesPreferencesOwnerRegistration(
            owner: InstalledProfilePreferenceOwner<UpdatesPreferences>,
        ): InstalledProfilePreferenceOwner<*> = owner

        @Provides
        @SingleIn(AppScope::class)
        fun updatesPreferences(
            owner: InstalledProfilePreferenceOwner<UpdatesPreferences>,
        ): UpdatesPreferences = owner.create()

        @Provides
        @SingleIn(AppScope::class)
        fun upcomingPreferencesOwner(
            owners: ProfilePreferenceOwnerInstaller,
        ): InstalledProfilePreferenceOwner<UpcomingPreferences> =
            owners.register(
                id = ProfilePreferenceOwnerId("app.upcoming"),
                factory = ::UpcomingPreferences,
            )

        @Provides
        @IntoSet
        fun upcomingPreferencesOwnerRegistration(
            owner: InstalledProfilePreferenceOwner<UpcomingPreferences>,
        ): InstalledProfilePreferenceOwner<*> = owner

        @Provides
        @SingleIn(AppScope::class)
        fun upcomingPreferences(
            owner: InstalledProfilePreferenceOwner<UpcomingPreferences>,
        ): UpcomingPreferences = owner.create()

        @Provides
        @SingleIn(AppScope::class)
        fun statisticsPreferencesOwner(
            owners: ProfilePreferenceOwnerInstaller,
        ): InstalledProfilePreferenceOwner<StatisticsPreferences> =
            owners.register(
                id = ProfilePreferenceOwnerId("app.statistics"),
                factory = ::StatisticsPreferences,
            )

        @Provides
        @IntoSet
        fun statisticsPreferencesOwnerRegistration(
            owner: InstalledProfilePreferenceOwner<StatisticsPreferences>,
        ): InstalledProfilePreferenceOwner<*> = owner

        @Provides
        @SingleIn(AppScope::class)
        fun statisticsPreferences(
            owner: InstalledProfilePreferenceOwner<StatisticsPreferences>,
        ): StatisticsPreferences = owner.create()

        @Provides
        @SingleIn(AppScope::class)
        fun trackPreferencesOwner(
            registry: ProfilePreferenceOwnerRegistry,
            profileStore: ProfileStore,
        ): InstalledProfilePreferenceOwner<TrackPreferences> =
            ProfilePreferenceOwnerInstaller(registry) { profileStore.privateStore() }.register(
                id = ProfilePreferenceOwnerId("app.tracking"),
                keyPatterns = TrackPreferences.profileKeyPatterns,
                factory = ::TrackPreferences,
            )

        @Provides
        @IntoSet
        fun trackPreferencesOwnerRegistration(
            owner: InstalledProfilePreferenceOwner<TrackPreferences>,
        ): InstalledProfilePreferenceOwner<*> = owner

        @Provides
        @SingleIn(AppScope::class)
        fun trackPreferences(
            owner: InstalledProfilePreferenceOwner<TrackPreferences>,
        ): TrackPreferences = owner.create()

        @Provides
        @SingleIn(AppScope::class)
        fun uiPreferencesOwner(
            owners: ProfilePreferenceOwnerInstaller,
        ): InstalledProfilePreferenceOwner<UiPreferences> =
            owners.register(
                id = ProfilePreferenceOwnerId("app.ui"),
            ) { store -> UiPreferences(store, DeviceUtil.isDynamicColorAvailable) }

        @Provides
        @IntoSet
        fun uiPreferencesOwnerRegistration(
            owner: InstalledProfilePreferenceOwner<UiPreferences>,
        ): InstalledProfilePreferenceOwner<*> = owner

        @Provides
        @SingleIn(AppScope::class)
        fun uiPreferences(owner: InstalledProfilePreferenceOwner<UiPreferences>): UiPreferences = owner.create()

        @Provides
        @SingleIn(AppScope::class)
        fun customPreferencesOwner(
            owners: ProfilePreferenceOwnerInstaller,
        ): InstalledProfilePreferenceOwner<CustomPreferences> =
            owners.register(
                id = ProfilePreferenceOwnerId("app.custom"),
                factory = ::CustomPreferences,
            )

        @Provides
        @IntoSet
        fun customPreferencesOwnerRegistration(
            owner: InstalledProfilePreferenceOwner<CustomPreferences>,
        ): InstalledProfilePreferenceOwner<*> = owner

        @Provides
        @SingleIn(AppScope::class)
        fun customPreferences(
            owner: InstalledProfilePreferenceOwner<CustomPreferences>,
        ): CustomPreferences = owner.create()

        @Provides
        @SingleIn(AppScope::class)
        fun translatorLanguagePreferencesOwner(
            owners: ProfilePreferenceOwnerInstaller,
        ): InstalledProfilePreferenceOwner<TranslatorLanguagePreferences> =
            owners.register(
                id = ProfilePreferenceOwnerId("app.translator"),
                factory = ::TranslatorLanguagePreferences,
            )

        @Provides
        @IntoSet
        fun translatorLanguagePreferencesOwnerRegistration(
            owner: InstalledProfilePreferenceOwner<TranslatorLanguagePreferences>,
        ): InstalledProfilePreferenceOwner<*> = owner

        @Provides
        @SingleIn(AppScope::class)
        fun translatorLanguagePreferences(
            owner: InstalledProfilePreferenceOwner<TranslatorLanguagePreferences>,
        ): TranslatorLanguagePreferences = owner.create()

        @Provides
        @SingleIn(AppScope::class)
        fun networkPreferences(
            @BasePreferenceStore store: PreferenceStore,
        ): NetworkPreferences = NetworkPreferences(preferenceStore = store, verboseLoggingDefault = isDebugBuildType)

        @Provides
        @SingleIn(AppScope::class)
        fun globalSourcePreferences(
            @BasePreferenceStore store: PreferenceStore,
        ): GlobalSourcePreferences = GlobalSourcePreferences(store)

        @Provides
        @SingleIn(AppScope::class)
        fun privacyPreferences(
            @BasePreferenceStore store: PreferenceStore,
        ): PrivacyPreferences = PrivacyPreferences(store)

        @Provides
        @SingleIn(AppScope::class)
        fun globalLibraryPreferences(
            @BasePreferenceStore store: PreferenceStore,
        ): GlobalLibraryPreferences = GlobalLibraryPreferences(store)

        @Provides
        @SingleIn(AppScope::class)
        fun globalTrackPreferences(
            @BasePreferenceStore store: PreferenceStore,
        ): GlobalTrackPreferences = GlobalTrackPreferences(store)

        @Provides
        @SingleIn(AppScope::class)
        fun downloadPreferences(
            @BasePreferenceStore store: PreferenceStore,
        ): DownloadPreferences = DownloadPreferences(store)

        @Provides
        @SingleIn(AppScope::class)
        fun entryTranslatePreferences(
            @BasePreferenceStore store: PreferenceStore,
        ): EntryTranslatePreferences = EntryTranslatePreferences(store)

        @Provides
        @SingleIn(AppScope::class)
        fun backupPreferences(@BasePreferenceStore store: PreferenceStore): BackupPreferences = BackupPreferences(store)

        @Provides
        @SingleIn(AppScope::class)
        fun globalCustomPreferences(
            @BasePreferenceStore store: PreferenceStore,
        ): GlobalCustomPreferences = GlobalCustomPreferences(store)

        @Provides
        @SingleIn(AppScope::class)
        internal fun appUpdatePreferences(
            @BasePreferenceStore store: PreferenceStore,
        ): AppUpdatePreferences = AppUpdatePreferences(store)

        @Provides
        @SingleIn(AppScope::class)
        fun progressiveImagePreferences(
            @BasePreferenceStore store: PreferenceStore,
        ): ProgressiveImagePreferences = ProgressiveImagePreferences(store)

        @Provides
        @SingleIn(AppScope::class)
        fun profileSourcePreferences(profileStore: ProfileStore, json: Json): ProfileSourcePreferences =
            ProfileSourcePreferences(profileStore, json)

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
