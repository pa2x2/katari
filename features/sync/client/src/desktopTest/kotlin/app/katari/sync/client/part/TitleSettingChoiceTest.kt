package app.katari.sync.client.part

import app.katari.sync.client.DOUBLE_TAP_ANIMATION
import app.katari.sync.client.DOUBLE_TAP_ANIMATION_PREFERENCE
import app.katari.sync.client.MANGA_READER
import app.katari.sync.client.SyncingTestDevice
import app.katari.sync.client.TestSyncServer
import app.katari.sync.client.rows
import app.katari.sync.client.run.SyncOutcome
import app.katari.sync.client.showRows
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class TitleSettingChoiceTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `a title's value of a setting kept on a device neither leaves nor arrives there until the setting syncs`():
        Unit = runBlocking {
        TestSyncServer(directory).use { server ->
            val account = server.account("reader")
            SyncingTestDevice(directory.resolve("a.db")).use { a ->
                SyncingTestDevice(directory.resolve("b.db")).use { b ->
                    a.sql(*showRows("/show"), titleValue(DOUBLE_TAP_ANIMATION, "250"))
                    a.join(server, account)
                    a.sync() shouldBe SyncOutcome.SYNCED
                    b.join(server, account)
                    b.sync() shouldBe SyncOutcome.SYNCED
                    b.titleValue(DOUBLE_TAP_ANIMATION) shouldBe "250"
                    b.choiceChanges.syncSetting(DOUBLE_TAP_ANIMATION_PREFERENCE, on = false)

                    a.sql("UPDATE viewer_setting_overrides SET encoded_value = '100'")
                    a.sync() shouldBe SyncOutcome.SYNCED
                    b.sync() shouldBe SyncOutcome.SYNCED
                    b.titleValue(DOUBLE_TAP_ANIMATION) shouldBe "250"

                    // Made after A's, so it wins once B syncs the setting again
                    b.sql("UPDATE viewer_setting_overrides SET encoded_value = '300'")
                    b.sync() shouldBe SyncOutcome.SYNCED
                    a.sync() shouldBe SyncOutcome.SYNCED
                    a.titleValue(DOUBLE_TAP_ANIMATION) shouldBe "100"

                    b.choiceChanges.syncSetting(DOUBLE_TAP_ANIMATION_PREFERENCE, on = true)
                    b.sync() shouldBe SyncOutcome.SYNCED
                    a.sync() shouldBe SyncOutcome.SYNCED
                    a.titleValue(DOUBLE_TAP_ANIMATION) shouldBe "300"
                    b.titleValue(DOUBLE_TAP_ANIMATION) shouldBe "300"
                }
            }
        }
    }

    @Test
    fun `a title's value of a setting this device doesn't have follows Title choices alone`(): Unit = runBlocking {
        TestSyncServer(directory).use { server ->
            val account = server.account("reader")
            SyncingTestDevice(directory.resolve("a.db")).use { a ->
                SyncingTestDevice(directory.resolve("b.db")).use { b ->
                    a.sql(*showRows("/show"))
                    a.join(server, account)
                    a.sync() shouldBe SyncOutcome.SYNCED
                    b.join(server, account)
                    b.sync() shouldBe SyncOutcome.SYNCED
                    b.choiceChanges.syncSetting(DOUBLE_TAP_ANIMATION_PREFERENCE, on = false)

                    a.sql(titleValue(UNKNOWN_SETTING, "on"))
                    a.sync() shouldBe SyncOutcome.SYNCED
                    b.sync() shouldBe SyncOutcome.SYNCED
                    b.titleValue(UNKNOWN_SETTING) shouldBe "on"
                }
            }
        }
    }

    private fun titleValue(settingKey: String, value: String) =
        """
        INSERT INTO viewer_setting_overrides(entry_id, provider_id, setting_key, encoded_value, updated_at)
        SELECT _id, '$MANGA_READER', '$settingKey', '$value', 0 FROM entries
        """

    private suspend fun SyncingTestDevice.titleValue(settingKey: String): String? = local.rows(
        "SELECT encoded_value FROM viewer_setting_overrides WHERE setting_key = '$settingKey'",
    ).singleOrNull()

    private companion object {
        const val UNKNOWN_SETTING = "a_setting_of_a_newer_reader"
    }
}
