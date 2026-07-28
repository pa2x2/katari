package mihon.translation.provider.libretranslate.offline.setup

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
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
    fun `invalid ports never persist or probe`() = runTest {
        val settings = FakeSettings()
        var probes = 0
        val coordinator = OfflineTranslatorSetupCoordinator(settings) {
            probes += 1
            service(languages = listOf(LANGUAGE))
        }

        coordinator.test("0") shouldBe OfflineTranslatorSetupResult.InvalidPort
        coordinator.test("65536") shouldBe OfflineTranslatorSetupResult.InvalidPort
        coordinator.test("not-a-port") shouldBe OfflineTranslatorSetupResult.InvalidPort

        probes shouldBe 0
        settings.port shouldBe OfflineTranslatorConfiguration.DEFAULT_PORT
    }

    @Test
    fun `valid port persists before probing`() = runTest {
        val settings = FakeSettings()
        var probedEndpoint: HttpUrl? = null
        val coordinator = OfflineTranslatorSetupCoordinator(settings) { endpoint ->
            probedEndpoint = endpoint
            service(languages = listOf(LANGUAGE))
        }

        coordinator.test("4242") shouldBe OfflineTranslatorSetupResult.Ready

        settings.port shouldBe 4242
        probedEndpoint?.toString() shouldBe "http://127.0.0.1:4242/"
    }

    @Test
    fun `empty capability catalog fails readiness`() = runTest {
        val coordinator = OfflineTranslatorSetupCoordinator(FakeSettings()) {
            service(languages = emptyList())
        }

        coordinator.test("5000") shouldBe OfflineTranslatorSetupResult.ConnectionFailed
    }

    @Test
    fun `connection failure retains the attempted port`() = runTest {
        val settings = FakeSettings()
        val coordinator = OfflineTranslatorSetupCoordinator(settings) {
            service(failure = IllegalStateException("private provider response"))
        }

        coordinator.test("4321") shouldBe OfflineTranslatorSetupResult.ConnectionFailed
        settings.port shouldBe 4321
    }

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

    private fun service(
        languages: List<LibreTranslateLanguage> = emptyList(),
        failure: Exception? = null,
        onLanguages: (suspend () -> List<LibreTranslateLanguage>)? = null,
    ) = object : LibreTranslateService {
        override suspend fun languages(): List<LibreTranslateLanguage> {
            failure?.let { throw it }
            return onLanguages?.invoke() ?: languages
        }

        override suspend fun translate(
            text: String,
            source: String,
            target: String,
        ) = error("Not used")
    }

    private companion object {
        val LANGUAGE = LibreTranslateLanguage(
            code = "en",
            name = "English",
            targets = setOf("pl"),
        )
    }
}
