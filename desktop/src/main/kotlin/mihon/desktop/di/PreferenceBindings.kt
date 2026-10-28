package mihon.desktop.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.domain.source.service.ProfileSourcePreferences
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.domain.track.service.GlobalTrackPreferences
import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.tachiyomi.network.NetworkPreferences
import kotlinx.serialization.json.Json
import mihon.feature.profiles.core.ProfileStore
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.domain.backup.service.BackupPreferences
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.upcoming.service.UpcomingPreferences
import tachiyomi.domain.updates.service.UpdatesPreferences

/**
 * The preference classes on the store Android keeps each in: what follows a profile on that profile's store, tracker
 * accounts on its private store, and what every profile shares on the base store.
 */
@BindingContainer
object PreferenceBindings {
    // On Android, too, the unqualified store is the base store's file.
    @Provides
    fun preferenceStore(profileStore: ProfileStore): PreferenceStore = profileStore.basePreferenceStore()

    @Provides
    @SingleIn(AppScope::class)
    fun uiPreferences(profileStore: ProfileStore): UiPreferences = UiPreferences(profileStore.profileStore())

    @Provides
    @SingleIn(AppScope::class)
    fun libraryPreferences(profileStore: ProfileStore): LibraryPreferences =
        LibraryPreferences(profileStore.profileStore())

    @Provides
    @SingleIn(AppScope::class)
    fun updatesPreferences(profileStore: ProfileStore): UpdatesPreferences =
        UpdatesPreferences(profileStore.profileStore())

    @Provides
    @SingleIn(AppScope::class)
    fun upcomingPreferences(profileStore: ProfileStore): UpcomingPreferences =
        UpcomingPreferences(profileStore.profileStore())

    @Provides
    @SingleIn(AppScope::class)
    fun sourcePreferences(profileStore: ProfileStore, json: Json): SourcePreferences =
        SourcePreferences(profileStore.profileStore(), json)

    @Provides
    @SingleIn(AppScope::class)
    fun profileSourcePreferences(profileStore: ProfileStore, json: Json): ProfileSourcePreferences =
        ProfileSourcePreferences(profileStore, json)

    @Provides
    @SingleIn(AppScope::class)
    fun trackPreferences(profileStore: ProfileStore): TrackPreferences = TrackPreferences(profileStore.privateStore())

    @Provides
    @SingleIn(AppScope::class)
    fun downloadPreferences(profileStore: ProfileStore): DownloadPreferences =
        DownloadPreferences(profileStore.basePreferenceStore())

    @Provides
    @SingleIn(AppScope::class)
    fun globalTrackPreferences(profileStore: ProfileStore): GlobalTrackPreferences =
        GlobalTrackPreferences(profileStore.basePreferenceStore())

    @Provides
    @SingleIn(AppScope::class)
    fun networkPreferences(profileStore: ProfileStore): NetworkPreferences =
        NetworkPreferences(profileStore.basePreferenceStore())

    @Provides
    @SingleIn(AppScope::class)
    fun backupPreferences(profileStore: ProfileStore): BackupPreferences =
        BackupPreferences(profileStore.basePreferenceStore())
}
