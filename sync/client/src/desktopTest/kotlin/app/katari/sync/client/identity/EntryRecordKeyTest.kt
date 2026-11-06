package app.katari.sync.client.identity

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.katari.sync.client.SyncTestDevice
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class EntryRecordKeyTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `an entry added on two devices has one key though its row id and the source's local key differ`() = runTest {
        val phone = SyncTestDevice(directory.resolve("phone.db"))
        val desktop = SyncTestDevice(directory.resolve("desktop.db"))
        phone.use { phone ->
            desktop.use { desktop ->
                phone.startSync(deviceId = 1)
                phone.sql(
                    "INSERT INTO source_identities(local_key, id_scheme, external_id) VALUES (-2, 'aniyomi', 777)",
                    """
                    INSERT INTO entries(_id, profile_id, source, url, title, favorite, type)
                    VALUES (1, 1, -2, '/show', 'Show', 1, 'anime')
                    """,
                )
                desktop.startSync(deviceId = 2)
                desktop.sql(
                    "INSERT INTO source_identities(local_key, id_scheme, external_id) VALUES (-2, 'lnreader', 5)",
                    "INSERT INTO source_identities(local_key, id_scheme, external_id) VALUES (-3, 'aniyomi', 777)",
                    """
                    INSERT INTO entries(_id, profile_id, source, url, title, favorite, type)
                    VALUES (7, 1, -3, '/show', 'Show', 1, 'anime')
                    """,
                )

                phone.drainAll()
                desktop.drainAll()

                // SHA-256 of 0x01 and the length-prefixed parts entry, default, aniyomi, 777, /show, anime, cut to
                // 16 bytes: a change to the derivation must fail here, since other devices and versions keep it.
                phone.entryKeys() shouldBe listOf("YP93zm6ey-TbuXHTzyenAQ")
                desktop.entryKeys() shouldBe listOf("YP93zm6ey-TbuXHTzyenAQ")
            }
        }
    }

    private suspend fun SyncTestDevice.entryKeys(): List<String> = handler.await {
        sync_recordsQueries.pending(Long.MAX_VALUE).awaitAsList().filter { it.kind == "entry" }.map { it.record_key }
    }
}
