package mihon.feature.library.update.planning

import mihon.entry.interactions.state.EntryUpdateEligibility
import mihon.entry.interactions.state.EntryUpdateEligibilityFeature
import mihon.entry.interactions.state.EntryUpdateEligibilityRequest
import mihon.entry.interactions.state.EntryUpdateSkipReason
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.domain.library.update.model.LibraryUpdateSkipRules

/** What the next automatic update would check with the library as it is now, and how many entries each rule hits. */
data class LibraryUpdatePreview(
    val checks: Int,
    val librarySize: Int,
    /** Entries each skip rule would skip on its own, whether or not it is switched on. */
    val skipRuleHits: Map<LibraryUpdateSkipRule, Int>,
) {
    class Calculator(
        private val planner: LibraryUpdatePlanner,
        private val eligibility: EntryUpdateEligibilityFeature,
    ) {
        fun preview(
            items: List<LibraryItem>,
            settings: LibraryUpdateSettings,
            context: LibraryUpdatePlanningContext,
        ): LibraryUpdatePreview {
            val decisions = planner.plan(LibraryUpdateRequest.FollowRules(automatic = true), items, settings, context)
            return LibraryUpdatePreview(
                checks = decisions.count { it is LibraryUpdateDecision.Check },
                librarySize = items.size,
                skipRuleHits = LibraryUpdateSkipRule.entries.associateWith { rule ->
                    items.count { item -> rule.skips(item, context) }
                },
            )
        }

        private fun LibraryUpdateSkipRule.skips(item: LibraryItem, context: LibraryUpdatePlanningContext): Boolean {
            val eligibility = eligibility.evaluate(
                EntryUpdateEligibilityRequest(
                    entry = item.entry,
                    skipRules = only,
                    totalCount = item.totalCount,
                    unconsumedCount = item.unconsumedCount,
                    hasStarted = item.hasStarted,
                    fetchWindowUpperBound = context.fetchWindowUpperBound,
                ),
            )
            return (eligibility as? EntryUpdateEligibility.Skipped)?.reason == reason
        }
    }
}
