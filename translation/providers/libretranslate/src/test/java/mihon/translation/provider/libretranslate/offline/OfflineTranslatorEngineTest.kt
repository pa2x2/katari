package mihon.translation.provider.libretranslate.offline

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import mihon.language.api.tag.LanguageTag
import mihon.translation.api.request.ResolvedTranslationRequest
import mihon.translation.provider.libretranslate.protocol.LibreTranslateLanguage
import mihon.translation.provider.libretranslate.protocol.LibreTranslateService
import mihon.translation.spi.engine.TranslationEnginePreparation
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.api.Test

class OfflineTranslatorEngineTest {

    @Test
    fun `text is not prepared for the provider until its disclosure is accepted`() = runTest {
        val settings = FakeSettings(disclosureAccepted = false)
        val engine = OfflineTranslatorEngine(
            application = FakeApp,
            settings = settings,
            serviceFactory = { FakeService },
        )
        val request = ResolvedTranslationRequest(
            text = "Hello",
            sourceLanguage = LanguageTag.require("en"),
            targetLanguage = LanguageTag.require("fr"),
            engine = OfflineTranslatorEngine.ENGINE_ID,
        )

        engine.prepare(request) shouldBe
            TranslationEnginePreparation.ProviderDisclosureRequired(OfflineTranslatorEngine.DISCLOSURE)

        settings.disclosureAccepted = true
        engine.prepare(request).shouldBeInstanceOf<TranslationEnginePreparation.Ready>()
    }

    private object FakeApp : OfflineTranslatorApp {
        override fun isInstalled() = true
        override fun open() = true
        override fun openInstallationPage() = true
    }

    private class FakeSettings(
        override var disclosureAccepted: Boolean,
    ) : OfflineTranslatorSettings {
        override var port: Int = 5000
        override fun endpoint() = "http://127.0.0.1:$port/".toHttpUrl()
    }

    private object FakeService : LibreTranslateService {
        override suspend fun languages() = listOf(
            LibreTranslateLanguage("en", "en", setOf("fr")),
            LibreTranslateLanguage("fr", "fr", setOf("en")),
        )

        override suspend fun translate(text: String, source: String, target: String) = error("Not used")
    }
}
