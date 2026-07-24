package mihon.entry.interactions

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mihon.entry.interactions.host.EntryMigrationConsequenceHost
import mihon.entry.interactions.host.EntryMigrationCustomCoverHost
import mihon.entry.interactions.host.EntryMigrationCustomCoverPayload
import mihon.entry.interactions.host.EntryMigrationPersistedPayload
import org.junit.jupiter.api.Test

class EntryMigrationCustomCoverOrphanCleanupTest {
    @Test
    fun `cleanup decodes only the custom-cover participant's active stages`() = runTest {
        val consequenceHost = mockk<EntryMigrationConsequenceHost> {
            coEvery { participantPayloads(ENTRY_MIGRATION_CUSTOM_COVER_PARTICIPANT.id.value) } returns listOf(
                EntryMigrationPersistedPayload(
                    schemaVersion = 1,
                    payload = Json.encodeToString(
                        EntryMigrationCustomCoverPayload.serializer(),
                        EntryMigrationCustomCoverPayload("active-stage", 7L),
                    ),
                ),
                EntryMigrationPersistedPayload(schemaVersion = 99, payload = "future"),
            )
        }
        val coverHost = mockk<EntryMigrationCustomCoverHost>(relaxed = true)

        EntryMigrationCustomCoverOrphanCleanup(
            consequenceHost = consequenceHost,
            coverHost = coverHost,
            clockMillis = { 100_000_000L },
        ).cleanup()

        coVerify(exactly = 1) {
            coverHost.cleanupOrphans(
                activeStageIds = setOf("active-stage"),
                olderThanMillis = 13_600_000L,
                limit = 100,
            )
        }
    }
}
