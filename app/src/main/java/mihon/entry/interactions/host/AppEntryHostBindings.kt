package mihon.entry.interactions.host

import android.app.Application
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.presentation.more.settings.screen.productionEntryViewerSettingsScreenProjectionResolver
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.entry.AppEntryChildGroupFilterDataSource
import eu.kanade.tachiyomi.entry.AppEntryDownloadNotificationActions
import eu.kanade.tachiyomi.entry.AppMediaSessionIncognitoState
import eu.kanade.tachiyomi.ui.base.delegate.ThemingDelegateImpl
import mihon.core.metro.BasePreferenceStore
import mihon.entry.interactions.download.EntryDownloadNotificationActions
import mihon.entry.interactions.host.library.AppEntryLibraryCustomCoverHost
import mihon.entry.interactions.host.library.AppEntryLibraryMembershipHost
import mihon.entry.interactions.host.lifecycle.profile.AppEntryProfileMoveChildGroupFilterStateHost
import mihon.entry.interactions.host.lifecycle.profile.AppEntryProfileMoveCoverHashStateHost
import mihon.entry.interactions.host.lifecycle.profile.AppEntryProfileMoveCustomCoverHost
import mihon.entry.interactions.host.lifecycle.profile.AppEntryProfileMoveHost
import mihon.entry.interactions.host.lifecycle.profile.AppEntryProfileMoveSourceVisibilityHost
import mihon.entry.interactions.host.lifecycle.profile.AppEntryProfileMoveTrackingStateHost
import mihon.entry.interactions.host.lifecycle.removal.AppEntryDestructiveRemovalCustomCoverHost
import mihon.entry.interactions.host.lifecycle.removal.AppEntryDestructiveRemovalHost
import mihon.entry.interactions.host.migration.AppEntryMigrationCustomCoverHost
import mihon.entry.interactions.host.migration.AppEntryMigrationHost
import mihon.entry.interactions.host.tracking.AppEntryTrackingHost
import mihon.entry.interactions.library.membership.host.EntryLibraryCustomCoverHost
import mihon.entry.interactions.library.membership.host.EntryLibraryMembershipHost
import mihon.entry.interactions.lifecycle.profile.host.EntryProfileMoveChildGroupFilterStateHost
import mihon.entry.interactions.lifecycle.profile.host.EntryProfileMoveCoverHashStateHost
import mihon.entry.interactions.lifecycle.profile.host.EntryProfileMoveCustomCoverHost
import mihon.entry.interactions.lifecycle.profile.host.EntryProfileMoveHost
import mihon.entry.interactions.lifecycle.profile.host.EntryProfileMoveSourceVisibilityHost
import mihon.entry.interactions.lifecycle.profile.host.EntryProfileMoveTrackingStateHost
import mihon.entry.interactions.lifecycle.removal.host.EntryDestructiveRemovalCustomCoverHost
import mihon.entry.interactions.lifecycle.removal.host.EntryDestructiveRemovalHost
import mihon.entry.interactions.media.EntryViewerSettingsScreenProjectionResolver
import mihon.entry.interactions.media.session.EntryMediaSessionIncognitoState
import mihon.entry.interactions.merge.host.EntryMergeHost
import mihon.entry.interactions.migration.host.EntryMigrationConsequenceHost
import mihon.entry.interactions.migration.host.EntryMigrationCustomCoverHost
import mihon.entry.interactions.migration.host.EntryMigrationExecutionHost
import mihon.entry.interactions.migration.host.EntryMigrationPreparationHost
import mihon.entry.interactions.runtime.EntryChildGroupFilterDataSource
import mihon.entry.interactions.runtime.EntryInteractionActivityTheme
import mihon.entry.interactions.source.EntryLibraryTitleUpdatePolicy
import mihon.entry.interactions.tracking.host.EntryTrackingHost
import mihon.feature.profiles.core.ProfileStore
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.library.service.DuplicatePreferences
import tachiyomi.domain.library.service.LibraryPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * The app's implementations of the services Entry Features need from their host. The app services they wrap are
 * still read from Injekt until those move into the graph.
 */
@BindingContainer
abstract class AppEntryHostBindings {
    @Binds
    internal abstract val AppEntryMigrationHost.preparationHost: EntryMigrationPreparationHost

    @Binds
    internal abstract val AppEntryMigrationHost.executionHost: EntryMigrationExecutionHost

    @Binds
    internal abstract val AppEntryMigrationHost.consequenceHost: EntryMigrationConsequenceHost

    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun mergeHost(): EntryMergeHost = AppEntryMergeHost(
            handler = Injekt.get(),
            duplicateCandidates = AppEntryMergeDuplicateCandidateHost(Injekt.get(), Injekt.get<DuplicatePreferences>()),
            defaultChildFlags = {
                val preferences = Injekt.get<LibraryPreferences>()
                Entry.SHOW_ALL or
                    preferences.sortChapterBySourceOrNumber.get() or
                    preferences.displayChapterByNameOrNumber.get() or
                    preferences.sortChapterByAscendingOrDescending.get()
            },
        )

        @Provides
        @SingleIn(AppScope::class)
        internal fun migrationHost(): AppEntryMigrationHost = AppEntryMigrationHost(
            handler = Injekt.get(),
            hasCustomCover = { entryId -> Injekt.get<CoverCache>().hasCustomCover(entryId) },
        )

        @Provides
        @SingleIn(AppScope::class)
        fun migrationCustomCoverHost(application: Application): EntryMigrationCustomCoverHost =
            AppEntryMigrationCustomCoverHost(application, Injekt.get())

        @Provides
        @SingleIn(AppScope::class)
        fun libraryMembershipHost(): EntryLibraryMembershipHost =
            AppEntryLibraryMembershipHost(Injekt.get(), Injekt.get<ProfileStore>())

        @Provides
        @SingleIn(AppScope::class)
        fun libraryCustomCoverHost(): EntryLibraryCustomCoverHost =
            AppEntryLibraryCustomCoverHost(Injekt.get(), Injekt.get())

        @Provides
        @SingleIn(AppScope::class)
        fun destructiveRemovalHost(): EntryDestructiveRemovalHost = AppEntryDestructiveRemovalHost(Injekt.get())

        @Provides
        @SingleIn(AppScope::class)
        fun destructiveRemovalCustomCoverHost(): EntryDestructiveRemovalCustomCoverHost =
            AppEntryDestructiveRemovalCustomCoverHost(Injekt.get())

        @Provides
        @SingleIn(AppScope::class)
        fun profileMoveHost(): EntryProfileMoveHost = AppEntryProfileMoveHost(Injekt.get())

        @Provides
        @SingleIn(AppScope::class)
        fun profileMoveSourceVisibilityHost(): EntryProfileMoveSourceVisibilityHost =
            AppEntryProfileMoveSourceVisibilityHost(Injekt.get())

        @Provides
        @SingleIn(AppScope::class)
        fun profileMoveCustomCoverHost(): EntryProfileMoveCustomCoverHost =
            AppEntryProfileMoveCustomCoverHost(Injekt.get())

        @Provides
        @SingleIn(AppScope::class)
        fun profileMoveTrackingStateHost(): EntryProfileMoveTrackingStateHost =
            AppEntryProfileMoveTrackingStateHost(Injekt.get())

        @Provides
        @SingleIn(AppScope::class)
        fun profileMoveChildGroupFilterStateHost(): EntryProfileMoveChildGroupFilterStateHost =
            AppEntryProfileMoveChildGroupFilterStateHost(Injekt.get())

        @Provides
        @SingleIn(AppScope::class)
        fun profileMoveCoverHashStateHost(): EntryProfileMoveCoverHashStateHost =
            AppEntryProfileMoveCoverHashStateHost(Injekt.get())

        @Provides
        @SingleIn(AppScope::class)
        fun trackingHost(application: Application): EntryTrackingHost = AppEntryTrackingHost(
            trackerManager = Injekt.get(),
            sourceManager = Injekt.get(),
            getTracks = Injekt.get(),
            handler = Injekt.get(),
            getTracksPerEntry = Injekt.get(),
            refreshTracks = Injekt.get(),
            deleteTrack = Injekt.get(),
            app = application,
            addTracks = Injekt.get(),
            trackChapter = Injekt.get(),
            syncChapterProgress = { Injekt.get() },
            trackPreferences = Injekt.get(),
        )

        @Provides
        fun viewerSettingsScreenProjectionResolver(): EntryViewerSettingsScreenProjectionResolver =
            productionEntryViewerSettingsScreenProjectionResolver()

        @Provides
        fun libraryTitleUpdatePolicy(): EntryLibraryTitleUpdatePolicy = EntryLibraryTitleUpdatePolicy { profileId ->
            LibraryPreferences(Injekt.get<ProfileStore>().profileStore(profileId)).updateMangaTitles.get()
        }

        @Provides
        @BasePreferenceStore
        fun basePreferenceStore(): PreferenceStore = Injekt.get<ProfileStore>().basePreferenceStore()

        @Provides
        @SingleIn(AppScope::class)
        fun activityTheme(): EntryInteractionActivityTheme =
            EntryInteractionActivityTheme(ThemingDelegateImpl()::applyAppTheme)

        @Provides
        @SingleIn(AppScope::class)
        fun notificationActions(): EntryDownloadNotificationActions = AppEntryDownloadNotificationActions()

        @Provides
        @SingleIn(AppScope::class)
        fun childGroupFilterDataSource(): EntryChildGroupFilterDataSource =
            AppEntryChildGroupFilterDataSource(Injekt.get(), Injekt.get(), Injekt.get())

        @Provides
        @SingleIn(AppScope::class)
        fun mediaSessionIncognitoState(): EntryMediaSessionIncognitoState = AppMediaSessionIncognitoState(Injekt.get())
    }
}
