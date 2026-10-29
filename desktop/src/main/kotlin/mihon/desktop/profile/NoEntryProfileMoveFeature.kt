package mihon.desktop.profile

import dev.zacsweers.metro.Inject
import mihon.entry.interactions.lifecycle.profile.EntryProfileMoveConflictResolution
import mihon.entry.interactions.lifecycle.profile.EntryProfileMoveFeature
import mihon.entry.interactions.lifecycle.profile.EntryProfileMovePreview
import mihon.entry.interactions.lifecycle.profile.EntryProfileMoveRequest
import mihon.entry.interactions.lifecycle.profile.EntryProfileMoveResult

/**
 * Stands in for moving entries between profiles, which needs the download features desktop does not host yet. It
 * reports itself not offered, so the library hides the move; asked anyway, it fails before changing anything. The
 * shared feature replaces this once desktop hosts the download features.
 */
@Inject
class NoEntryProfileMoveFeature : EntryProfileMoveFeature {
    override val isOffered: Boolean = false

    override suspend fun preview(request: EntryProfileMoveRequest): EntryProfileMovePreview =
        throw UnsupportedOperationException("Desktop cannot move entries between profiles")

    override suspend fun execute(
        preview: EntryProfileMovePreview,
        resolutions: Map<Long, EntryProfileMoveConflictResolution>,
    ): EntryProfileMoveResult = throw UnsupportedOperationException("Desktop cannot move entries between profiles")
}
