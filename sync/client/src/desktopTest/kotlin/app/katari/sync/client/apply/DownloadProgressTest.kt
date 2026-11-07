package app.katari.sync.client.apply

import app.katari.sync.client.CallCutter
import app.katari.sync.client.SyncingTestDevice
import app.katari.sync.client.TestSyncServer
import app.katari.sync.client.account.AccountCall
import app.katari.sync.client.connection.SyncStage
import app.katari.sync.client.firstsync.ProfileChoice
import app.katari.sync.client.firstsync.ProfilePairing
import app.katari.sync.client.firstsync.SettingsWinner
import app.katari.sync.client.run.SyncOutcome
import app.katari.sync.client.showRows
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class DownloadProgressTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `a first download runs to what the account holds by then, not when the device joined`(): Unit = runBlocking {
        TestSyncServer(directory).use { server ->
            val account = server.account("reader")
            val cutter = CallCutter()
            SyncingTestDevice(directory.resolve("a.db")).use { a ->
                SyncingTestDevice(directory.resolve("b.db"), cutter.client).use { b ->
                    a.sql(*showRows("/show", episodes = 2, read = 2))
                    a.join(server, account)
                    a.sync() shouldBe SyncOutcome.SYNCED
                    b.sql(*showRows("/own", read = 1))
                    b.join(server, account)
                    b.sync() shouldBe SyncOutcome.IDLE
                    val joinedAt = b.connection.state().stageTarget

                    // While the choices wait, another device pushes much more
                    a.sql(*showRows("/later", episodes = 30, read = 30))
                    a.sync() shouldBe SyncOutcome.SYNCED
                    cutter.pullsBeforeCut = 4
                    val proposal = b.account.firstSyncProposal()
                        .shouldBeInstanceOf<AccountCall.Done<List<ProfilePairing>>>().value
                    b.account.answerFirstSync(
                        proposal.map { ProfileChoice(it.profileId, it.pairedWith?.uuid, SettingsWinner.ACCOUNT) },
                    )
                    // Answering starts the download; a run requested now could follow it and finish the download
                    b.runs.lastOutcome.first { it != SyncOutcome.IDLE } shouldBe SyncOutcome.UNREACHABLE

                    b.connection.state().let { state ->
                        state.stage shouldBe SyncStage.DOWNLOADING
                        state.position shouldBeGreaterThan joinedAt
                        state.position shouldBeLessThan state.stageTarget
                    }
                }
            }
        }
    }
}
