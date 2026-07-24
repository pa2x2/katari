package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.feature.graph.FeatureGraphEvaluation
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.service.sortedForReading

internal class DefaultEntryDownloadActionFeature(
    private val evaluation: FeatureGraphEvaluation,
    private val interaction: EntryDownloadInteraction,
    private val sourceAccessResolver: EntryDownloadSourceAccessResolver,
) : EntryDownloadActionFeature {
    private val individualTypes = evaluation.downloadIndividualTypes()
    private val bulkTypes = evaluation.downloadBulkTypes()
    private val bookmarkedBulkTypes = evaluation.downloadBookmarkedBulkTypes()

    override fun individualAvailability(request: EntryDownloadActionRequest): EntryDownloadActionAvailability {
        return availability(listOf(request), individualTypes, evaluation::requireDownloadIndividualContext)
    }

    override fun individualSelectionAvailability(
        requests: List<EntryDownloadActionRequest>,
    ): EntryDownloadActionAvailability {
        return availability(requests, individualTypes, evaluation::requireDownloadIndividualContext)
    }

    override fun bulkAvailability(
        requests: List<EntryDownloadActionRequest>,
        action: EntryBulkDownloadAction,
    ): EntryDownloadActionAvailability {
        val bookmarked = action.type == EntryBulkDownloadActionType.BOOKMARKED
        val applicableTypes = if (bookmarked) bookmarkedBulkTypes else bulkTypes
        return availability(requests, applicableTypes) { target ->
            evaluation.requireDownloadBulkContext(target, bookmarked)
        }
    }

    override fun notificationAvailability(
        entry: Entry,
        childCount: Int,
    ): EntryDownloadActionAvailability {
        val target = resolveTarget(EntryDownloadActionRequest.forEntry(entry))
        if (target.type !in individualTypes) {
            return EntryDownloadActionAvailability.Inapplicable(setOf(target.type))
        }
        val selectionState = when {
            childCount <= 0 -> EntryDownloadSelectionState.EMPTY
            childCount > NOTIFICATION_DOWNLOAD_SELECTION_LIMIT ->
                EntryDownloadSelectionState.NOTIFICATION_LIMIT_EXCEEDED
            else -> EntryDownloadSelectionState.ACTIONABLE
        }
        evaluation.requireDownloadNotificationContext(target, selectionState)
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
        val target = resolveTarget(EntryDownloadActionRequest.forEntry(entry))
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
        val target = resolveTarget(EntryDownloadActionRequest.forEntry(entry))
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
            val availability = availabilityResolved(
                listOf(target),
                individualTypes,
                evaluation::requireDownloadIndividualContext,
            )
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
        val target = resolveTarget(EntryDownloadActionRequest(request.entry.type, request.sourceIds))
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
        val selectionState = if (chapters.isEmpty()) {
            EntryDownloadSelectionState.EMPTY
        } else {
            EntryDownloadSelectionState.ACTIONABLE
        }
        evaluation.requireDownloadIndividualOperationContext(target, selectionState)
        return when {
            target.sourceAccess == EntryDownloadSourceAccess.LOCAL_OR_STUB ->
                blockedBy(EntryDownloadActionBlocker.LOCAL_OR_STUB)
            selectionState == EntryDownloadSelectionState.EMPTY ->
                blockedBy(EntryDownloadActionBlocker.EMPTY_SELECTION)
            else -> EntryDownloadActionAvailability.Available
        }
    }

    private fun availability(
        requests: List<EntryDownloadActionRequest>,
        applicableTypes: Set<EntryType>,
        requireContext: (EntryDownloadActionTarget) -> Unit,
    ): EntryDownloadActionAvailability {
        return availabilityResolved(requests.map(::resolveTarget), applicableTypes, requireContext)
    }

    private fun bulkAvailabilityResolved(
        targets: List<EntryDownloadActionTarget>,
        action: EntryBulkDownloadAction,
    ): EntryDownloadActionAvailability {
        val bookmarked = action.type == EntryBulkDownloadActionType.BOOKMARKED
        val applicableTypes = if (bookmarked) bookmarkedBulkTypes else bulkTypes
        return availabilityResolved(targets, applicableTypes) { target ->
            evaluation.requireDownloadBulkContext(target, bookmarked)
        }
    }

    private fun availabilityResolved(
        targets: List<EntryDownloadActionTarget>,
        applicableTypes: Set<EntryType>,
        requireContext: (EntryDownloadActionTarget) -> Unit,
    ): EntryDownloadActionAvailability {
        if (targets.isEmpty()) return blockedBy(EntryDownloadActionBlocker.EMPTY_SELECTION)

        val inapplicableTypes = targets.map(
            EntryDownloadActionTarget::type,
        ).filterNot(applicableTypes::contains).toSet()
        if (inapplicableTypes.isNotEmpty()) {
            return EntryDownloadActionAvailability.Inapplicable(inapplicableTypes)
        }

        targets.forEach(requireContext)
        return if (targets.any { it.sourceAccess == EntryDownloadSourceAccess.LOCAL_OR_STUB }) {
            blockedBy(EntryDownloadActionBlocker.LOCAL_OR_STUB)
        } else {
            EntryDownloadActionAvailability.Available
        }
    }

    private fun resolveTarget(request: EntryDownloadActionRequest): EntryDownloadActionTarget {
        return EntryDownloadActionTarget(request.type, sourceAccessResolver.resolve(request.sourceIds))
    }
}

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
