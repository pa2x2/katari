package app.katari.sync.client.readelsewhere

import app.katari.sync.client.RecordExchange
import app.katari.sync.client.SyncTestDevice
import app.katari.sync.client.rows
import app.katari.sync.client.showRows
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ChaptersReadElsewhereTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `only chapters another device's read turned read here are told, once`(): Unit = runBlocking {
        val exchange = RecordExchange()
        SyncTestDevice(directory.resolve("a.db")).use { a ->
            SyncTestDevice(directory.resolve("b.db")).use { b ->
                a.startSync(deviceId = 1)
                b.startSync(deviceId = 2)
                a.sql(*showRows("/show", episodes = 3), "UPDATE chapters SET read = 1 WHERE url != '/show/1'")
                b.sql(*showRows("/show", episodes = 3, read = 1))
                b.sql("UPDATE chapters SET read = 1 WHERE url = '/show/3'")
                val readByA = exchange.pushFrom(a)
                exchange.deliver(b, readByA)
                exchange.deliver(b, readByA)

                val episode2 = b.rows("SELECT _id FROM chapters WHERE url = '/show/2'").single().toLong()
                b.readElsewhere shouldBe listOf(ChaptersReadElsewhere(b.entryId("/show"), setOf(episode2)))
            }
        }
    }
}
