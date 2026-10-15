package mihon.entry.interactions.host

import android.app.Application
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.domain.source.interactor.GetIncognitoState
import eu.kanade.domain.track.interactor.AddTracks
import eu.kanade.domain.track.interactor.RefreshTracks
import eu.kanade.domain.track.interactor.SyncChapterProgressWithTrack
import eu.kanade.domain.track.interactor.TrackChapter
import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.presentation.more.settings.screen.productionEntryViewerSettingsScreenProjectionResolver
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.entry.AppEntryChildGroupFilterDataSource
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.entry.AppEntryDownloadNotificationActions
import eu.kanade.tachiyomi.entry.AppMediaSessionIncognitoState
import eu.kanade.tachiyomi.ui.base.delegate.ThemingDelegateImpl
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
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.library.service.DuplicatePreferences
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.track.interactor.DeleteTrack
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.domain.track.interactor.GetTracksPerEntry

/**
 * The app's implementations of the services Entry Features need from their host. Validation graphs supply controlled
 * ones instead, so these are not contributed.
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
        fun mergeHost(
            handler: DatabaseHandler,
            duplicatePreferences: DuplicatePreferences,
            libraryPreferences: LibraryPreferences,
        ): EntryMergeHost = AppEntryMergeHost(
            handler = handler,
            duplicateCandidates = AppEntryMergeDuplicateCandidateHost(handler, duplicatePreferences),
            defaultChildFlags = {
                Entry.SHOW_ALL or
                    libraryPreferences.sortChapterBySourceOrNumber.get() or
                    libraryPreferences.displayChapterByNameOrNumber.get() or
                    libraryPreferences.sortChapterByAscendingOrDescending.get()
            },
        )

        @Provides
        @SingleIn(AppScope::class)
        internal fun migrationHost(handler: DatabaseHandler, coverCache: CoverCache): AppEntryMigrationHost =
            AppEntryMigrationHost(handler = handler, hasCustomCover = coverCache::hasCustomCover)

        @Provides
        @SingleIn(AppScope::class)
        fun migrationCustomCoverHost(application: Application, coverCache: CoverCache): EntryMigrationCustomCoverHost =
            AppEntryMigrationCustomCoverHost(application, coverCache)

        @Provides
        @SingleIn(AppScope::class)
        fun libraryMembershipHost(handler: DatabaseHandler, profileStore: ProfileStore): EntryLibraryMembershipHost =
            AppEntryLibraryMembershipHost(handler, profileStore)

        @Provides
        @SingleIn(AppScope::class)
        fun libraryCustomCoverHost(coverCache: CoverCache, handler: DatabaseHandler): EntryLibraryCustomCoverHost =
            AppEntryLibraryCustomCoverHost(coverCache, handler)

        @Provides
        @SingleIn(AppScope::class)
        fun destructiveRemovalHost(handler: DatabaseHandler): EntryDestructiveRemovalHost =
            AppEntryDestructiveRemovalHost(handler)

        @Provides
        @SingleIn(AppScope::class)
        fun destructiveRemovalCustomCoverHost(coverCache: CoverCache): EntryDestructiveRemovalCustomCoverHost =
            AppEntryDestructiveRemovalCustomCoverHost(coverCache)

        @Provides
        @SingleIn(AppScope::class)
        fun profileMoveHost(handler: DatabaseHandler): EntryProfileMoveHost = AppEntryProfileMoveHost(handler)

        @Provides
        @SingleIn(AppScope::class)
        fun profileMoveSourceVisibilityHost(profileStore: ProfileStore): EntryProfileMoveSourceVisibilityHost =
            AppEntryProfileMoveSourceVisibilityHost(profileStore)

        @Provides
        @SingleIn(AppScope::class)
        fun profileMoveCustomCoverHost(coverCache: CoverCache): EntryProfileMoveCustomCoverHost =
            AppEntryProfileMoveCustomCoverHost(coverCache)

        @Provides
        @SingleIn(AppScope::class)
        fun profileMoveTrackingStateHost(handler: DatabaseHandler): EntryProfileMoveTrackingStateHost =
            AppEntryProfileMoveTrackingStateHost(handler)

        @Provides
        @SingleIn(AppScope::class)
        fun profileMoveChildGroupFilterStateHost(handler: DatabaseHandler): EntryProfileMoveChildGroupFilterStateHost =
            AppEntryProfileMoveChildGroupFilterStateHost(handler)

        @Provides
        @SingleIn(AppScope::class)
        fun profileMoveCoverHashStateHost(handler: DatabaseHandler): EntryProfileMoveCoverHashStateHost =
            AppEntryProfileMoveCoverHashStateHost(handler)

        @Provides
        @SingleIn(AppScope::class)
        fun trackingHost(
            application: Application,
            trackerManager: TrackerManager,
            sourceManager: SourceManager,
            getTracks: GetTracks,
            handler: DatabaseHandler,
            getTracksPerEntry: GetTracksPerEntry,
            refreshTracks: RefreshTracks,
            deleteTrack: DeleteTrack,
            addTracks: AddTracks,
            trackChapter: TrackChapter,
            syncChapterProgress: () -> SyncChapterProgressWithTrack,
            trackPreferences: TrackPreferences,
        ): EntryTrackingHost = AppEntryTrackingHost(
            trackerManager = trackerManager,
            sourceManager = sourceManager,
            getTracks = getTracks,
            handler = handler,
            getTracksPerEntry = getTracksPerEntry,
            refreshTracks = refreshTracks,
            deleteTrack = deleteTrack,
            app = application,
            addTracks = addTracks,
            trackChapter = trackChapter,
            syncChapterProgress = syncChapterProgress,
            trackPreferences = trackPreferences,
        )

        @Provides
        fun viewerSettingsScreenProjectionResolver(): EntryViewerSettingsScreenProjectionResolver =
            productionEntryViewerSettingsScreenProjectionResolver()

        @Provides
        fun libraryTitleUpdatePolicy(profileStore: ProfileStore): EntryLibraryTitleUpdatePolicy =
            EntryLibraryTitleUpdatePolicy { profileId ->
                LibraryPreferences(profileStore.profileStore(profileId)).updateMangaTitles.get()
            }

        @Provides
        @SingleIn(AppScope::class)
        fun activityTheme(): EntryInteractionActivityTheme =
            EntryInteractionActivityTheme(ThemingDelegateImpl()::applyAppTheme)

        @Provides
        @SingleIn(AppScope::class)
        fun notificationActions(): EntryDownloadNotificationActions = AppEntryDownloadNotificationActions()

        @Provides
        @SingleIn(AppScope::class)
        fun childGroupFilterDataSource(
            entryChapterRepository: EntryChapterRepository,
            handler: DatabaseHandler,
            profileProvider: ActiveProfileProvider,
        ): EntryChildGroupFilterDataSource =
            AppEntryChildGroupFilterDataSource(entryChapterRepository, handler, profileProvider)

        @Provides
        @SingleIn(AppScope::class)
        fun mediaSessionIncognitoState(getIncognitoState: GetIncognitoState): EntryMediaSessionIncognitoState =
            AppMediaSessionIncognitoState(getIncognitoState)
    }
}
