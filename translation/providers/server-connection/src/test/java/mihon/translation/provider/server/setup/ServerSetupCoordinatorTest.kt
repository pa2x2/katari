package mihon.translation.provider.server.setup

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class ServerSetupCoordinatorTest {
    @Test
    fun `secure storage failure reports save failure without leaking values`() = runTest {
        val coordinator = ServerSetupCoordinator(
            verify = { _, _ -> true },
            saveConfiguration = { _, _, _ -> error("keystore rejected private-key") },
        )

        coordinator.saveAndTest("https://translate.example", "private-key") shouldBe
            ServerSetupResult.SaveFailed
    }

    @Test
    fun `cancellation propagates without saving`() = runTest {
        var saved = false
        val coordinator = ServerSetupCoordinator(
            verify = { _, _ -> throw CancellationException("cancelled") },
            saveConfiguration = { _, _, _ -> saved = true },
        )

        shouldThrow<CancellationException> {
            coordinator.saveAndTest("https://translate.example", "private-key")
        }
        saved shouldBe false
    }
}
