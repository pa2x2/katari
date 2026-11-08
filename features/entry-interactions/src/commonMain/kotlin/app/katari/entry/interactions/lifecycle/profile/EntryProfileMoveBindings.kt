package app.katari.entry.interactions.lifecycle.profile

import app.katari.entry.interactions.download.EntryDownloadMaintenanceFeature
import app.katari.entry.interactions.lifecycle.profile.host.EntryProfileMoveCoverHashStateHost
import app.katari.entry.interactions.lifecycle.profile.host.EntryProfileMoveCustomCoverHost
import app.katari.entry.interactions.lifecycle.profile.host.EntryProfileMoveHost
import app.katari.entry.interactions.lifecycle.profile.host.EntryProfileMoveScanlatorFilterStateHost
import app.katari.entry.interactions.lifecycle.profile.host.EntryProfileMoveSourceVisibilityHost
import app.katari.entry.interactions.merge.EntryMergeProfileMoveFeature
import app.katari.entry.interactions.runtime.EntryScanlatorFilterProcessor
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.source.entry.EntryType

@ContributesTo(AppScope::class)
@BindingContainer
object EntryProfileMoveBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun feature(
        profileMoveHost: EntryProfileMoveHost,
        mergeProfileMove: () -> EntryMergeProfileMoveFeature,
        downloads: () -> EntryDownloadMaintenanceFeature,
        scanlatorFilterStateHost: EntryProfileMoveScanlatorFilterStateHost,
        scanlatorFilterProcessors: Map<EntryType, EntryScanlatorFilterProcessor>,
        coverHashStateHost: EntryProfileMoveCoverHashStateHost,
        customCoverHost: EntryProfileMoveCustomCoverHost,
        sourceVisibilityHost: EntryProfileMoveSourceVisibilityHost,
    ): EntryProfileMoveFeature = EntryProfileMoveCoordinator(
        host = profileMoveHost,
        mergeProfileMove = mergeProfileMove,
        downloads = downloads,
        scanlatorFilterStateHost = scanlatorFilterStateHost,
        scanlatorFilterTypes = scanlatorFilterProcessors.keys,
        coverHashStateHost = coverHashStateHost,
        customCoverHost = customCoverHost,
        sourceVisibilityHost = sourceVisibilityHost,
    )
}
