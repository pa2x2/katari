package app.katari.sync.client.account

import app.katari.sync.client.SyncingTestDevice
import app.katari.sync.client.TestSyncServer
import app.katari.sync.client.connection.SyncStop
import app.katari.sync.client.run.SyncOutcome
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class SignInAgainTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `signing in again removes the device's earlier registration from the account`(): Unit = runBlocking {
        TestSyncServer(directory).use { server ->
            val account = server.account("reader")
            SyncingTestDevice(directory.resolve("a.db")).use { a ->
                a.join(server, account)
                a.sync() shouldBe SyncOutcome.SYNCED
                val earlier = a.deviceId()

                // As when the server stopped taking its token
                a.connection.forgetToken()
                a.connection.stop(SyncStop.SIGN_IN_AGAIN)
                a.join(server, account)
                a.sync() shouldBe SyncOutcome.SYNCED

                a.deviceId() shouldNotBe earlier
                a.client().devices().devices.map { it.id } shouldBe listOf(a.deviceId())
            }
        }
    }
}
