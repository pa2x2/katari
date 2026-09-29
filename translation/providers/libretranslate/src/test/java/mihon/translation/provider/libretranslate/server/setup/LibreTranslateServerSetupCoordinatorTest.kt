package mihon.translation.provider.libretranslate.server.setup

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import mihon.translation.provider.libretranslate.protocol.LibreTranslateLanguage
import mihon.translation.provider.libretranslate.protocol.LibreTranslateService
import org.junit.jupiter.api.Test

class LibreTranslateServerSetupCoordinatorTest {
    @Test
    fun `secure storage failure reports save failure without leaking values`() = runTest {
        val coordinator = LibreTranslateServerSetupCoordinator(
            serviceFactory = { _, _ -> service(listOf(LANGUAGE)) },
            saveConfiguration = { _, _, _ -> error("keystore rejected private-key") },
        )

        coordinator.saveAndTest("https://translate.example", "private-key") shouldBe
            LibreTranslateServerSetupResult.SaveFailed
    }

    @Test
    fun `cancellation propagates without saving`() = runTest {
        var saved = false
        val coordinator = LibreTranslateServerSetupCoordinator(
            serviceFactory = { _, _ ->
                service(onLanguages = { throw CancellationException("cancelled") })
            },
            saveConfiguration = { _, _, _ -> saved = true },
        )

        shouldThrow<CancellationException> {
            coordinator.saveAndTest("https://translate.example", "private-key")
        }
        saved shouldBe false
    }

    private fun service(
        languages: List<LibreTranslateLanguage> = emptyList(),
        onLanguages: (suspend () -> List<LibreTranslateLanguage>)? = null,
    ) = object : LibreTranslateService {
        override suspend fun languages(): List<LibreTranslateLanguage> {
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
