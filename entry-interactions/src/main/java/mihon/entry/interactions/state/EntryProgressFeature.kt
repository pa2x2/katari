package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.documentation.EntryContentTypeReferenceSection
import mihon.entry.interactions.documentation.entryContentTypeReferenceContribution
import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.ContextInputId
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureBehaviorProjection
import mihon.feature.graph.FeatureContextBlocker
import mihon.feature.graph.FeatureContextDecision
import mihon.feature.graph.FeatureContribution
import mihon.feature.graph.FeatureExecutionParticipantDefinition
import mihon.feature.graph.FeatureExecutionParticipantId
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.FeatureGraphEvaluation
import mihon.feature.graph.FeatureId
import mihon.feature.graph.FeatureIntegration
import mihon.feature.graph.FeatureIntegrationId
import mihon.feature.graph.allOf
import mihon.feature.graph.contextInputDefinition
import mihon.feature.graph.featureContextRule
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryProgressState
import tachiyomi.domain.entry.model.progressResourceKey
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.library.service.GlobalLibraryPreferences
import tachiyomi.domain.library.service.LibraryPreferences

internal val ENTRY_PROGRESS_FEATURE_ID = FeatureId("entry.progress-transfer")
internal val ENTRY_PROGRESS_INTEGRATION_ID = FeatureIntegrationId("entry.progress-transfer.provider")
private val ENTRY_PROGRESS_MIGRATION_INTEGRATION_ID = FeatureIntegrationId("entry.progress-transfer.migration")
internal val ENTRY_PROGRESS_MEDIA_SESSION_INTEGRATION_ID = FeatureIntegrationId("entry.progress.media-session")
internal val ENTRY_PROGRESS_FEATURE_OWNER = ContributionOwner("entry-progress-transfer")
private val ENTRY_PROGRESS_REFERENCE = entryContentTypeReferenceContribution(
    id = "progress-transfer",
    owner = ENTRY_PROGRESS_FEATURE_OWNER,
    section = EntryContentTypeReferenceSection.ENTRY_INTERACTIONS,
    label = "Preserve progress through backup and migration",
    order = 350,
)

internal enum class EntryProgressBehavior(
    override val id: FeatureArtifactId,
) : FeatureBehaviorProjection {
    DISPATCH(FeatureArtifactId("entry.progress-transfer.dispatch")),
    BACKUP_CREATE(FeatureArtifactId("entry.progress-transfer.backup-create")),
    BACKUP_RESTORE(FeatureArtifactId("entry.progress-transfer.backup-restore")),
}

private object EntryProgressMigrationBehavior : FeatureBehaviorProjection {
    override val id = FeatureArtifactId("entry.progress-transfer.migration-copy")
}

private object EntryProgressMediaSessionBehavior : FeatureBehaviorProjection {
    override val id = FeatureArtifactId("entry.progress.media-session")
}

internal object EntryProgressBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.progress-transfer.behavior")
}

internal object EntryProgressMediaSessionBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.progress.media-session.behavior")
}

internal val ENTRY_MEDIA_SESSION_PROGRESS_ALLOWED = contextInputDefinition<Boolean>(
    ContextInputId("entry.media-session.progress-allowed"),
    ENTRY_MEDIA_SESSION_INCOGNITO_OWNER,
)
private val ENTRY_MEDIA_SESSION_PROGRESS_BLOCKED = FeatureContextBlocker(
    FeatureArtifactId("entry.media-session.progress-blocked"),
    listOf(ENTRY_MEDIA_SESSION_PROGRESS_ALLOWED),
)

internal val ENTRY_PROGRESS_MEDIA_SESSION_PARTICIPANT = FeatureExecutionParticipantDefinition(
    id = FeatureExecutionParticipantId("entry.progress.media-session"),
    owner = ENTRY_PROGRESS_FEATURE_OWNER,
    point = ENTRY_MEDIA_SESSION_CONSEQUENCE_EXECUTION_POINT,
    prerequisites = allOf(
        CapabilityExpression.Provided(EntryMediaSessionCapability.definition),
        CapabilityExpression.Provided(EntryProgressCapability.definition),
    ),
    contextInputs = listOf(ENTRY_MEDIA_SESSION_PROGRESS_ALLOWED),
    contextRule = featureContextRule(ENTRY_PROGRESS_FEATURE_OWNER) { evidence ->
        if (evidence.value(ENTRY_MEDIA_SESSION_PROGRESS_ALLOWED)) {
            FeatureContextDecision.Applicable
        } else {
            FeatureContextDecision.Blocked(listOf(ENTRY_MEDIA_SESSION_PROGRESS_BLOCKED))
        }
    },
    contextBlockers = listOf(ENTRY_MEDIA_SESSION_PROGRESS_BLOCKED),
    behavioralContracts = listOf(EntryProgressMediaSessionBehaviorContract),
)

internal object EntryProgressFeatureContributor : FeatureGraphContributor {
    override val owner = ENTRY_PROGRESS_FEATURE_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(
            FeatureContribution(
                feature = ENTRY_PROGRESS_FEATURE_ID,
                owner = owner,
                integrations = listOf(
                    FeatureIntegration(
                        id = ENTRY_PROGRESS_INTEGRATION_ID,
                        prerequisites = CapabilityExpression.Provided(EntryProgressCapability.definition),
                        behaviorProjections = EntryProgressBehavior.entries,
                        behavioralContracts = listOf(EntryProgressBehaviorContract),
                        projectionRequirements = listOf(ENTRY_PROGRESS_REFERENCE.requirement),
                        projections = listOf(ENTRY_PROGRESS_REFERENCE.projection),
                    ),
                    FeatureIntegration(
                        id = ENTRY_PROGRESS_MIGRATION_INTEGRATION_ID,
                        prerequisites = allOf(
                            CapabilityExpression.Provided(EntryProgressCapability.definition),
                            CapabilityExpression.Provided(EntryMigrationCapability.definition),
                        ),
                        behaviorProjections = listOf(EntryProgressMigrationBehavior),
                    ),
                    FeatureIntegration(
                        id = ENTRY_PROGRESS_MEDIA_SESSION_INTEGRATION_ID,
                        prerequisites = allOf(
                            CapabilityExpression.Provided(EntryMediaSessionCapability.definition),
                            CapabilityExpression.Provided(EntryProgressCapability.definition),
                        ),
                        behaviorProjections = listOf(EntryProgressMediaSessionBehavior),
                        behavioralContracts = listOf(EntryProgressMediaSessionBehaviorContract),
                    ),
                ),
            ),
        )
    }
}

internal object EntryProgressMediaSessionContributor : FeatureGraphContributor {
    override val owner = ENTRY_PROGRESS_FEATURE_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(ENTRY_PROGRESS_MEDIA_SESSION_PARTICIPANT)
    }
}

internal class DefaultEntryProgressFeature(
    evaluation: FeatureGraphEvaluation,
    private val interaction: EntryProgressInteraction,
    private val repository: EntryProgressRepository,
    private val getEntryWithChapters: GetEntryWithChapters,
    private val globalLibraryPreferences: GlobalLibraryPreferences,
) : EntryProgressFeature {
    private val applicableTypes = EntryProgressBehavior.entries
        .map { behavior ->
            evaluation.applicableProviderTypes<EntryProgressProcessor>(
                feature = ENTRY_PROGRESS_FEATURE_ID,
                integration = ENTRY_PROGRESS_INTEGRATION_ID,
                behaviorProjection = behavior.id,
            )
        }
        .also { selectedTypes ->
            check(selectedTypes.distinct().size <= 1) {
                "Progress-transfer behaviors selected different provider sets: $selectedTypes"
            }
        }
        .firstOrNull()
        .orEmpty()
    private val migrationTypes = evaluation.applicableProviderTypes<EntryProgressProcessor>(
        feature = ENTRY_PROGRESS_FEATURE_ID,
        integration = ENTRY_PROGRESS_MIGRATION_INTEGRATION_ID,
        behaviorProjection = EntryProgressMigrationBehavior.id,
    )

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

        val sourceStates = interaction.snapshot(sourceEntry).states
            .associateBy { it.contentKey to it.resourceKey }
        val targetStates = resourceMappings.mapNotNull { mapping ->
            sourceStates[mapping.sourceContentKey to mapping.sourceResourceKey]?.copy(
                contentKey = mapping.targetContentKey,
                resourceKey = mapping.targetResourceKey,
                sourceChildKey = mapping.targetResourceKey,
            )
        }
        return EntryProgressMigrationPreparation.Prepared(
            EntryProgressMigrationPayload(targetEntry, EntryProgressSnapshot(targetStates)),
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
        return copy(locator = locator.copy(extensions = current.locator.extensions))
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
                    child.chapterNumber == currentChild.chapterNumber
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
