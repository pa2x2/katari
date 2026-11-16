package app.katari.sync.client.firstsync

import app.katari.sync.client.SyncingTestDevice
import app.katari.sync.client.TestSyncServer
import app.katari.sync.client.account.AccountCall
import app.katari.sync.client.library
import app.katari.sync.client.run.SyncOutcome
import app.katari.sync.client.showRows
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ExistingPositionsTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `places watched before the first sync keep the one watched last, or else the further one`(): Unit =
        runBlocking {
            TestSyncServer(directory).use { server ->
                val account = server.account("reader")
                SyncingTestDevice(directory.resolve("a.db")).use { a ->
                    SyncingTestDevice(directory.resolve("b.db")).use { b ->
                        // Episode 1 was watched last on B, to an earlier place; episode 2's rows keep no time
                        a.sql(*showRows("/show", episodes = 2))
                        a.sql(position("/show/1", 45, readAt = 2000), position("/show/2", 9, readAt = 0))
                        b.sql(*showRows("/show", episodes = 2))
                        b.sql(position("/show/1", 30, readAt = 5000), position("/show/2", 12, readAt = 0))
                        a.join(server, account)
                        a.sync() shouldBe SyncOutcome.SYNCED

                        b.join(server, account)
                        b.sync()
                        val proposal = b.account.firstSyncProposal()
                            .shouldBeInstanceOf<AccountCall.Done<List<ProfilePairing>>>().value
                        b.account.answerFirstSync(
                            proposal.map { ProfileChoice(it.profileId, it.pairedWith?.uuid, SettingsWinner.DEVICE) },
                        )
                        b.sync() shouldBe SyncOutcome.SYNCED
                        a.sync() shouldBe SyncOutcome.SYNCED

                        b.library() shouldContainAll listOf(
                            """["progress","/show","","/show/1",30,0]""",
                            """["progress","/show","","/show/2",12,0]""",
                        )
                        a.library() shouldBe b.library()
                    }
                }
            }
        }

    private fun position(chapterUrl: String, position: Long, readAt: Long) = """
        INSERT INTO entry_progress_state(entry_id, chapter_id, resource_key, locator_kind, position, locator_updated_at)
        SELECT entry_id, _id, url, 'anime', $position, $readAt FROM chapters WHERE url = '$chapterUrl'
    """
}
