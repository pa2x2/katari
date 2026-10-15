package mihon.entry.interactions.validation

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import eu.kanade.domain.ui.UiPreferences
import io.mockk.mockk
import tachiyomi.domain.entry.repository.MergedMemberExpansionRepository
import tachiyomi.domain.entry.service.EntryCustomCoverWriter
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.source.service.SourceInfoNames
import tachiyomi.domain.track.repository.TrackRepository
import tachiyomi.source.local.image.LocalCoverManager

/**
 * App services that only Android-constructed components (activities and their view models, workers, widgets) read
 * through their graph entry points. Validation never starts those components, so these only have to satisfy the graph.
 */
@BindingContainer
object ControlledEntryPointBindings {
    @Provides
    fun uiPreferences(): UiPreferences = mockk(relaxed = true)

    @Provides
    fun libraryPreferences(): LibraryPreferences = mockk(relaxed = true)

    @Provides
    fun trackRepository(): TrackRepository = mockk(relaxed = true)

    @Provides
    fun mergedMemberExpansionRepository(): MergedMemberExpansionRepository = mockk(relaxed = true)

    @Provides
    fun sourceInfoNames(): SourceInfoNames = mockk(relaxed = true)

    @Provides
    fun entryCustomCoverWriter(): EntryCustomCoverWriter = mockk(relaxed = true)

    @Provides
    fun localCoverManager(): LocalCoverManager = mockk(relaxed = true)
}
