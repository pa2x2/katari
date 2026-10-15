package mihon.entry.interactions.download

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.download.host.EntryDownloadSourceAccessResolver
import mihon.entry.interactions.download.runtime.EntryDownloadActionTarget
import mihon.entry.interactions.download.runtime.EntryDownloadSourceAccess
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.service.sortedForReading

internal class DefaultEntryDownloadActionFeature(
    private val types: EntryDownloadActionTypes,
    private val interaction: EntryDownloadInteraction,
    private val sourceAccessResolver: EntryDownloadSourceAccessResolver,
) : EntryDownloadActionFeature {
    private val individualTypes = types.individual

    override fun individualAvailability(request: EntryDownloadActionRequest): EntryDownloadActionAvailability {
        return availability(listOf(request), individualTypes)
    }

    override fun individualSelectionAvailability(
        requests: List<EntryDownloadActionRequest>,
    ): EntryDownloadActionAvailability {
        return availability(requests, individualTypes)
    }

    override fun bulkAvailability(
        requests: List<EntryDownloadActionRequest>,
        action: EntryBulkDownloadAction,
    ): EntryDownloadActionAvailability {
        return availability(requests, bulkTypes(action))
    }

    override fun notificationAvailability(
        entry: Entry,
        childCount: Int,
    ): EntryDownloadActionAvailability {
        val target = resolveTarget(EntryDownloadActionRequest.forEntry(entry))
        if (target.type !in individualTypes) {
            return EntryDownloadActionAvailability.Inapplicable(setOf(target.type))
        }
        return when {
            target.sourceAccess == EntryDownloadSourceAccess.LOCAL_OR_STUB ->
                blockedBy(EntryDownloadActionBlocker.LOCAL_OR_STUB)
            childCount <= 0 -> blockedBy(EntryDownloadActionBlocker.EMPTY_SELECTION)
            childCount > NOTIFICATION_DOWNLOAD_SELECTION_LIMIT -> {
                blockedBy(EntryDownloadActionBlocker.NOTIFICATION_SELECTION_TOO_LARGE)
            }
            else -> EntryDownloadActionAvailability.Available
        }
    }

    override suspend fun download(
        entry: Entry,
        chapters: List<EntryChapter>,
        startNow: Boolean,
    ): EntryDownloadActionResult {
        val target = resolveLoadedTarget(EntryDownloadActionRequest.forEntry(entry))
        return when (val availability = individualOperationAvailability(target, chapters)) {
            EntryDownloadActionAvailability.Available -> {
                interaction.download(entry, chapters, startNow)
                EntryDownloadActionResult.Performed
            }
            is EntryDownloadActionAvailability.Inapplicable -> availability.asActionResult()
            is EntryDownloadActionAvailability.Blocked -> availability.asActionResult()
        }
    }

    override suspend fun delete(
        entry: Entry,
        chapters: List<EntryChapter>,
    ): EntryDownloadActionResult {
        val target = resolveLoadedTarget(EntryDownloadActionRequest.forEntry(entry))
        return when (val availability = individualOperationAvailability(target, chapters)) {
            EntryDownloadActionAvailability.Available -> {
                interaction.delete(entry, chapters)
                EntryDownloadActionResult.Performed
            }
            is EntryDownloadActionAvailability.Inapplicable -> availability.asActionResult()
            is EntryDownloadActionAvailability.Blocked -> availability.asActionResult()
        }
    }

    override fun cancel(
        request: EntryDownloadActionRequest,
        chapterId: Long,
    ): EntryDownloadCancellationResult {
        val target = resolveTarget(request)
        return when (
            val availability = availabilityResolved(listOf(target), individualTypes)
        ) {
            EntryDownloadActionAvailability.Available -> interaction.cancelQueuedDownload(target.type, chapterId)
                ?.let(EntryDownloadCancellationResult::Cancelled)
                ?: EntryDownloadCancellationResult.NotQueued
            is EntryDownloadActionAvailability.Inapplicable -> {
                EntryDownloadCancellationResult.Inapplicable(availability.types)
            }
            is EntryDownloadActionAvailability.Blocked -> {
                EntryDownloadCancellationResult.Blocked(availability.blockers)
            }
        }
    }

    override fun retry(requests: List<EntryDownloadActionRequest>): EntryDownloadActionResult {
        return when (val availability = individualSelectionAvailability(requests)) {
            EntryDownloadActionAvailability.Available -> {
                interaction.startDownloads()
                EntryDownloadActionResult.Performed
            }
            is EntryDownloadActionAvailability.Inapplicable -> availability.asActionResult()
            is EntryDownloadActionAvailability.Blocked -> availability.asActionResult()
        }
    }

    override suspend fun resolveBulkDownloadCandidates(
        request: EntryBulkDownloadRequest,
    ): EntryBulkDownloadResolutionResult {
        val target = resolveLoadedTarget(EntryDownloadActionRequest(request.entry.type, request.sourceIds))
        return when (val availability = bulkAvailabilityResolved(listOf(target), request.action)) {
            EntryDownloadActionAvailability.Available -> {
                val pool = interaction.resolveBulkDownloadCandidatePool(request.entry, request.visibleCandidates)
                pool.selectBulkDownloadCandidates(request.entry, request.action, request.memberEntryIds)
                    .takeIf(List<EntryChapter>::isNotEmpty)
                    ?.let(EntryBulkDownloadResolutionResult::Candidates)
                    ?: EntryBulkDownloadResolutionResult.NoCandidates
            }
            is EntryDownloadActionAvailability.Inapplicable -> {
                EntryBulkDownloadResolutionResult.Inapplicable(availability.types)
            }
            is EntryDownloadActionAvailability.Blocked -> {
                EntryBulkDownloadResolutionResult.Blocked(availability.blockers)
            }
        }
    }

    private fun individualOperationAvailability(
        target: EntryDownloadActionTarget,
        chapters: List<EntryChapter>,
    ): EntryDownloadActionAvailability {
        if (target.type !in individualTypes) {
            return EntryDownloadActionAvailability.Inapplicable(setOf(target.type))
        }
        return when {
            target.sourceAccess == EntryDownloadSourceAccess.LOCAL_OR_STUB ->
                blockedBy(EntryDownloadActionBlocker.LOCAL_OR_STUB)
            chapters.isEmpty() ->
                blockedBy(EntryDownloadActionBlocker.EMPTY_SELECTION)
            else -> EntryDownloadActionAvailability.Available
        }
    }

    private fun availability(
        requests: List<EntryDownloadActionRequest>,
        applicableTypes: Set<EntryType>,
    ): EntryDownloadActionAvailability {
        return availabilityResolved(requests.map(::resolveTarget), applicableTypes)
    }

    private fun bulkAvailabilityResolved(
        targets: List<EntryDownloadActionTarget>,
        action: EntryBulkDownloadAction,
    ): EntryDownloadActionAvailability {
        return availabilityResolved(targets, bulkTypes(action))
    }

    private fun bulkTypes(action: EntryBulkDownloadAction): Set<EntryType> {
        return if (action.type == EntryBulkDownloadActionType.BOOKMARKED) types.bookmarkedBulk else types.bulk
    }

    private fun availabilityResolved(
        targets: List<EntryDownloadActionTarget>,
        applicableTypes: Set<EntryType>,
    ): EntryDownloadActionAvailability {
        if (targets.isEmpty()) return blockedBy(EntryDownloadActionBlocker.EMPTY_SELECTION)

        val inapplicableTypes = targets.map(
            EntryDownloadActionTarget::type,
        ).filterNot(applicableTypes::contains).toSet()
        if (inapplicableTypes.isNotEmpty()) {
            return EntryDownloadActionAvailability.Inapplicable(inapplicableTypes)
        }

        return if (targets.any { it.sourceAccess == EntryDownloadSourceAccess.LOCAL_OR_STUB }) {
            blockedBy(EntryDownloadActionBlocker.LOCAL_OR_STUB)
        } else {
            EntryDownloadActionAvailability.Available
        }
    }

    private fun resolveTarget(request: EntryDownloadActionRequest): EntryDownloadActionTarget {
        return EntryDownloadActionTarget(request.type, sourceAccessResolver.resolve(request.sourceIds))
    }

    private suspend fun resolveLoadedTarget(request: EntryDownloadActionRequest): EntryDownloadActionTarget {
        return EntryDownloadActionTarget(request.type, sourceAccessResolver.resolveLoaded(request.sourceIds))
    }
}

/** The types each action applies to, read from the providers the type modules contribute. */
internal class EntryDownloadActionTypes(
    val individual: Set<EntryType>,
    val bulk: Set<EntryType>,
    val bookmarkedBulk: Set<EntryType>,
)

private const val NOTIFICATION_DOWNLOAD_SELECTION_LIMIT = 15

private fun blockedBy(blocker: EntryDownloadActionBlocker): EntryDownloadActionAvailability.Blocked {
    return EntryDownloadActionAvailability.Blocked(setOf(blocker))
}

private fun EntryDownloadActionAvailability.Inapplicable.asActionResult(): EntryDownloadActionResult.Inapplicable {
    return EntryDownloadActionResult.Inapplicable(types)
}

private fun EntryDownloadActionAvailability.Blocked.asActionResult(): EntryDownloadActionResult.Blocked {
    return EntryDownloadActionResult.Blocked(blockers)
}

private fun List<EntryChapter>.selectBulkDownloadCandidates(
    entry: Entry,
    action: EntryBulkDownloadAction,
    memberEntryIds: List<Long>,
): List<EntryChapter> {
    return when (action.type) {
        EntryBulkDownloadActionType.NEXT -> filterNot(EntryChapter::read)
            .sortedForReading(entry, memberEntryIds.ifEmpty { map(EntryChapter::entryId).distinct() })
            .let { chapters -> action.limit?.let(chapters::take) ?: chapters }
        EntryBulkDownloadActionType.UNREAD -> filterNot(EntryChapter::read)
        EntryBulkDownloadActionType.BOOKMARKED -> filter(EntryChapter::bookmark)
    }
}
