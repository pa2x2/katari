package mihon.translation.provider.libretranslate.offline.setup

import io.kotest.assertions.throwables.shouldThrow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import mihon.translation.provider.libretranslate.offline.OfflineTranslatorConfiguration
import mihon.translation.provider.libretranslate.offline.OfflineTranslatorSettings
import mihon.translation.provider.libretranslate.protocol.LibreTranslateLanguage
import mihon.translation.provider.libretranslate.protocol.LibreTranslateService
import okhttp3.HttpUrl
import org.junit.jupiter.api.Test

class OfflineTranslatorSetupCoordinatorTest {
    @Test
    fun `cancellation propagates`() = runTest {
        val coordinator = OfflineTranslatorSetupCoordinator(FakeSettings()) {
            service(onLanguages = { throw CancellationException("cancelled") })
        }

        shouldThrow<CancellationException> {
            coordinator.test("5000")
        }
    }

    private class FakeSettings : OfflineTranslatorSettings {
        override var port = OfflineTranslatorConfiguration.DEFAULT_PORT
        override var disclosureAccepted = false

        override fun endpoint(): HttpUrl {
            return HttpUrl.Builder()
                .scheme("http")
                .host(OfflineTranslatorConfiguration.LOOPBACK_HOST)
                .port(port)
                .build()
        }
    }

    private fun service(onLanguages: suspend () -> List<LibreTranslateLanguage>) = object : LibreTranslateService {
        override suspend fun languages(): List<LibreTranslateLanguage> = onLanguages()

        override suspend fun translate(
            text: String,
            source: String,
            target: String,
        ) = error("Not used")
    }
}
