package mihon.entry.interactions.download

import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import mihon.entry.interactions.merge.EntryMergeDownloadOwnershipProjection
import mihon.entry.interactions.merge.EntryMergeSubject
import tachiyomi.domain.entry.model.Entry

internal class DefaultEntryDownloadMaintenanceFeature(
    private val types: Set<EntryType>,
    private val interaction: EntryDownloadInteraction,
    private val ownership: EntryMergeDownloadOwnershipProjection,
) : EntryDownloadMaintenanceFeature {
    override fun invalidateCaches(): EntryDownloadMaintenanceResult {
        if (types.isEmpty()) return EntryDownloadMaintenanceResult.NoParticipants
        interaction.invalidateCaches()
        return EntryDownloadMaintenanceResult.Performed
    }

    override fun renameSource(
        oldSource: UnifiedSource,
        newSource: UnifiedSource,
    ): EntryDownloadMaintenanceResult {
        if (types.isEmpty()) return EntryDownloadMaintenanceResult.NoParticipants
        interaction.renameSource(oldSource, newSource)
        return EntryDownloadMaintenanceResult.Performed
    }

    override suspend fun renameEntry(entry: Entry, newTitle: String): EntryDownloadMaintenanceResult {
        if (entry.type !in types) return inapplicable(entry.type)
        interaction.renameEntry(entry, newTitle)
        return EntryDownloadMaintenanceResult.Performed
    }

    override suspend fun inspectEntry(entry: Entry): EntryDownloadMaintenanceInspection {
        if (entry.type !in types) {
            return EntryDownloadMaintenanceInspection.Inapplicable(entry.type)
        }
        val owners = ownership.resolveDownloadOwners(entry.mergeSubject()).orderedOwners
        return if (owners.any(interaction::hasDownloads)) {
            EntryDownloadMaintenanceInspection.HasDownloads
        } else {
            EntryDownloadMaintenanceInspection.NoDownloads
        }
    }

    override suspend fun removeEntryDownloads(entry: Entry): EntryDownloadMaintenanceResult {
        return when (val preparation = prepareRemoval(entry)) {
            is EntryDownloadRemovalPreparation.Prepared -> applyRemoval(preparation.plan)
            EntryDownloadRemovalPreparation.NothingToRemove -> EntryDownloadMaintenanceResult.Performed
            is EntryDownloadRemovalPreparation.Inapplicable -> inapplicable(preparation.type)
        }
    }

    override suspend fun prepareRemoval(entry: Entry): EntryDownloadRemovalPreparation {
        if (entry.type !in types) return EntryDownloadRemovalPreparation.Inapplicable(entry.type)
        val owners = ownership.resolveDownloadOwners(entry.mergeSubject()).orderedOwners
            .filter(interaction::hasDownloads)
        return if (owners.isEmpty()) {
            EntryDownloadRemovalPreparation.NothingToRemove
        } else {
            EntryDownloadRemovalPreparation.Prepared(EntryDownloadRemovalPlan(owners))
        }
    }

    override suspend fun applyRemoval(plan: EntryDownloadRemovalPlan): EntryDownloadMaintenanceResult {
        val remaining = plan.owners.filter { owner ->
            !interaction.deleteEntryDownloads(owner) || interaction.hasDownloads(owner)
        }
        return if (remaining.isEmpty()) {
            EntryDownloadMaintenanceResult.Performed
        } else {
            EntryDownloadMaintenanceResult.Incomplete(remaining)
        }
    }

    private fun inapplicable(type: EntryType): EntryDownloadMaintenanceResult {
        return inapplicable(setOf(type))
    }

    private fun inapplicable(types: Set<EntryType>) = EntryDownloadMaintenanceResult.Inapplicable(types)
}

private fun Entry.mergeSubject() = EntryMergeSubject(profileId, id)
