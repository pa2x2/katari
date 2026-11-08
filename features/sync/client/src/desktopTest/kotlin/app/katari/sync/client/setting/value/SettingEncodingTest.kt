package app.katari.sync.client.setting.value

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.katari.sync.client.SyncTestDevice
import app.katari.sync.client.kind.setting.ProfileSettingKind
import app.katari.sync.client.kind.setting.ProfileSettingPayload
import app.katari.sync.client.payload.SourceReference
import app.katari.sync.client.payload.Stamped
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class SettingEncodingTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `a preference naming sources syncs alike from devices that key those sources differently`(): Unit =
        runBlocking {
            val records = listOf("phone" to -2L, "desktop" to -5L).map { (name, animeKey) ->
                SyncTestDevice(directory.resolve("$name.db")).use { device ->
                    device.sql(
                        "INSERT INTO source_identities(local_key, id_scheme, external_id) " +
                            "VALUES ($animeKey, 'aniyomi', 777), (-9, 'lnreader', 31), (6000, 'tachiyomi', 6000)",
                    )
                    device.startSync(deviceId = 1)
                    val store = device.preferences.profiles.profileStore(1)
                    store.getStringSet("hidden_sources").set(setOf("$animeKey", "6000", "-9"))
                    store.getString("pref_display_mode_catalogue_$animeKey").set("LIST")
                    device.awaitDrained(records = 2)
                }
            }

            records[0] shouldBe records[1]
            records[0].values.toSet() shouldBe setOf(
                ProfileSettingPayload(
                    profileUuid = "default",
                    key = "hidden_sources",
                    value = stamped(
                        SettingValue(
                            stringSet = StringSetValue(listOf("0", "1", "2")),
                            sources = listOf(
                                SourceReference("aniyomi", 777),
                                SourceReference("lnreader", 31),
                                SourceReference("tachiyomi", 6000),
                            ),
                        ),
                    ),
                ),
                ProfileSettingPayload(
                    profileUuid = "default",
                    key = "pref_display_mode_catalogue_0",
                    keySources = listOf(SourceReference("aniyomi", 777)),
                    value = stamped(SettingValue(string = "LIST")),
                ),
            )
        }

    /** The profile setting records once the outbox is drained, by record key, without their edit times. */
    private suspend fun SyncTestDevice.awaitDrained(records: Int): Map<String, ProfileSettingPayload> =
        withTimeout(5_000) {
            var stored = emptyMap<String, ProfileSettingPayload>()
            while (stored.size < records) {
                delay(10)
                drainAll()
                stored = handler.await {
                    sync_recordsQueries.pendingAfter("", Long.MAX_VALUE).awaitAsList()
                        .filter { it.kind == ProfileSettingKind.name }
                        .associate { it.record_key to ProfileSettingKind.decode(it.payload) }
                }
            }
            stored.mapValues { (_, payload) -> payload.copy(value = stamped(payload.value.value)) }
        }

    private fun stamped(value: SettingValue) = Stamped(value, Stamped.UNKNOWN_EDIT)
}
