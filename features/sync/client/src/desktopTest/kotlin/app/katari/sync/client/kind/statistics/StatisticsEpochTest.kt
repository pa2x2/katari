package app.katari.sync.client.kind.statistics

import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.katari.sync.client.SyncingTestDevice
import app.katari.sync.client.TestSyncServer
import app.katari.sync.client.account.AccountCall
import app.katari.sync.client.firstsync.ProfileChoice
import app.katari.sync.client.firstsync.ProfilePairing
import app.katari.sync.client.firstsync.SettingsWinner
import app.katari.sync.client.run.SyncOutcome
import app.katari.sync.client.showRows
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class StatisticsEpochTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `devices that counted before syncing keep the earliest start`(): Unit = runBlocking {
        withSyncedDevices { a, b ->
            a.statisticsStart() shouldBe AUGUST
            b.statisticsStart() shouldBe AUGUST
        }
    }

    @Test
    fun `clearing history on one device moves the start forward on the other`(): Unit = runBlocking {
        withSyncedDevices { a, b ->
            b.local.database.activityQueries.resetStatisticsEpoch(1, DECEMBER)
            b.sync() shouldBe SyncOutcome.SYNCED
            a.sync() shouldBe SyncOutcome.SYNCED

            a.statisticsStart() shouldBe DECEMBER
        }
    }

    @Test
    fun `a record stored by the previous version is read again from its row`(): Unit = runBlocking {
        withSyncedDevices { a, b ->
            // As an update finds it: the record stored under version 1, and the start moved back by a migration,
            // which no capture records
            a.sql("UPDATE statistics_epochs SET started_at = $JULY")
            a.local.clearOutbox()
            a.sql("UPDATE sync_records SET kind_version = 1 WHERE kind = '${StatisticsEpochKind.name}'")
            a.sync() shouldBe SyncOutcome.SYNCED
            b.sync() shouldBe SyncOutcome.SYNCED

            b.statisticsStart() shouldBe JULY
        }
    }

    private suspend fun withSyncedDevices(block: suspend (SyncingTestDevice, SyncingTestDevice) -> Unit) {
        TestSyncServer(directory).use { server ->
            val account = server.account("reader")
            SyncingTestDevice(directory.resolve("a.db")).use { a ->
                SyncingTestDevice(directory.resolve("b.db")).use { b ->
                    a.sql(*showRows("/show"), startedAt(AUGUST))
                    a.join(server, account)
                    a.sync() shouldBe SyncOutcome.SYNCED

                    b.sql(*showRows("/show"), startedAt(NOVEMBER))
                    b.join(server, account)
                    b.sync()
                    val proposal = b.account.firstSyncProposal()
                        .shouldBeInstanceOf<AccountCall.Done<List<ProfilePairing>>>().value
                    b.account.answerFirstSync(
                        proposal.map { ProfileChoice(it.profileId, it.pairedWith?.uuid, SettingsWinner.ACCOUNT) },
                    )
                    b.sync() shouldBe SyncOutcome.SYNCED
                    a.sync() shouldBe SyncOutcome.SYNCED

                    block(a, b)
                }
            }
        }
    }

    private fun startedAt(millis: Long) = "INSERT INTO statistics_epochs(profile_id, started_at) VALUES (1, $millis)"

    private suspend fun SyncingTestDevice.statisticsStart(): Long =
        local.database.activityQueries.getStatisticsEpoch(1).awaitAsOne()

    private companion object {
        const val JULY = 1_782_864_000_000L
        const val AUGUST = 1_785_542_400_000L
        const val NOVEMBER = 1_793_491_200_000L
        const val DECEMBER = 1_796_083_200_000L
    }
}
