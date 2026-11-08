package app.katari.entry.interactions.host

import app.katari.core.platform.storage.AppPrivateDirectories
import app.katari.entry.interactions.host.library.AppEntryLibraryCustomCoverHost
import app.katari.entry.interactions.host.library.AppEntryLibraryMembershipHost
import app.katari.entry.interactions.host.lifecycle.profile.AppEntryProfileMoveCoverHashStateHost
import app.katari.entry.interactions.host.lifecycle.profile.AppEntryProfileMoveCustomCoverHost
import app.katari.entry.interactions.host.lifecycle.profile.AppEntryProfileMoveHost
import app.katari.entry.interactions.host.lifecycle.profile.AppEntryProfileMoveScanlatorFilterStateHost
import app.katari.entry.interactions.host.lifecycle.profile.AppEntryProfileMoveSourceVisibilityHost
import app.katari.entry.interactions.host.lifecycle.removal.AppEntryDestructiveRemovalCustomCoverHost
import app.katari.entry.interactions.host.lifecycle.removal.AppEntryDestructiveRemovalHost
import app.katari.entry.interactions.host.migration.AppEntryMigrationCustomCoverHost
import app.katari.entry.interactions.host.migration.AppEntryMigrationHost
import app.katari.entry.interactions.library.membership.host.EntryLibraryCustomCoverHost
import app.katari.entry.interactions.library.membership.host.EntryLibraryMembershipHost
import app.katari.entry.interactions.lifecycle.profile.host.EntryProfileMoveCoverHashStateHost
import app.katari.entry.interactions.lifecycle.profile.host.EntryProfileMoveCustomCoverHost
import app.katari.entry.interactions.lifecycle.profile.host.EntryProfileMoveHost
import app.katari.entry.interactions.lifecycle.profile.host.EntryProfileMoveScanlatorFilterStateHost
import app.katari.entry.interactions.lifecycle.profile.host.EntryProfileMoveSourceVisibilityHost
import app.katari.entry.interactions.lifecycle.removal.host.EntryDestructiveRemovalCustomCoverHost
import app.katari.entry.interactions.lifecycle.removal.host.EntryDestructiveRemovalHost
import app.katari.entry.interactions.media.session.EntryMediaSessionIncognitoState
import app.katari.entry.interactions.merge.host.EntryMergeHost
import app.katari.entry.interactions.migration.host.EntryMigrationConsequenceHost
import app.katari.entry.interactions.migration.host.EntryMigrationCustomCoverHost
import app.katari.entry.interactions.migration.host.EntryMigrationExecutionHost
import app.katari.entry.interactions.migration.host.EntryMigrationPreparationHost
import app.katari.entry.interactions.runtime.EntryScanlatorFilterDataSource
import app.katari.profiles.core.ProfileStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.domain.source.interactor.GetIncognitoState
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.entry.AppEntryScanlatorFilterDataSource
import eu.kanade.tachiyomi.entry.AppMediaSessionIncognitoState
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.service.EntryCustomCoverWriter
import tachiyomi.domain.library.service.DuplicatePreferences
import tachiyomi.domain.library.service.LibraryPreferences

/**
 * The app's implementations of the services Entry Features need from their host, over its database, on every
 * platform. Each application graph lists it; validation graphs supply controlled ones instead, so these are not
 * contributed.
 */
@BindingContainer
abstract class AppEntryHostBindings {
    @Binds
    internal abstract val AppEntryMigrationHost.preparationHost: EntryMigrationPreparationHost

    @Binds
    internal abstract val AppEntryMigrationHost.executionHost: EntryMigrationExecutionHost

    @Binds
    internal abstract val AppEntryMigrationHost.consequenceHost: EntryMigrationConsequenceHost

    @Binds
    abstract val CoverCache.customCoverWriter: EntryCustomCoverWriter

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
            defaultChildFlags = libraryPreferences::defaultChapterFlags,
        )

        @Provides
        @SingleIn(AppScope::class)
        internal fun migrationHost(handler: DatabaseHandler, coverCache: CoverCache): AppEntryMigrationHost =
            AppEntryMigrationHost(handler = handler, hasCustomCover = coverCache::hasCustomCover)

        @Provides
        @SingleIn(AppScope::class)
        fun migrationCustomCoverHost(
            directories: AppPrivateDirectories,
            coverCache: CoverCache,
        ): EntryMigrationCustomCoverHost = AppEntryMigrationCustomCoverHost(directories, coverCache)

        @Provides
        @SingleIn(AppScope::class)
        fun libraryMembershipHost(handler: DatabaseHandler, profileStore: ProfileStore): EntryLibraryMembershipHost =
            AppEntryLibraryMembershipHost(handler, profileStore)

        @Provides
        @SingleIn(AppScope::class)
        fun libraryCustomCoverHost(coverCache: CoverCache): EntryLibraryCustomCoverHost =
            AppEntryLibraryCustomCoverHost(coverCache)

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
        fun profileMoveScanlatorFilterStateHost(handler: DatabaseHandler): EntryProfileMoveScanlatorFilterStateHost =
            AppEntryProfileMoveScanlatorFilterStateHost(handler)

        @Provides
        @SingleIn(AppScope::class)
        fun profileMoveCoverHashStateHost(handler: DatabaseHandler): EntryProfileMoveCoverHashStateHost =
            AppEntryProfileMoveCoverHashStateHost(handler)

        @Provides
        @SingleIn(AppScope::class)
        fun scanlatorFilterDataSource(
            entryChapterRepository: EntryChapterRepository,
            handler: DatabaseHandler,
            profileProvider: ActiveProfileProvider,
        ): EntryScanlatorFilterDataSource =
            AppEntryScanlatorFilterDataSource(entryChapterRepository, handler, profileProvider)

        @Provides
        @SingleIn(AppScope::class)
        fun mediaSessionIncognitoState(getIncognitoState: GetIncognitoState): EntryMediaSessionIncognitoState =
            AppMediaSessionIncognitoState(getIncognitoState)
    }
}
