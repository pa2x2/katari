package mihon.entry.interactions.lifecycle.profile

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.download.EntryDownloadMaintenanceFeature
import mihon.entry.interactions.lifecycle.profile.host.EntryProfileMoveChildGroupFilterStateHost
import mihon.entry.interactions.lifecycle.profile.host.EntryProfileMoveCoverHashStateHost
import mihon.entry.interactions.lifecycle.profile.host.EntryProfileMoveCustomCoverHost
import mihon.entry.interactions.lifecycle.profile.host.EntryProfileMoveHost
import mihon.entry.interactions.lifecycle.profile.host.EntryProfileMoveSourceVisibilityHost
import mihon.entry.interactions.lifecycle.profile.host.EntryProfileMoveTrackingStateHost
import mihon.entry.interactions.merge.EntryMergeProfileMoveFeature
import mihon.entry.interactions.runtime.EntryChildGroupFilterProcessor

@ContributesTo(AppScope::class)
@BindingContainer
object EntryProfileMoveBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun feature(
        profileMoveHost: EntryProfileMoveHost,
        mergeProfileMove: () -> EntryMergeProfileMoveFeature,
        downloads: () -> EntryDownloadMaintenanceFeature,
        childGroupFilterStateHost: EntryProfileMoveChildGroupFilterStateHost,
        childGroupFilterProcessors: Map<EntryType, EntryChildGroupFilterProcessor>,
        coverHashStateHost: EntryProfileMoveCoverHashStateHost,
        trackingStateHost: EntryProfileMoveTrackingStateHost,
        customCoverHost: EntryProfileMoveCustomCoverHost,
        sourceVisibilityHost: EntryProfileMoveSourceVisibilityHost,
    ): EntryProfileMoveFeature = EntryProfileMoveCoordinator(
        host = profileMoveHost,
        mergeProfileMove = mergeProfileMove,
        downloads = downloads,
        childGroupFilterStateHost = childGroupFilterStateHost,
        childGroupFilterTypes = childGroupFilterProcessors.keys,
        coverHashStateHost = coverHashStateHost,
        trackingStateHost = trackingStateHost,
        customCoverHost = customCoverHost,
        sourceVisibilityHost = sourceVisibilityHost,
    )
}
