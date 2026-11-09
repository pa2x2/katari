package app.katari.entry.interactions.media

import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import tachiyomi.domain.entry.model.Entry

/**
 * Feature-owned boundary for immersive source entry, loading, rendering, and lifecycle. The feed is a preview: it starts
 * media where it was left and records nothing, neither progress nor History.
 */
interface EntryImmersiveFeature {
    fun sourceAvailability(source: UnifiedSource?): EntryImmersiveSourceAvailability

    fun availability(context: EntryImmersiveContext): EntryImmersiveAvailability

    fun preloadRadius(type: EntryType): EntryImmersivePreloadRadiusResult

    suspend fun refreshChildren(entry: Entry): EntryImmersiveChildRefreshResult

    suspend fun load(request: EntryImmersiveLoadRequest): EntryImmersiveLoadResult

    fun renderer(handle: EntryImmersiveHandle): EntryImmersiveRendererResult

    fun openTarget(handle: EntryImmersiveHandle): EntryImmersiveOpenTargetResult

    fun release(handle: EntryImmersiveHandle)
}

data class EntryImmersiveContext(
    val entry: Entry,
    val source: UnifiedSource?,
)
