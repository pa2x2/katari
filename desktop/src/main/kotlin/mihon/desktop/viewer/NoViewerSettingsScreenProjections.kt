package mihon.desktop.viewer

import dev.zacsweers.metro.Inject
import mihon.entry.interactions.media.EntryViewerSettingsScreenProjection
import mihon.entry.interactions.media.EntryViewerSettingsScreenProjectionResolver

/**
 * Desktop has no entry type that ships a viewer, so no viewer settings surface needs a screen; the shared resolver
 * would name screens for surfaces no viewer declares here. Stands in until desktop has viewers.
 */
@Inject
class NoViewerSettingsScreenProjections : EntryViewerSettingsScreenProjectionResolver {
    override fun resolve(surfaceIds: Set<String>): Collection<EntryViewerSettingsScreenProjection> = emptyList()
}
