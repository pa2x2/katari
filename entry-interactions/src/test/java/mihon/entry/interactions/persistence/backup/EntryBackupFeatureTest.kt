package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.validation.entryBackupTestRuntime
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry

class EntryBackupFeatureTest {

    @Test
    fun `new participant snapshots restores and finalizes without coordinator knowledge`() = runTest {
        val fixture = fixture()
        val entry = Entry.create().copy(id = 1, profileId = 2, source = 3, url = "/entry")
        val session = EntryBackupRestoreSession(EntryBackupRestoreSessionId("session"))

        val states = fixture.feature.snapshot(2, entry, EntryBackupSelection(true, true))
        states.single().participantId shouldBe PARTICIPANT_STATE_ID
        states.single().schemaVersion shouldBe 1
        states.single().payload.toList() shouldBe listOf(7)

        fixture.feature.restore(
            session,
            2,
            entry,
            states + EntryFeatureStateEnvelope("future.feature", 4, byteArrayOf(9)),
        )
        fixture.restoredPayload() shouldBe byteArrayOf(7).toList()

        fixture.feature.finalizeRestore(session, 2, setOf(EntryType.MANGA)).issues shouldBe emptyList()
        fixture.finalizedTypes shouldBe listOf(EntryType.MANGA)
    }

    @Test
    fun `duplicate participant state is rejected before restore execution`() = runTest {
        val fixture = fixture()
        val state = EntryFeatureStateEnvelope(PARTICIPANT_STATE_ID, 1, byteArrayOf(1))

        shouldThrow<IllegalStateException> {
            fixture.feature.restore(
                EntryBackupRestoreSession(EntryBackupRestoreSessionId("session")),
                1,
                Entry.create().copy(type = EntryType.MANGA),
                listOf(state, state.copy(payload = byteArrayOf(2))),
            )
        }
        fixture.restoredPayload() shouldBe null
    }

    private fun fixture() = entryBackupTestRuntime(PARTICIPANT_STATE_ID)

    private companion object {
        const val PARTICIPANT_STATE_ID = "test.feature.backup"
    }
}
