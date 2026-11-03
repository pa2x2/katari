package mihon.entry.interactions.state.migration

import kotlinx.serialization.json.Json
import mihon.entry.interactions.durable.EntryDurableParticipant
import mihon.entry.interactions.durable.EntryDurablePayload
import mihon.entry.interactions.migration.EntryMigrationChildMatch
import mihon.entry.interactions.migration.EntryMigrationOption
import mihon.entry.interactions.migration.consequence.EntryMigrationDurableEvent
import mihon.entry.interactions.state.EntryProgressFeature
import mihon.entry.interactions.state.EntryProgressMigrationPayload
import mihon.entry.interactions.state.EntryProgressMigrationPreparation
import mihon.entry.interactions.state.EntryProgressResourceMapping
import mihon.entry.interactions.state.EntryProgressRestoreResult
import tachiyomi.domain.entry.model.progressResourceKey

internal class EntryProgressMigrationParticipant(
    private val feature: () -> EntryProgressFeature,
) : EntryDurableParticipant<EntryMigrationDurableEvent> {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    override val id = "entry.migration.progress"

    override suspend fun prepare(event: EntryMigrationDurableEvent): EntryDurablePayload? {
        val mappings = if (EntryMigrationOption.CHILD_STATE in event.selectedOptions) {
            prepareMigrationProgressMappings(event.childMatches)
        } else {
            emptyList()
        }
        return when (val result = feature().prepareMigration(event.source, event.target, mappings)) {
            is EntryProgressMigrationPreparation.Prepared -> EntryDurablePayload(
                schemaVersion = 1,
                value = json.encodeToString(EntryProgressMigrationPayload.serializer(), result.payload),
            )
            is EntryProgressMigrationPreparation.Inapplicable -> null
            is EntryProgressMigrationPreparation.IncompatibleTypes -> error(
                "Progress Migration requires matching Entry types",
            )
        }
    }

    override suspend fun deliver(payload: EntryDurablePayload) {
        require(payload.schemaVersion == 1) { "Unsupported Progress Migration payload ${payload.schemaVersion}" }
        val decoded = json.decodeFromString(EntryProgressMigrationPayload.serializer(), payload.value)
        check(feature().applyMigration(decoded) is EntryProgressRestoreResult.Applied)
    }
}

private fun prepareMigrationProgressMappings(
    childMatches: List<EntryMigrationChildMatch>,
): List<EntryProgressResourceMapping> {
    return childMatches.mapNotNull { match ->
        match.source?.let { source ->
            EntryProgressResourceMapping(
                sourceResourceKey = source.progressResourceKey,
                targetResourceKey = match.target.progressResourceKey,
                targetChapterId = match.target.id,
            )
        }
    }
}
