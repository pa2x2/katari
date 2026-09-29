package mihon.translation.provider.libretranslate.server

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import mihon.language.api.tag.LanguageTag
import mihon.translation.api.preparation.TranslationSystemSetupReason
import mihon.translation.api.request.ResolvedTranslationRoute
import mihon.translation.provider.libretranslate.protocol.LibreTranslateLanguage
import mihon.translation.provider.libretranslate.protocol.LibreTranslateService
import mihon.translation.spi.engine.TranslationEngineDeviceAvailability
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
        engine.prepare(route()) shouldBe setupRequired()
        serviceCreated shouldBe false
    }

    @Test
    fun `text is not prepared for the server until its disclosure is accepted`() = runTest {
        val settings = FakeSettings(disclosureAccepted = false)
        val service = FakeService(
            languages = listOf(
                language("en", setOf("fr")),
                language("fr", setOf("en")),
            ),
        )
        val engine = engine(settings = settings, serviceFactory = { service })

        engine.prepare(route()) shouldBe
            TranslationEnginePreparation.ProviderDisclosureRequired(LibreTranslateServerEngine.DISCLOSURE)

        settings.disclosureAccepted = true
        engine.prepare(route()).shouldBeInstanceOf<TranslationEnginePreparation.Ready>()
    }

    private fun engine(
        settings: FakeSettings = FakeSettings(),
        serviceFactory: () -> LibreTranslateService? = { FakeService() },
    ) = LibreTranslateServerEngine(settings, serviceFactory)

    private fun route(
        source: LanguageTag = ENGLISH,
        target: LanguageTag = FRENCH,
    ) = ResolvedTranslationRoute(
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
    ) : LibreTranslateService {
        override suspend fun languages(): List<LibreTranslateLanguage> {
            return languages
        }

        override suspend fun translate(
            text: String,
            source: String,
            target: String,
        ): String {
            return "translated"
        }
    }

    private companion object {
        val ENGLISH = LanguageTag.require("en")
        val FRENCH = LanguageTag.require("fr")

        fun language(
            code: String,
            targets: Set<String> = emptySet(),
        ) = LibreTranslateLanguage(code, code, targets)
    }
}
