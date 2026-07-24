package mihon.entry.interactions

import kotlinx.serialization.json.Json
import mihon.entry.interactions.host.EntryMigrationConsequenceHost
import mihon.entry.interactions.host.EntryMigrationCustomCoverHost
import mihon.feature.graph.FeatureDurableExecutionPayload

internal class EntryMigrationCustomCoverOrphanCleanup(
    private val consequenceHost: EntryMigrationConsequenceHost,
    private val coverHost: EntryMigrationCustomCoverHost,
    private val clockMillis: () -> Long = System::currentTimeMillis,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun cleanup() {
        val activeStageIds = consequenceHost.participantPayloads(ENTRY_MIGRATION_CUSTOM_COVER_PARTICIPANT.id.value)
            .mapNotNull { persisted ->
                runCatching {
                    FeatureDurableExecutionPayload(persisted.schemaVersion, persisted.payload)
                        .decodeCustomCover(json)
                        .stageId
                }.getOrNull()
            }
            .toSet()
        coverHost.cleanupOrphans(
            activeStageIds = activeStageIds,
            olderThanMillis = clockMillis() - ORPHAN_MINIMUM_AGE_MILLIS,
            limit = ORPHAN_CLEANUP_BATCH_SIZE,
        )
    }

    private companion object {
        const val ORPHAN_MINIMUM_AGE_MILLIS = 24 * 60 * 60 * 1_000L
        const val ORPHAN_CLEANUP_BATCH_SIZE = 100
    }
}
