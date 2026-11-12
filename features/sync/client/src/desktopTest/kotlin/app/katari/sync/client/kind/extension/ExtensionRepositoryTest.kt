package app.katari.sync.client.kind.extension

import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.katari.sync.client.SyncingTestDevice
import app.katari.sync.client.TestSyncServer
import app.katari.sync.client.run.SyncOutcome
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ExtensionRepositoryTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `a repository removed on one device and added again on the other stays on both`(): Unit = runBlocking {
        TestSyncServer(directory).use { server ->
            val account = server.account("reader")
            SyncingTestDevice(directory.resolve("a.db")).use { a ->
                SyncingTestDevice(directory.resolve("b.db")).use { b ->
                    a.join(server, account)
                    a.sync() shouldBe SyncOutcome.SYNCED
                    b.join(server, account)
                    b.sync() shouldBe SyncOutcome.SYNCED

                    a.addRepository()
                    a.sync() shouldBe SyncOutcome.SYNCED
                    b.sync() shouldBe SyncOutcome.SYNCED
                    b.hasRepository() shouldBe true

                    b.local.database.extension_storeQueries.delete(INDEX_URL)
                    b.sync() shouldBe SyncOutcome.SYNCED
                    a.sync() shouldBe SyncOutcome.SYNCED
                    a.hasRepository() shouldBe false

                    a.addRepository()
                    a.sync() shouldBe SyncOutcome.SYNCED
                    b.sync() shouldBe SyncOutcome.SYNCED
                    a.sync() shouldBe SyncOutcome.SYNCED

                    a.hasRepository() shouldBe true
                    b.hasRepository() shouldBe true
                }
            }
        }
    }

    private suspend fun SyncingTestDevice.addRepository() {
        local.database.extension_storeQueries.upsert(
            indexUrl = INDEX_URL,
            name = "Repo",
            badgeLabel = "R",
            signingKey = null,
            contactWebsite = "https://repo",
            contactDiscord = null,
            format = "tachiyomi",
            extensionListUrl = null,
        )
    }

    private suspend fun SyncingTestDevice.hasRepository(): Boolean =
        local.database.extension_storeQueries.contains(INDEX_URL).awaitAsOne()

    private companion object {
        const val INDEX_URL = "https://repo/index.min.json"
    }
}
