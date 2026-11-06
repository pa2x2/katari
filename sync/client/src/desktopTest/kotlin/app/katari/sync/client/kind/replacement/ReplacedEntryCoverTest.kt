package app.katari.sync.client.kind.replacement

import app.katari.sync.client.SyncingTestDevice
import app.katari.sync.client.TestSyncServer
import app.katari.sync.client.run.SyncOutcome
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ReplacedEntryCoverTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `an entry migrated with its custom cover keeps it on the other device`(): Unit = runBlocking {
        TestSyncServer(directory).use { server ->
            val account = server.account("reader")
            SyncingTestDevice(directory.resolve("a.db")).use { a ->
                SyncingTestDevice(directory.resolve("b.db")).use { b ->
                    a.sql(
                        """
                        INSERT INTO source_identities(local_key, id_scheme, external_id)
                        VALUES (-2, 'aniyomi', 777), (-3, 'aniyomi', 888)
                        """,
                        "INSERT INTO entries(source, url, title, favorite, type) VALUES (-2, '/old', 'Show', 1, 'anime')",
                    )
                    val cover = "custom cover".toByteArray()
                    a.local.setCustomCover("/old", cover)
                    a.join(server, account)
                    a.sync() shouldBe SyncOutcome.SYNCED
                    b.join(server, account)
                    b.sync() shouldBe SyncOutcome.SYNCED
                    b.local.customCover("/old") shouldBe cover

                    // A migrates the entry as local migration does with the custom cover option: the target gets a
                    // copy of the cover under its own ID
                    a.sql(
                        "INSERT INTO entries(source, url, title, favorite, type) VALUES (-3, '/new', 'Show', 1, 'anime')",
                        "UPDATE entries SET favorite = 0 WHERE url = '/old'",
                        """
                        INSERT INTO entry_migration_operations(
                            operation_id, intent_fingerprint, profile_id, source_entry_id, target_entry_id, mode,
                            created_at
                        )
                        SELECT 'operation', 'REPLACE:CUSTOM_COVER', 1, Old._id, New._id, 'REPLACE', 0
                        FROM entries Old, entries New WHERE Old.url = '/old' AND New.url = '/new'
                        """,
                    )
                    a.local.setCustomCover("/new", cover)
                    a.sync() shouldBe SyncOutcome.SYNCED
                    b.sync() shouldBe SyncOutcome.SYNCED

                    b.local.customCover("/new") shouldBe cover
                    b.library() shouldBe a.library()
                }
            }
        }
    }
}
