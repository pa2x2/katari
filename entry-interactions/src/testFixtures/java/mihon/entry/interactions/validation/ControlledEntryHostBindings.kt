package mihon.entry.interactions.validation

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import io.mockk.mockk
import mihon.core.metro.BasePreferenceStore
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
import mihon.entry.interactions.media.EntryViewerSettingsScreenProjection
import mihon.entry.interactions.media.EntryViewerSettingsScreenProjectionResolver
import mihon.entry.interactions.merge.host.EntryMergeHost
import mihon.entry.interactions.migration.host.EntryMigrationConsequenceHost
import mihon.entry.interactions.migration.host.EntryMigrationCustomCoverHost
import mihon.entry.interactions.migration.host.EntryMigrationExecutionHost
import mihon.entry.interactions.migration.host.EntryMigrationPreparationHost
import mihon.entry.interactions.source.EntryLibraryTitleUpdatePolicy
import mihon.entry.interactions.tracking.host.EntryTrackingHost
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.core.common.preference.PreferenceStore

/** Host services for production validation graphs, which run the real Features without the app behind them. */
@BindingContainer
object ControlledEntryHostBindings {
    @Provides
    fun libraryMembershipHost(): EntryLibraryMembershipHost = mockk(relaxed = true)

    @Provides
    fun libraryCustomCoverHost(): EntryLibraryCustomCoverHost = mockk(relaxed = true)

    @Provides
    fun destructiveRemovalHost(): EntryDestructiveRemovalHost = mockk(relaxed = true)

    @Provides
    fun destructiveRemovalCustomCoverHost(): EntryDestructiveRemovalCustomCoverHost = mockk(relaxed = true)

    @Provides
    fun profileMoveHost(): EntryProfileMoveHost = mockk(relaxed = true)

    @Provides
    fun profileMoveSourceVisibilityHost(): EntryProfileMoveSourceVisibilityHost = mockk(relaxed = true)

    @Provides
    fun profileMoveCustomCoverHost(): EntryProfileMoveCustomCoverHost = mockk(relaxed = true)

    @Provides
    fun profileMoveTrackingStateHost(): EntryProfileMoveTrackingStateHost = mockk(relaxed = true)

    @Provides
    fun profileMoveChildGroupFilterStateHost(): EntryProfileMoveChildGroupFilterStateHost = mockk(relaxed = true)

    @Provides
    fun profileMoveCoverHashStateHost(): EntryProfileMoveCoverHashStateHost = mockk(relaxed = true)

    @Provides
    fun mergeHost(): EntryMergeHost = mockk(relaxed = true)

    @Provides
    fun migrationPreparationHost(): EntryMigrationPreparationHost = mockk(relaxed = true)

    @Provides
    fun migrationExecutionHost(): EntryMigrationExecutionHost = mockk(relaxed = true)

    @Provides
    fun migrationConsequenceHost(): EntryMigrationConsequenceHost = mockk(relaxed = true)

    @Provides
    fun migrationCustomCoverHost(): EntryMigrationCustomCoverHost = mockk(relaxed = true)

    @Provides
    fun trackingHost(): EntryTrackingHost = mockk(relaxed = true)

    @Provides
    fun viewerSettingsScreenProjectionResolver(): EntryViewerSettingsScreenProjectionResolver =
        EntryViewerSettingsScreenProjectionResolver { surfaceIds -> surfaceIds.map(::viewerSettingsProjection) }

    @Provides
    fun libraryTitleUpdatePolicy(): EntryLibraryTitleUpdatePolicy = EntryLibraryTitleUpdatePolicy { false }

    @Provides
    @BasePreferenceStore
    fun basePreferenceStore(): PreferenceStore = InMemoryPreferenceStore()
}

private fun viewerSettingsProjection(id: String): EntryViewerSettingsScreenProjection {
    return object : EntryViewerSettingsScreenProjection {
        override val surfaceId = id
    }
}
