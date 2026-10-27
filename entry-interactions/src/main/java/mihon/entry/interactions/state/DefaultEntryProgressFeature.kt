package mihon.entry.interactions.state

import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.serialization.json.JsonObject
import mihon.entry.interactions.media.session.EntryMediaSessionEvent
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryProgressState
import tachiyomi.domain.entry.model.progressResourceKey
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.entry.service.numberKey
import tachiyomi.domain.library.service.GlobalLibraryPreferences
import tachiyomi.domain.library.service.LibraryPreferences

internal class DefaultEntryProgressFeature(
    private val applicableTypes: Set<EntryType>,
    private val migrationTypes: Set<EntryType>,
    private val interaction: EntryProgressInteraction,
    private val repository: EntryProgressRepository,
    private val getEntryWithChapters: GetEntryWithChapters,
    private val globalLibraryPreferences: GlobalLibraryPreferences,
) : EntryProgressFeature {

    override fun isApplicable(type: EntryType): Boolean = type in applicableTypes

    override suspend fun recordMediaProgress(
        event: EntryMediaSessionEvent.Progressed,
    ): EntryProgressRecordingResult {
        val incoming = event.progress.withPreservedLocatorExtensionsIfNeeded(event)
        val current = repository.get(incoming.entryId, incoming.contentKey, incoming.resourceKey)
        val merged = repository.mergeAndSyncChild(incoming)
        val completedNow = merged.completed && current?.completed != true
        if (completedNow && event.completeEquivalentChildrenByNumber && duplicateCompletionEnabled()) {
            completeEquivalentChildren(event, merged.completionUpdatedAt)
        }
        return EntryProgressRecordingResult(merged, completedNow)
    }

    override suspend fun snapshot(entry: Entry): EntryProgressSnapshotResult {
        if (!isApplicable(entry.type)) return EntryProgressSnapshotResult.Inapplicable(entry.type)
        return EntryProgressSnapshotResult.Available(interaction.snapshot(entry))
    }

    override suspend fun restore(
        entry: Entry,
        snapshot: EntryProgressSnapshot,
    ): EntryProgressRestoreResult {
        if (!isApplicable(entry.type)) return EntryProgressRestoreResult.Inapplicable(entry.type)
        interaction.restore(entry, snapshot)
        return EntryProgressRestoreResult.Applied
    }

    override suspend fun copy(
        sourceEntry: Entry,
        targetEntry: Entry,
        resourceMappings: List<EntryProgressResourceMapping>,
    ): EntryProgressCopyResult {
        if (sourceEntry.type != targetEntry.type) {
            return EntryProgressCopyResult.IncompatibleTypes(sourceEntry.type, targetEntry.type)
        }

        val inapplicableTypes = setOf(sourceEntry.type, targetEntry.type) - migrationTypes
        if (inapplicableTypes.isNotEmpty()) return EntryProgressCopyResult.Inapplicable(inapplicableTypes)

        interaction.copy(sourceEntry, targetEntry, resourceMappings)
        return EntryProgressCopyResult.Applied
    }

    override suspend fun prepareMigration(
        sourceEntry: Entry,
        targetEntry: Entry,
        resourceMappings: List<EntryProgressResourceMapping>,
    ): EntryProgressMigrationPreparation {
        if (sourceEntry.type != targetEntry.type) {
            return EntryProgressMigrationPreparation.IncompatibleTypes(sourceEntry.type, targetEntry.type)
        }
        val inapplicableTypes = setOf(sourceEntry.type, targetEntry.type) - migrationTypes
        if (inapplicableTypes.isNotEmpty()) return EntryProgressMigrationPreparation.Inapplicable(inapplicableTypes)

        return EntryProgressMigrationPreparation.Prepared(
            EntryProgressMigrationPayload(
                targetEntry,
                interaction.prepareMigration(sourceEntry, targetEntry, resourceMappings),
            ),
        )
    }

    override suspend fun applyMigration(payload: EntryProgressMigrationPayload): EntryProgressRestoreResult {
        return restore(payload.target, payload.snapshot)
    }

    private suspend fun EntryProgressState.withPreservedLocatorExtensionsIfNeeded(
        event: EntryMediaSessionEvent.Progressed,
    ): EntryProgressState {
        if (!event.preserveLocatorExtensions) return this
        val current = repository.get(entryId, contentKey, resourceKey) ?: return this
        return copy(
            locator = locator.copy(
                extensions = JsonObject(
                    current.locator.extensions + locator.extensions,
                ),
            ),
        )
    }

    private fun duplicateCompletionEnabled(): Boolean {
        return LibraryPreferences.MARK_DUPLICATE_CHAPTER_READ_EXISTING in
            globalLibraryPreferences.markDuplicateReadChapterAsRead.get()
    }

    private suspend fun completeEquivalentChildren(
        event: EntryMediaSessionEvent.Progressed,
        completionTimestamp: Long,
    ) {
        val currentChild = event.child
        if (!currentChild.isRecognizedNumber) return
        getEntryWithChapters.awaitChapters(event.visibleEntry)
            .filter { child ->
                child.id != currentChild.id &&
                    !child.read &&
                    child.isRecognizedNumber &&
                    child.numberKey == currentChild.numberKey
            }
            .forEach { child ->
                val current = repository.get(child.entryId, "", child.progressResourceKey)
                val state = current?.copy(
                    chapterId = child.id,
                    completed = true,
                    completionUpdatedAt = completionTimestamp,
                ) ?: EntryProgressState(
                    entryId = child.entryId,
                    chapterId = child.id,
                    resourceKey = child.progressResourceKey,
                    locator = event.progress.locator.copy(
                        position = null,
                        extent = null,
                        progression = null,
                        totalProgression = null,
                    ),
                    completed = true,
                    completionUpdatedAt = completionTimestamp,
                )
                repository.mergeAndSyncChild(state)
            }
    }
}
