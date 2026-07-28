package mihon.translation.provider.libretranslate.offline

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import mihon.translation.api.ResolvedTranslationRequest
import mihon.translation.api.TranslationLanguageTag
import mihon.translation.api.TranslationUnavailableReason
import mihon.translation.provider.libretranslate.protocol.LibreTranslateException
import mihon.translation.provider.libretranslate.protocol.LibreTranslateFailureKind
import mihon.translation.provider.libretranslate.protocol.LibreTranslateLanguage
import mihon.translation.provider.libretranslate.protocol.LibreTranslateService
import mihon.translation.spi.TranslationEngineDeviceAvailability
import mihon.translation.spi.TranslationEngineExecution
import mihon.translation.spi.TranslationEnginePreparation
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.api.Test

class OfflineTranslatorEngineTest {

    @Test
    fun `missing provider app is an install requirement without a network probe`() = runTest {
        var serviceCreated = false
        val engine = engine(
            installed = false,
            serviceFactory = {
                serviceCreated = true
                FakeService()
            },
        )

        engine.inspectDevice() shouldBe TranslationEngineDeviceAvailability.NotInstalled
        serviceCreated shouldBe false
    }

    @Test
    fun `installed app requires configuration until its API returns capabilities`() = runTest {
        val engine = engine(
            serviceFactory = {
                FakeService(languageFailure = LibreTranslateException(LibreTranslateFailureKind.Connection))
            },
        )

        engine.inspectDevice() shouldBe
            TranslationEngineDeviceAvailability.ConfigurationRequired(
                OfflineTranslatorEngine.SETUP_DESCRIPTION,
            )
    }

    @Test
    fun `disclosure and provider catalog gate inline translation`() = runTest {
        val settings = FakeSettings(disclosureAccepted = false)
        val service = FakeService(
            languages = listOf(
                language("en", setOf("fr")),
                language("fr", setOf("en")),
            ),
            translatedText = "Bonjour",
        )
        val engine = engine(settings = settings, serviceFactory = { service })

        engine.prepare(request()) shouldBe
            TranslationEnginePreparation.ProviderDisclosureRequired(OfflineTranslatorEngine.DISCLOSURE)

        settings.disclosureAccepted = true
        val ready = engine.prepare(request()) as TranslationEnginePreparation.Ready
        val refreshed = engine.revalidate(ready.request) as TranslationEnginePreparation.Ready
        engine.translate(refreshed.request) shouldBe TranslationEngineExecution.Success("Bonjour")
        service.lastSource shouldBe "en"
        service.lastTarget shouldBe "fr"
        service.lastText shouldBe "Hello"
    }

    @Test
    fun `unsupported pairs remain provider-owned capability failures`() = runTest {
        val engine = engine(
            settings = FakeSettings(disclosureAccepted = true),
            serviceFactory = {
                FakeService(
                    languages = listOf(
                        language("en"),
                        language("fr", setOf("en")),
                    ),
                )
            },
        )

        engine.prepare(request()) shouldBe TranslationEnginePreparation.Unavailable(
            TranslationUnavailableReason.UnsupportedLanguagePair(ENGLISH, FRENCH),
        )
    }

    private fun engine(
        installed: Boolean = true,
        settings: FakeSettings = FakeSettings(),
        serviceFactory: (HttpUrl) -> LibreTranslateService = { FakeService() },
    ): OfflineTranslatorEngine {
        return OfflineTranslatorEngine(
            application = FakeApp(installed),
            settings = settings,
            serviceFactory = serviceFactory,
        )
    }

    private fun request() = ResolvedTranslationRequest(
        text = "Hello",
        sourceLanguage = ENGLISH,
        targetLanguage = FRENCH,
        engine = OfflineTranslatorEngine.ENGINE_ID,
    )

    private class FakeApp(
        private val installed: Boolean,
    ) : OfflineTranslatorApp {
        override fun isInstalled() = installed
        override fun open() = installed
        override fun openInstallationPage() = true
    }

    private class FakeSettings(
        override var port: Int = 5000,
        override var disclosureAccepted: Boolean = true,
    ) : OfflineTranslatorSettings {
        override fun endpoint() = "http://127.0.0.1:$port/".toHttpUrl()
    }

    private class FakeService(
        private val languages: List<LibreTranslateLanguage> = emptyList(),
        private val translatedText: String = "translated",
        private val languageFailure: Exception? = null,
    ) : LibreTranslateService {
        var lastText: String? = null
        var lastSource: String? = null
        var lastTarget: String? = null

        override suspend fun languages(): List<LibreTranslateLanguage> {
            languageFailure?.let { throw it }
            return languages
        }

        override suspend fun translate(
            text: String,
            source: String,
            target: String,
        ): String {
            lastText = text
            lastSource = source
            lastTarget = target
            return translatedText
        }
    }

    private companion object {
        val ENGLISH = TranslationLanguageTag.require("en")
        val FRENCH = TranslationLanguageTag.require("fr")

        fun language(
            code: String,
            targets: Set<String> = emptySet(),
        ) = LibreTranslateLanguage(code, code, targets)
    }
}
