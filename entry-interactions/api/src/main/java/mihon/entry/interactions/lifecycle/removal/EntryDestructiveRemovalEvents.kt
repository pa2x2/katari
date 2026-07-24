package mihon.entry.interactions

import tachiyomi.domain.entry.model.Entry

data class EntryDestructiveRemovingEvent(
    val entries: List<Entry>,
    val outcomes: EntryDestructiveRemovalOutcomeSink,
)

data class EntryDestructiveRemovedEvent(
    val entries: List<Entry>,
    val outcomes: EntryDestructiveRemovalOutcomes,
)

interface EntryDestructiveRemovalOutcomeSink {
    fun addDownloadPlan(plan: EntryDownloadRemovalPlan)
}

data class EntryDestructiveRemovalOutcomes(
    val downloadPlans: List<EntryDownloadRemovalPlan>,
)
