package mihon.desktop.media

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import mihon.entry.interactions.media.EntryPreviewAvailability
import mihon.entry.interactions.media.EntryPreviewContext
import mihon.entry.interactions.media.EntryPreviewFeature
import mihon.entry.interactions.media.EntryPreviewHandle
import mihon.entry.interactions.media.EntryPreviewLoadRequest
import mihon.entry.interactions.media.EntryPreviewLoadResult
import mihon.entry.interactions.media.EntryPreviewOpenTargetResult
import mihon.entry.interactions.media.EntryPreviewSettings

/**
 * Entry previews as a feature that applies to no entry type, so screens offer no preview and the browse settings show
 * no preview settings, as for a type without previews. Stands in for the shared preview feature, which needs the child
 * list feature desktop doesn't host yet.
 */
@Inject
class NoEntryPreviewFeature : EntryPreviewFeature {
    override val settings: List<EntryPreviewSettings> = emptyList()

    override fun isApplicable(type: EntryType): Boolean = false

    override fun isOpenApplicable(type: EntryType): Boolean = false

    override fun availability(context: EntryPreviewContext): EntryPreviewAvailability =
        EntryPreviewAvailability.Inapplicable(context.entry.type)

    override fun availabilityChanges(context: EntryPreviewContext): Flow<EntryPreviewAvailability> =
        flowOf(availability(context))

    override suspend fun load(request: EntryPreviewLoadRequest): EntryPreviewLoadResult =
        EntryPreviewLoadResult.Inapplicable(request.previewContext.entry.type)

    override suspend fun loadPage(handle: EntryPreviewHandle, pageIndex: Int) = Unit

    override fun openTarget(handle: EntryPreviewHandle, pageIndex: Int): EntryPreviewOpenTargetResult =
        EntryPreviewOpenTargetResult.Inapplicable(handle.entryType)

    override fun release(handle: EntryPreviewHandle) = Unit
}
