package mihon.entry.interactions.persistence.backup

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.validation.entryBackupTestRuntime
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry

class EntryBackupFeatureTest {

    @Test
    fun `participant state returns to its owner on restore alongside unknown feature state`() = runTest {
        val fixture = fixture()
        val entry = Entry.create().copy(id = 1, profileId = 2, source = 3, url = "/entry")
        val session = EntryBackupRestoreSession(EntryBackupRestoreSessionId("session"))

        val states = fixture.feature.snapshot(2, entry, EntryBackupSelection(true, true))

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

    private fun fixture() = entryBackupTestRuntime(PARTICIPANT_STATE_ID)

    private companion object {
        const val PARTICIPANT_STATE_ID = "test.feature.backup"
    }
}
