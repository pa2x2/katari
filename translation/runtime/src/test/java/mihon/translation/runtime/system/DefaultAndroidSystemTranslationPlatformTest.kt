package mihon.translation.runtime.system

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import mihon.translation.api.TranslationLanguageTag
import org.junit.jupiter.api.Test

class DefaultAndroidSystemTranslationPlatformTest {
    @Test
    fun `device inspection only gates OS and service presence`() = runTest {
        DefaultAndroidSystemTranslationPlatform(31, FakeBridge(emptyList())).inspectDevice() shouldBe
            AndroidSystemDeviceInspection.Available
        DefaultAndroidSystemTranslationPlatform(30, null).inspectDevice() shouldBe
            AndroidSystemDeviceInspection.UnsupportedOs
        DefaultAndroidSystemTranslationPlatform(31, null).inspectDevice() shouldBe
            AndroidSystemDeviceInspection.ServiceMissing
    }

    @Test
    fun `regional request can use a provider language-only capability`() = runTest {
        val bridge = FakeBridge(
            capabilities = listOf(capability("en", "pl")),
            translator = FakeTranslator(AndroidTranslationManagerResult.Success("Cześć")),
        )
        val platform = DefaultAndroidSystemTranslationPlatform(31, bridge)

        platform.inspect(pair("en-US", "pl-PL")) shouldBe
            AndroidSystemTranslationInspection.Capability(AndroidSystemCapabilityState.OnDevice)
        platform.translate(pair("en-US", "pl-PL"), "Hello") shouldBe
            AndroidSystemPlatformExecution.Success("Cześć")

        bridge.createdPair shouldBe ("en" to "pl")
        bridge.closedRegistrations shouldBe 1
        bridge.translator?.destroyed shouldBe true
    }

    @Test
    fun `region-specific provider capability is not guessed for a different region`() = runTest {
        val bridge = FakeBridge(
            capabilities = listOf(capability("pt-BR", "en")),
        )
        val platform = DefaultAndroidSystemTranslationPlatform(31, bridge)

        platform.inspect(pair("pt-PT", "en")) shouldBe AndroidSystemTranslationInspection.UnsupportedPair
    }

    @Test
    fun `capability loss cancels active translation and releases provider resources`() = runTest {
        val translator = FakeTranslator()
        val bridge = FakeBridge(
            capabilities = listOf(capability("en", "pl")),
            translator = translator,
        )
        val platform = DefaultAndroidSystemTranslationPlatform(31, bridge)

        val execution = async {
            platform.translate(pair("en", "pl"), "Hello")
        }
        runCurrent()
        bridge.listener?.invoke(
            capability("en", "pl", AndroidSystemCapabilityState.Downloading),
        )

        execution.await() shouldBe AndroidSystemPlatformExecution.CapabilityChanged(
            AndroidSystemTranslationInspection.Capability(AndroidSystemCapabilityState.Downloading),
        )
        bridge.cancellation.cancelled shouldBe true
        translator.destroyed shouldBe true
        bridge.closedRegistrations shouldBe 1
    }

    @Test
    fun `unrelated capability update does not interrupt translation`() = runTest {
        val translator = FakeTranslator()
        val bridge = FakeBridge(
            capabilities = listOf(capability("en", "pl")),
            translator = translator,
        )
        val platform = DefaultAndroidSystemTranslationPlatform(31, bridge)

        val execution = async {
            platform.translate(pair("en", "pl"), "Hello")
        }
        runCurrent()
        bridge.listener?.invoke(
            capability("de", "pl", AndroidSystemCapabilityState.Unavailable),
        )
        translator.result.complete(AndroidTranslationManagerResult.Success("Cześć"))

        execution.await() shouldBe AndroidSystemPlatformExecution.Success("Cześć")
        bridge.cancellation.cancelled shouldBe false
        translator.destroyed shouldBe true
        bridge.closedRegistrations shouldBe 1
    }

    @Test
    fun `unsupported OS and missing service are reported without provider access`() = runTest {
        DefaultAndroidSystemTranslationPlatform(30, null).inspect(pair("en", "pl")) shouldBe
            AndroidSystemTranslationInspection.UnsupportedOs
        DefaultAndroidSystemTranslationPlatform(31, null).inspect(pair("en", "pl")) shouldBe
            AndroidSystemTranslationInspection.ServiceMissing
    }

    private class FakeBridge(
        private val capabilities: List<AndroidTranslationManagerCapability>,
        var translator: FakeTranslator? = null,
        private val hasSettings: Boolean = true,
    ) : AndroidTranslationManagerBridge {
        var listener: ((AndroidTranslationManagerCapability) -> Unit)? = null
        var closedRegistrations = 0
        var createdPair: Pair<String, String>? = null
        val cancellation = FakeCancellation()

        override suspend fun capabilities(): List<AndroidTranslationManagerCapability> = capabilities

        override suspend fun openSettings(): AndroidSystemPlatformSetup {
            return if (hasSettings) {
                AndroidSystemPlatformSetup.Opened
            } else {
                AndroidSystemPlatformSetup.SettingsUnavailable
            }
        }

        override fun observeCapabilities(
            listener: (AndroidTranslationManagerCapability) -> Unit,
        ): AndroidTranslationCapabilityRegistration {
            this.listener = listener
            return AndroidTranslationCapabilityRegistration {
                this.listener = null
                closedRegistrations++
            }
        }

        override fun createCancellation(): AndroidTranslationCancellation = cancellation

        override suspend fun createTranslator(
            sourceLanguageTag: String,
            targetLanguageTag: String,
        ): AndroidTranslationManagerTranslator? {
            createdPair = sourceLanguageTag to targetLanguageTag
            return translator
        }
    }

    private class FakeTranslator(
        initialResult: AndroidTranslationManagerResult? = null,
    ) : AndroidTranslationManagerTranslator {
        val result = CompletableDeferred<AndroidTranslationManagerResult>()
        var destroyed = false

        init {
            initialResult?.let(result::complete)
        }

        override suspend fun translate(
            text: String,
            cancellation: AndroidTranslationCancellation,
        ): AndroidTranslationManagerResult {
            return result.await()
        }

        override fun destroy() {
            destroyed = true
        }
    }

    private class FakeCancellation : AndroidTranslationCancellation {
        var cancelled = false

        override fun cancel() {
            cancelled = true
        }
    }

    private companion object {
        fun pair(source: String, target: String) = AndroidSystemTranslationPair(
            TranslationLanguageTag.require(source),
            TranslationLanguageTag.require(target),
        )

        fun capability(
            source: String,
            target: String,
            state: AndroidSystemCapabilityState = AndroidSystemCapabilityState.OnDevice,
        ) = AndroidTranslationManagerCapability(source, target, state)
    }
}
