package app.katari.entry.interactions.state

import app.katari.entry.interactions.download.EntryDownloadLifecycleEvent
import app.katari.entry.interactions.download.EntryDownloadLifecycleEventSink
import app.katari.entry.interactions.history.EntryHistoryFeature
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

internal class DefaultEntryConsumptionFeature(
    private val applicableTypes: Set<EntryType>,
    private val interaction: EntryConsumptionInteraction,
    private val downloadLifecycle: EntryDownloadLifecycleEventSink,
    private val history: EntryHistoryFeature,
) : EntryConsumptionFeature {
    override fun isApplicable(type: EntryType): Boolean = type in applicableTypes

    override fun canSetConsumed(
        type: EntryType,
        status: EntryConsumptionStatus,
        consumed: Boolean,
    ): Boolean {
        if (!isApplicable(type)) return false
        val canChange = shouldChangeConsumption(status, consumed)
        return canChange
    }

    override suspend fun setConsumed(
        entry: Entry,
        children: List<EntryChapter>,
        consumed: Boolean,
    ): EntryConsumptionResult {
        if (!isApplicable(entry.type)) return EntryConsumptionResult.Inapplicable(entry.type)

        val changed = interaction.setConsumed(entry, children, consumed)
        if (changed.isEmpty()) return EntryConsumptionResult.NoChange

        if (consumed) {
            history.recordManualCompletions(entry, changed)
            downloadLifecycle.onEvent(EntryDownloadLifecycleEvent.MarkedConsumed(entry, changed))
        }
        return EntryConsumptionResult.Changed(changed)
    }
}
