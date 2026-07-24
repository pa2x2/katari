package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.feature.graph.FeatureGraphEvaluation
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

internal class DefaultEntryConsumptionFeature(
    private val evaluation: FeatureGraphEvaluation,
    private val interaction: EntryConsumptionInteraction,
    private val downloadLifecycle: EntryDownloadLifecycleEventSink,
) : EntryConsumptionFeature {
    private val applicableTypes = evaluation.consumptionTypes()

    override fun isApplicable(type: EntryType): Boolean = type in applicableTypes

    override fun canSetConsumed(
        type: EntryType,
        status: EntryConsumptionStatus,
        consumed: Boolean,
    ): Boolean {
        if (!isApplicable(type)) return false
        val canChange = shouldChangeConsumption(status, consumed)
        evaluation.requireConsumptionEligibilityContext(type, canChange)
        return canChange
    }

    override suspend fun setConsumed(
        entry: Entry,
        children: List<EntryChapter>,
        consumed: Boolean,
    ): EntryConsumptionResult {
        if (!isApplicable(entry.type)) return EntryConsumptionResult.Inapplicable(entry.type)

        val changed = interaction.setConsumed(entry, children, consumed)
        evaluation.requireConsumptionMutationContext(entry.type, changed.isNotEmpty())
        evaluation.requireConsumptionLifecycleContext(entry.type, changed.isNotEmpty(), consumed)
        if (changed.isEmpty()) return EntryConsumptionResult.NoChange

        if (consumed) {
            downloadLifecycle.onEvent(EntryDownloadLifecycleEvent.MarkedConsumed(entry, changed))
        }
        return EntryConsumptionResult.Changed(changed)
    }
}
