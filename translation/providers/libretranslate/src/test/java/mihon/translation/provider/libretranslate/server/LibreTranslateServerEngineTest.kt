package mihon.translation.provider.libretranslate.server

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import mihon.language.api.tag.LanguageTag
import mihon.translation.api.preparation.TranslationSystemSetupReason
import mihon.translation.api.preparation.TranslationUnavailableReason
import mihon.translation.api.request.ResolvedTranslationRequest
import mihon.translation.provider.libretranslate.protocol.LibreTranslateException
import mihon.translation.provider.libretranslate.protocol.LibreTranslateFailureKind
import mihon.translation.provider.libretranslate.protocol.LibreTranslateLanguage
import mihon.translation.provider.libretranslate.protocol.LibreTranslateService
import mihon.translation.spi.engine.TranslationEngineDeviceAvailability
import mihon.translation.spi.engine.TranslationEngineExecution
import mihon.translation.spi.engine.TranslationEnginePreparation
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.api.Test

class LibreTranslateServerEngineTest {

    @Test
    fun `unverified configuration cannot be selected or used`() = runTest {
        var serviceCreated = false
        val engine = engine(
            settings = FakeSettings(isInitiallyVerified = false),
            serviceFactory = {
                serviceCreated = true
                FakeService()
            },
        )

        engine.inspectDevice() shouldBe
            TranslationEngineDeviceAvailability.ConfigurationRequired(
                LibreTranslateServerEngine.CONFIGURATION_DESCRIPTION,
            )
        engine.prepare(request()) shouldBe setupRequired()
        serviceCreated shouldBe false
    }

    @Test
    fun `later connection failure is transient and preserves verified configuration`() = runTest {
        val settings = FakeSettings(isInitiallyVerified = true)
        val engine = engine(
            settings = settings,
            serviceFactory = {
                FakeService(languageFailure = LibreTranslateException(LibreTranslateFailureKind.Connection))
            },
        )

        engine.inspectDevice() shouldBe
            TranslationEngineDeviceAvailability.Unavailable("Configured server is unreachable")
        settings.isInitiallyVerified shouldBe true
    }

    @Test
    fun `disclosure and server capabilities gate inline translation`() = runTest {
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
            TranslationEnginePreparation.ProviderDisclosureRequired(LibreTranslateServerEngine.DISCLOSURE)

        settings.disclosureAccepted = true
        val ready = engine.prepare(request()) as TranslationEnginePreparation.Ready
        val refreshed = engine.revalidate(ready.request) as TranslationEnginePreparation.Ready
        engine.translate(refreshed.request) shouldBe TranslationEngineExecution.Success("Bonjour")
        service.lastText shouldBe "Hello"
        service.lastSource shouldBe "en"
        service.lastTarget shouldBe "fr"
    }

    @Test
    fun `unsupported individual languages preserve the complete requested pair`() = runTest {
        val engine = engine(
            serviceFactory = {
                FakeService(
                    languages = listOf(
                        language("en", setOf("fr")),
                        language("fr", setOf("en")),
                    ),
                )
            },
        )

        engine.prepare(request(source = CATALAN)) shouldBe TranslationEnginePreparation.Unavailable(
            TranslationUnavailableReason.UnsupportedLanguagePair(CATALAN, FRENCH),
        )
        engine.prepare(request(target = GERMAN)) shouldBe TranslationEnginePreparation.Unavailable(
            TranslationUnavailableReason.UnsupportedLanguagePair(ENGLISH, GERMAN),
        )
    }

    @Test
    fun `server rejection returns to configuration without exposing provider response`() = runTest {
        val service = FakeService(
            languages = listOf(
                language("en", setOf("fr")),
                language("fr"),
            ),
            translationFailure = LibreTranslateException(LibreTranslateFailureKind.Rejected),
        )
        val engine = engine(serviceFactory = { service })
        val ready = engine.prepare(request()) as TranslationEnginePreparation.Ready

        engine.translate(ready.request) shouldBe
            TranslationEngineExecution.PreparationChanged(setupRequired())
    }

    private fun engine(
        settings: FakeSettings = FakeSettings(),
        serviceFactory: () -> LibreTranslateService? = { FakeService() },
    ) = LibreTranslateServerEngine(settings, serviceFactory)

    private fun request(
        source: LanguageTag = ENGLISH,
        target: LanguageTag = FRENCH,
    ) = ResolvedTranslationRequest(
        text = "Hello",
        sourceLanguage = source,
        targetLanguage = target,
        engine = LibreTranslateServerEngine.ENGINE_ID,
    )

    private fun setupRequired() = TranslationEnginePreparation.SystemSetupRequired(
        TranslationSystemSetupReason.ProviderActionRequired(
            LibreTranslateServerEngine.CONFIGURATION_DESCRIPTION,
        ),
    )

    private class FakeSettings(
        override val endpoint: HttpUrl? = "https://translate.example/".toHttpUrl(),
        override val apiKey: String? = null,
        override val isInitiallyVerified: Boolean = true,
        override var disclosureAccepted: Boolean = true,
    ) : LibreTranslateServerSettings

    private class FakeService(
        private val languages: List<LibreTranslateLanguage> = emptyList(),
        private val translatedText: String = "translated",
        private val languageFailure: Exception? = null,
        private val translationFailure: Exception? = null,
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
            translationFailure?.let { throw it }
            lastText = text
            lastSource = source
            lastTarget = target
            return translatedText
        }
    }

    private companion object {
        val ENGLISH = LanguageTag.require("en")
        val FRENCH = LanguageTag.require("fr")
        val CATALAN = LanguageTag.require("ca")
        val GERMAN = LanguageTag.require("de")

        fun language(
            code: String,
            targets: Set<String> = emptySet(),
        ) = LibreTranslateLanguage(code, code, targets)
    }
}
