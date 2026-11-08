package app.katari.sync.client.apply

import app.katari.sync.client.RecordExchange
import app.katari.sync.client.SyncTestDevice
import app.katari.sync.client.kind.entry.EntryKind
import app.katari.sync.protocol.record.SequencedRecord
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class HeldRecordsTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `a record in a newer format waits, is reported, and this device's edits don't overwrite it`(): Unit =
        runBlocking {
            val exchange = RecordExchange()
            SyncTestDevice(directory.resolve("a.db")).use { a ->
                SyncTestDevice(directory.resolve("b.db")).use { b ->
                    a.startSync(deviceId = 1)
                    b.startSync(deviceId = 2)
                    a.sql(
                        "INSERT INTO source_identities(local_key, id_scheme, external_id) VALUES (-2, 'aniyomi', 777)",
                        "INSERT INTO entries(_id, source, url, title, favorite, type) VALUES (1, -2, '/show', 'Show', 1, 'anime')",
                    )
                    val entry = exchange.pushFrom(a).single { it.record.kind == EntryKind.name }
                    exchange.deliver(b, listOf(entry))

                    // A newer app on another device rewrote the entry in a format this app can't read
                    val newer = SequencedRecord(
                        entry.sequence + 100,
                        entry.record.copy(kindVersion = EntryKind.version + 1, payload = byteArrayOf(0x7f)),
                    )
                    exchange.deliver(b, listOf(newer))
                    b.held.unreadableKinds().first() shouldBe listOf(EntryKind.name)

                    b.sql("UPDATE entries SET notes = 'Edited here'")
                    b.drainAll()
                    b.incoming.retryHeld()

                    b.held.unreadableKinds().first() shouldBe listOf(EntryKind.name)
                    b.pending.next(Long.MAX_VALUE).filter { it.key == entry.record.key }.shouldBeEmpty()
                }
            }
        }
}
