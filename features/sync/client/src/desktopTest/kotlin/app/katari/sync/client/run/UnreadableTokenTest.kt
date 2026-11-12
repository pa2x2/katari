package app.katari.sync.client.run

import app.katari.sync.client.SyncingTestDevice
import app.katari.sync.client.TestSyncServer
import app.katari.sync.client.showRows
import app.katari.sync.client.status.SyncStatus
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class UnreadableTokenTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `a token store that can't be read for now leaves syncing on, and a later run reads it`(): Unit = runBlocking {
        TestSyncServer(directory).use { server ->
            val tokens = SyncingTestDevice.MemoryTokens()
            SyncingTestDevice(directory.resolve("a.db"), tokens = tokens).use { a ->
                a.sql(*showRows("/show"))
                a.join(server, server.account("reader"))
                a.sync() shouldBe SyncOutcome.SYNCED
            }
            // The app starts again with its keyring still locked
            tokens.unreadable = true
            SyncingTestDevice(directory.resolve("a.db"), tokens = tokens).use { a ->
                a.sql("UPDATE chapters SET read = 1 WHERE url = '/show/1'")
                a.sync() shouldBe SyncOutcome.TOKEN_UNREADABLE
                a.connection.state().stopped.shouldBeNull()
                a.status.status().first().activity shouldBe SyncStatus.Activity.TokenUnreadable

                tokens.unreadable = false
                a.sync() shouldBe SyncOutcome.SYNCED
                a.local.pending.count() shouldBe 0
            }
        }
    }
}
