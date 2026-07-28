package mihon.translation.provider.libretranslate.server.setup

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import mihon.translation.provider.libretranslate.protocol.LibreTranslateLanguage
import mihon.translation.provider.libretranslate.protocol.LibreTranslateService
import okhttp3.HttpUrl
import org.junit.jupiter.api.Test

class LibreTranslateServerSetupCoordinatorTest {
    @Test
    fun `invalid or insecure endpoints never probe or save`() = runTest {
        var probes = 0
        var saves = 0
        val coordinator = LibreTranslateServerSetupCoordinator(
            serviceFactory = { _, _ ->
                probes += 1
                service(listOf(LANGUAGE))
            },
            saveConfiguration = { _, _, _ -> saves += 1 },
        )

        listOf(
            "not a URL",
            "http://translate.example",
            "https://user:password@translate.example",
            "https://translate.example/?key=private",
            "https://translate.example/#private",
        ).forEach { endpoint ->
            coordinator.saveAndTest(endpoint, "private-key") shouldBe
                LibreTranslateServerSetupResult.InvalidEndpoint
        }

        probes shouldBe 0
        saves shouldBe 0
    }

    @Test
    fun `API key reaches the client separately from the endpoint`() = runTest {
        var clientEndpoint: HttpUrl? = null
        var clientApiKey: String? = null
        val coordinator = LibreTranslateServerSetupCoordinator(
            serviceFactory = { endpoint, apiKey ->
                clientEndpoint = endpoint
                clientApiKey = apiKey
                service(listOf(LANGUAGE))
            },
            saveConfiguration = { _, _, _ -> },
        )

        coordinator.saveAndTest("https://translate.example/api", " private-key ") shouldBe
            LibreTranslateServerSetupResult.Ready

        clientEndpoint?.query shouldBe null
        clientEndpoint?.username shouldBe ""
        clientEndpoint?.password shouldBe ""
        clientApiKey shouldBe "private-key"
    }

    @Test
    fun `successful test stores verified configuration`() = runTest {
        var saved: SavedConfiguration? = null
        val coordinator = LibreTranslateServerSetupCoordinator(
            serviceFactory = { _, _ -> service(listOf(LANGUAGE)) },
            saveConfiguration = { endpoint, apiKey, verified ->
                saved = SavedConfiguration(endpoint, apiKey, verified)
            },
        )

        coordinator.saveAndTest("https://translate.example", "key") shouldBe
            LibreTranslateServerSetupResult.Ready

        saved?.endpoint?.toString() shouldBe "https://translate.example/"
        saved?.apiKey shouldBe "key"
        saved?.verified shouldBe true
    }

    @Test
    fun `failed retest retains attempted configuration and clears verification`() = runTest {
        var saved: SavedConfiguration? = null
        val coordinator = LibreTranslateServerSetupCoordinator(
            serviceFactory = { _, _ -> service(emptyList()) },
            saveConfiguration = { endpoint, apiKey, verified ->
                saved = SavedConfiguration(endpoint, apiKey, verified)
            },
        )

        coordinator.saveAndTest("https://translate.example/new", "new-key") shouldBe
            LibreTranslateServerSetupResult.ConnectionFailed

        saved?.endpoint?.toString() shouldBe "https://translate.example/new/"
        saved?.apiKey shouldBe "new-key"
        saved?.verified shouldBe false
    }

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

    private data class SavedConfiguration(
        val endpoint: HttpUrl,
        val apiKey: String?,
        val verified: Boolean,
    )

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
