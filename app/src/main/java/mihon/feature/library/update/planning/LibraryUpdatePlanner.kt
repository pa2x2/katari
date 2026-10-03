package mihon.feature.library.update.planning

import mihon.entry.interactions.state.EntryUpdateEligibility
import mihon.entry.interactions.state.EntryUpdateEligibilityFeature
import mihon.entry.interactions.state.EntryUpdateEligibilityRequest
import mihon.entry.interactions.state.EntryUpdateSkipReason
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.domain.library.update.model.EntryUpdateDecisionReason
import tachiyomi.domain.library.update.model.EntryUpdateMode
import tachiyomi.domain.library.update.model.LibraryUpdateSkipRules
import kotlin.time.Duration.Companion.hours

/**
 * Decides which library entries an update checks.
 *
 * A rule-following update walks each entry down a ladder and stops at the first step that decides: the entry's Never
 * mode, its Always mode, a switched-off category, a paused source, a switched-off entry type, the skip rules, and for
 * automatic updates the check interval of its categories. An entry in several categories is skipped when any of them
 * would skip it.
 */
class LibraryUpdatePlanner(
    private val eligibility: EntryUpdateEligibilityFeature,
) {

    fun plan(
        request: LibraryUpdateRequest,
        items: List<LibraryItem>,
        settings: LibraryUpdateSettings,
        context: LibraryUpdatePlanningContext,
    ): List<LibraryUpdateDecision> {
        return when (request) {
            // Reports list merged entries member by member, so a picked member stands for its merged entry.
            is LibraryUpdateRequest.Selection ->
                items
                    .filter { item ->
                        item.entry.id in request.entryIds || item.memberEntries.any { it.id in request.entryIds }
                    }
                    .map { item ->
                        if (settings.entryModes[item.entry.id] == EntryUpdateMode.NEVER) {
                            LibraryUpdateDecision.Leave(item, EntryUpdateDecisionReason.ENTRY_NEVER)
                        } else {
                            LibraryUpdateDecision.Check(item)
                        }
                    }
            is LibraryUpdateRequest.FollowRules ->
                items
                    .filter { it.isInScope(request) }
                    .map { item -> decide(item, request.automatic, settings, context) }
        }
    }

    private fun decide(
        item: LibraryItem,
        automatic: Boolean,
        settings: LibraryUpdateSettings,
        context: LibraryUpdatePlanningContext,
    ): LibraryUpdateDecision {
        when (settings.entryModes[item.entry.id]) {
            EntryUpdateMode.NEVER -> return LibraryUpdateDecision.Leave(item, EntryUpdateDecisionReason.ENTRY_NEVER)
            EntryUpdateMode.ALWAYS -> return LibraryUpdateDecision.Check(item)
            EntryUpdateMode.FOLLOW_RULES, null -> Unit
        }

        // The library lists uncategorized entries under the Default category; this keeps a missing row from crashing.
        val categories = item.categories.ifEmpty { listOf(Category.UNCATEGORIZED_ID) }
        categories.firstOrNull { settings.categoryRules[it]?.autoUpdate == false }?.let { categoryId ->
            return LibraryUpdateDecision.Leave(item, EntryUpdateDecisionReason.CATEGORY_OFF, categoryId)
        }
        if (item.sourceIds.all { it in settings.pausedSourceIds }) {
            return LibraryUpdateDecision.Leave(item, EntryUpdateDecisionReason.SOURCE_OFF)
        }
        if (item.entry.type in settings.excludedEntryTypes) {
            return LibraryUpdateDecision.Leave(item, EntryUpdateDecisionReason.TYPE_OFF)
        }

        val categoryRules = categories.map { categoryId ->
            CategorySkipRules(
                categoryId = categoryId,
                overridden = settings.categoryRules[categoryId]?.overrides?.skipRules,
                library = settings.skipRules,
            )
        }
        val eligibilityRequest = EntryUpdateEligibilityRequest(
            entry = item.entry,
            skipRules = categoryRules.map(CategorySkipRules::effective).reduce(LibraryUpdateSkipRules::strictest),
            totalCount = item.totalCount,
            unconsumedCount = item.unconsumedCount,
            hasStarted = item.hasStarted,
            fetchWindowUpperBound = context.fetchWindowUpperBound,
        )
        val skipped = eligibility.evaluate(eligibilityRequest) as? EntryUpdateEligibility.Skipped
        if (skipped != null) {
            val reason = skipped.reason.toDecisionReason()
            return LibraryUpdateDecision.Leave(item, reason, categoryRules.decidingCategory(reason))
        }

        if (automatic) {
            notDue(item, categories, settings, context)?.let { return it }
        }
        return LibraryUpdateDecision.Check(item, skippedSourceIds = item.sourceIds intersect settings.pausedSourceIds)
    }

    /**
     * Automatic updates run at the shortest interval any category asks for. An entry whose categories ask for a longer
     * one waits until that has passed since its last check, give or take half a run so it doesn't slip a whole run late.
     */
    private fun notDue(
        item: LibraryItem,
        categories: List<Long>,
        settings: LibraryUpdateSettings,
        context: LibraryUpdatePlanningContext,
    ): LibraryUpdateDecision.Leave? {
        val periodHours = settings.schedulePeriodHours ?: return null
        val (categoryId, intervalHours) = categories
            .map { categoryId ->
                val overridden = settings.categoryRules[categoryId]?.overrides?.intervalHours
                (categoryId.takeIf { overridden != null }) to (overridden ?: settings.intervalHours)
            }
            .maxBy { (_, hours) -> if (hours <= 0) Int.MAX_VALUE else hours }
        if (intervalHours <= 0) {
            return LibraryUpdateDecision.Leave(item, EntryUpdateDecisionReason.NOT_DUE, categoryId)
        }
        if (intervalHours <= periodHours) return null

        val lastCheckedAt = context.lastCheckedAt[item.entry.id] ?: return null
        val dueAt = lastCheckedAt + (intervalHours.hours - (periodHours.hours / 2)).inWholeMilliseconds
        return if (context.now < dueAt) {
            LibraryUpdateDecision.Leave(item, EntryUpdateDecisionReason.NOT_DUE, categoryId)
        } else {
            null
        }
    }
}

private fun LibraryItem.isInScope(request: LibraryUpdateRequest.FollowRules): Boolean {
    return (request.categoryId == null || request.categoryId in categories) &&
        (request.sourceId == null || request.sourceId in sourceIds) &&
        (request.entryType == null || entry.type == request.entryType)
}

private class CategorySkipRules(
    val categoryId: Long,
    val overridden: LibraryUpdateSkipRules?,
    library: LibraryUpdateSkipRules,
) {
    val effective = overridden ?: library
}

/**
 * The category to name as the reason for a skip: none when the library rules alone would have skipped the entry,
 * otherwise the first category whose own rules did.
 */
private fun List<CategorySkipRules>.decidingCategory(reason: EntryUpdateDecisionReason): Long? {
    if (any { it.overridden == null && it.effective.skips(reason) }) return null
    return firstOrNull { it.overridden?.skips(reason) == true }?.categoryId
}

private fun LibraryUpdateSkipRules.skips(reason: EntryUpdateDecisionReason): Boolean = when (reason) {
    EntryUpdateDecisionReason.COMPLETED -> skipCompleted
    EntryUpdateDecisionReason.HAS_UNSEEN -> skipUnseen
    EntryUpdateDecisionReason.NOT_STARTED -> skipNotStarted
    EntryUpdateDecisionReason.OUTSIDE_RELEASE_PERIOD -> skipOutsideReleasePeriod
    else -> false
}

private fun EntryUpdateSkipReason.toDecisionReason(): EntryUpdateDecisionReason = when (this) {
    EntryUpdateSkipReason.NOT_ALWAYS_UPDATE -> EntryUpdateDecisionReason.FETCH_ONCE
    EntryUpdateSkipReason.COMPLETED -> EntryUpdateDecisionReason.COMPLETED
    EntryUpdateSkipReason.NOT_CAUGHT_UP -> EntryUpdateDecisionReason.HAS_UNSEEN
    EntryUpdateSkipReason.NOT_STARTED -> EntryUpdateDecisionReason.NOT_STARTED
    EntryUpdateSkipReason.OUTSIDE_RELEASE_PERIOD -> EntryUpdateDecisionReason.OUTSIDE_RELEASE_PERIOD
}
