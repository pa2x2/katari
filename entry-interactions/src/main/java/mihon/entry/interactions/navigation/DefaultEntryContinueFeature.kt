package mihon.entry.interactions.navigation

import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.Entry

internal class DefaultEntryContinueFeature(
    private val applicableTypes: Set<EntryType>,
    private val interaction: EntryContinueInteraction,
    private val batchPreparation: EntryContinueBatchPreparation,
) : EntryContinueFeature, SeededEntryContinueBatchFeature {
    override fun isApplicable(type: EntryType): Boolean = type in applicableTypes

    override suspend fun nextTarget(entry: Entry): EntryContinueTargetResult {
        if (entry.type !in applicableTypes) return EntryContinueTargetResult.Inapplicable
        return interaction.findNext(entry)
            ?.let(EntryContinueTargetResult::Available)
            ?: EntryContinueTargetResult.NoNext
    }

    override suspend fun nextTargets(entries: List<Entry>): Map<Long, EntryContinueTargetResult> {
        val applicableEntries = entries.filter { it.type in applicableTypes }
        val preparedByEntryId = batchPreparation.prepare(applicableEntries)
        return resolvePreparedTargets(entries, preparedByEntryId)
    }

    override suspend fun nextTargets(
        entries: List<Entry>,
        seed: EntryContinueBatchSeed,
    ): Map<Long, EntryContinueTargetResult> {
        val applicableEntries = entries.filter { it.type in applicableTypes }
        val preparedByEntryId = batchPreparation.prepare(applicableEntries, seed)
        return resolvePreparedTargets(entries, preparedByEntryId)
    }

    private suspend fun resolvePreparedTargets(
        entries: List<Entry>,
        preparedByEntryId: Map<Long, EntryContinuePreparedInput>,
    ): Map<Long, EntryContinueTargetResult> {
        return entries.associate { entry ->
            entry.id to when {
                entry.type !in applicableTypes -> EntryContinueTargetResult.Inapplicable
                else -> preparedByEntryId[entry.id]?.let { prepared ->
                    interaction.findNext(entry, prepared.chapters, prepared.progressStates)
                        ?.let(EntryContinueTargetResult::Available)
                        ?: EntryContinueTargetResult.NoNext
                } ?: nextTarget(entry)
            }
        }
    }

    override suspend fun continueEntry(context: Context, entry: Entry): EntryContinueResult {
        if (!isApplicable(entry.type)) return EntryContinueResult.Inapplicable
        return interaction.continueEntry(context, entry)
            ?.let(EntryContinueResult::Opened)
            ?: EntryContinueResult.NoNext
    }
}
