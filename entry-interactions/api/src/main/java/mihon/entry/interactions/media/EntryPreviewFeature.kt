package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.flow.Flow

/** Feature-owned boundary for preview availability, configuration, loading, opening, and lifecycle. */
interface EntryPreviewFeature {
    val settings: List<EntryPreviewSettings>

    fun isApplicable(type: EntryType): Boolean

    fun isOpenApplicable(type: EntryType): Boolean

    fun availability(context: EntryPreviewContext): EntryPreviewAvailability

    fun availabilityChanges(context: EntryPreviewContext): Flow<EntryPreviewAvailability>

    suspend fun load(request: EntryPreviewLoadRequest): EntryPreviewLoadResult

    suspend fun loadPage(handle: EntryPreviewHandle, pageIndex: Int)

    fun openTarget(handle: EntryPreviewHandle, pageIndex: Int): EntryPreviewOpenTargetResult

    fun release(handle: EntryPreviewHandle)
}
