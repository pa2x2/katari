package app.katari.sync.client.kind.history

import app.katari.sync.client.RecordExchange
import app.katari.sync.client.SyncTestDevice
import app.katari.sync.client.SyncingTestDevice
import app.katari.sync.client.TestSyncServer
import app.katari.sync.client.account.AccountCall
import app.katari.sync.client.firstsync.ProfileChoice
import app.katari.sync.client.firstsync.ProfilePairing
import app.katari.sync.client.firstsync.SettingsWinner
import app.katari.sync.client.library
import app.katari.sync.client.run.SyncOutcome
import app.katari.sync.client.showRows
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class HistoryTimeReadTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `time read on two devices at once adds up instead of the later one replacing the other`(): Unit = runBlocking {
        val exchange = RecordExchange()
        SyncTestDevice(directory.resolve("a.db")).use { a ->
            SyncTestDevice(directory.resolve("b.db")).use { b ->
                a.startSync(deviceId = 1)
                b.startSync(deviceId = 2)
                a.sql(
                    "INSERT INTO source_identities(local_key, id_scheme, external_id) VALUES (-2, 'aniyomi', 777)",
                    "INSERT INTO entries(_id, source, url, title, favorite, type) VALUES (1, -2, '/show', 'Show', 1, 'anime')",
                    "INSERT INTO chapters(_id, entry_id, url, name, chapter_number) VALUES (10, 1, '/show/1', 'Ep 1', 1)",
                    "INSERT INTO history(entry_id, chapter_id, last_read, time_read) VALUES (1, 10, 1000, 1000)",
                )
                exchange.settle(listOf(a, b))

                a.sql("UPDATE history SET time_read = time_read + 200, last_read = 2000")
                a.drainAll()
                b.sql("UPDATE history SET time_read = time_read + 500, last_read = 3000")
                exchange.settle(listOf(a, b))

                a.library() shouldBe b.library()
                a.library() shouldContain """["history","/show/1",3000,1700]"""
            }
        }
    }

    @Test
    fun `time read on a chapter removed and listed again adds to what every device read on it before`(): Unit =
        runBlocking {
            val exchange = RecordExchange()
            SyncTestDevice(directory.resolve("a.db")).use { a ->
                SyncTestDevice(directory.resolve("b.db")).use { b ->
                    a.startSync(deviceId = 1)
                    b.startSync(deviceId = 2)
                    a.sql(
                        "INSERT INTO source_identities(local_key, id_scheme, external_id) VALUES (-2, 'aniyomi', 777)",
                        "INSERT INTO entries(_id, source, url, title, favorite, type) VALUES (1, -2, '/show', 'Show', 1, 'anime')",
                        "INSERT INTO chapters(_id, entry_id, url, name, chapter_number) VALUES (10, 1, '/show/1', 'Ep 1', 1)",
                        "INSERT INTO history(entry_id, chapter_id, last_read, time_read) VALUES (1, 10, 1000, 1000)",
                    )
                    exchange.settle(listOf(a, b))
                    b.sql("UPDATE history SET time_read = time_read + 500, last_read = 2000")
                    exchange.settle(listOf(a, b))

                    // A refresh on A no longer lists the episode, and a later one lists it again
                    a.sql("DELETE FROM chapters WHERE _id = 10")
                    a.sql(
                        "INSERT INTO chapters(_id, entry_id, url, name, chapter_number) VALUES (11, 1, '/show/1', 'Ep 1', 1)",
                        "INSERT INTO history(entry_id, chapter_id, last_read, time_read) VALUES (1, 11, 3000, 200)",
                    )
                    exchange.settle(listOf(a, b))

                    a.library() shouldBe b.library()
                    a.library() shouldContain """["history","/show/1",3000,1700]"""
                }
            }
        }

    @Test
    fun `a joining device's time on a chapter the account has counts as the larger of the two, then adds up`(): Unit =
        runBlocking {
            TestSyncServer(directory).use { server ->
                val account = server.account("reader")
                SyncingTestDevice(directory.resolve("a.db")).use { a ->
                    SyncingTestDevice(directory.resolve("b.db")).use { b ->
                        a.sql(*showRows("/show", read = 1), history(minutes = 55))
                        a.join(server, account)
                        a.sync() shouldBe SyncOutcome.SYNCED

                        // As restored from a backup of the same library, read a little further since
                        b.sql(*showRows("/show", read = 1), history(minutes = 60))
                        b.join(server, account)
                        b.sync()
                        val proposal = b.account.firstSyncProposal()
                            .shouldBeInstanceOf<AccountCall.Done<List<ProfilePairing>>>().value
                        b.account.answerFirstSync(
                            proposal.map { ProfileChoice(it.profileId, it.pairedWith?.uuid, SettingsWinner.ACCOUNT) },
                        )
                        b.sync() shouldBe SyncOutcome.SYNCED
                        a.sync() shouldBe SyncOutcome.SYNCED
                        a.library() shouldContain timeRead(lastRead = 2000, minutes = 60)
                        b.library() shouldBe a.library()

                        a.sql("UPDATE history SET time_read = time_read + ${minutes(10)}, last_read = 3000")
                        a.sync() shouldBe SyncOutcome.SYNCED
                        b.sync() shouldBe SyncOutcome.SYNCED
                        b.library() shouldContain timeRead(lastRead = 3000, minutes = 70)
                        a.library() shouldBe b.library()
                    }
                }
            }
        }

    private fun history(minutes: Int) = """
        INSERT INTO history(entry_id, chapter_id, last_read, time_read)
        SELECT entry_id, _id, 2000, ${minutes(minutes)} FROM chapters WHERE url = '/show/1'
    """

    private fun timeRead(lastRead: Long, minutes: Int) = """["history","/show/1",$lastRead,${minutes(minutes)}]"""

    private fun minutes(count: Int) = count * 60_000L
}
