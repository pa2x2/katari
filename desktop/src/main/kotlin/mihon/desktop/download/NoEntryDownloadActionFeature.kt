package mihon.desktop.download

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.download.EntryBulkDownloadAction
import mihon.entry.interactions.download.EntryBulkDownloadRequest
import mihon.entry.interactions.download.EntryBulkDownloadResolutionResult
import mihon.entry.interactions.download.EntryDownloadActionAvailability
import mihon.entry.interactions.download.EntryDownloadActionBlocker
import mihon.entry.interactions.download.EntryDownloadActionFeature
import mihon.entry.interactions.download.EntryDownloadActionRequest
import mihon.entry.interactions.download.EntryDownloadActionResult
import mihon.entry.interactions.download.EntryDownloadCancellationResult
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

/**
 * Download actions as a feature that applies to no entry type, answering as the shared feature does for a type
 * without downloads, so screens hide or refuse them. Stands in for the shared download actions, which desktop can host
 * once it has entry types to download and the application services their features contribute to.
 */
@Inject
class NoEntryDownloadActionFeature : EntryDownloadActionFeature {
    private val emptySelection = setOf(EntryDownloadActionBlocker.EMPTY_SELECTION)

    override fun individualAvailability(request: EntryDownloadActionRequest): EntryDownloadActionAvailability =
        availability(listOf(request))

    override fun individualSelectionAvailability(
        requests: List<EntryDownloadActionRequest>,
    ): EntryDownloadActionAvailability = availability(requests)

    override fun bulkAvailability(
        requests: List<EntryDownloadActionRequest>,
        action: EntryBulkDownloadAction,
    ): EntryDownloadActionAvailability = availability(requests)

    override fun notificationAvailability(entry: Entry, childCount: Int): EntryDownloadActionAvailability =
        EntryDownloadActionAvailability.Inapplicable(setOf(entry.type))

    override suspend fun download(
        entry: Entry,
        chapters: List<EntryChapter>,
        startNow: Boolean,
    ): EntryDownloadActionResult = EntryDownloadActionResult.Inapplicable(setOf(entry.type))

    override suspend fun delete(entry: Entry, chapters: List<EntryChapter>): EntryDownloadActionResult =
        EntryDownloadActionResult.Inapplicable(setOf(entry.type))

    override fun cancel(request: EntryDownloadActionRequest, chapterId: Long): EntryDownloadCancellationResult =
        EntryDownloadCancellationResult.Inapplicable(setOf(request.type))

    override fun retry(requests: List<EntryDownloadActionRequest>): EntryDownloadActionResult =
        if (requests.isEmpty()) {
            EntryDownloadActionResult.Blocked(emptySelection)
        } else {
            EntryDownloadActionResult.Inapplicable(requests.types())
        }

    override suspend fun resolveBulkDownloadCandidates(
        request: EntryBulkDownloadRequest,
    ): EntryBulkDownloadResolutionResult = EntryBulkDownloadResolutionResult.Inapplicable(setOf(request.entry.type))

    private fun availability(requests: List<EntryDownloadActionRequest>): EntryDownloadActionAvailability =
        if (requests.isEmpty()) {
            EntryDownloadActionAvailability.Blocked(emptySelection)
        } else {
            EntryDownloadActionAvailability.Inapplicable(requests.types())
        }

    private fun List<EntryDownloadActionRequest>.types(): Set<EntryType> = mapTo(mutableSetOf()) { it.type }
}
