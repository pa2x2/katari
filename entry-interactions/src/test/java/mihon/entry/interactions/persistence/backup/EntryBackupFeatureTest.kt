package mihon.entry.interactions.persistence.backup

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry

class EntryBackupFeatureTest {

    @Test
    fun `participant state returns to its owner on restore alongside unknown feature state`() = runTest {
        val participant = RecordingParticipant()
        val feature = EntryBackupCoordinator(setOf(participant))
        val entry = Entry.create().copy(id = 1, profileId = 2, source = 3, url = "/entry")
        val session = EntryBackupRestoreSession(EntryBackupRestoreSessionId("session"))

        val states = feature.snapshot(2, entry, EntryBackupSelection(true, true))

        feature.restore(
            session,
            2,
            entry,
            states + EntryFeatureStateEnvelope("future.feature", 4, byteArrayOf(9)),
        )
        participant.restoredPayload shouldBe byteArrayOf(7).toList()

        feature.finalizeRestore(session, 2, setOf(EntryType.MANGA)).issues shouldBe emptyList()
        participant.finalizedTypes shouldBe listOf(EntryType.MANGA)
    }

    private class RecordingParticipant : EntryBackupStateParticipant {
        override val stateId = "test.feature.backup"
        var restoredPayload: List<Byte>? = null
        val finalizedTypes = mutableListOf<EntryType>()

        override suspend fun snapshot(event: EntryBackupSnapshotEvent) {
            event.contributions.add(EntryFeatureStateEnvelope(stateId, 1, byteArrayOf(7)))
        }

        override suspend fun restore(event: EntryBackupRestoreEvent) {
            restoredPayload = event.states.state(stateId)?.payload?.toList()
        }

        override suspend fun finalizeRestore(event: EntryBackupRestoreFinalizingEvent) {
            finalizedTypes += event.type
        }
    }
}
