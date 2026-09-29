package mihon.translation.runtime.system

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import mihon.language.api.tag.LanguageTag
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultAndroidSystemTranslationPlatformTest {
    @Test
    fun `a regional request can use a language-only capability but not another region's`() = runTest {
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

        DefaultAndroidSystemTranslationPlatform(31, FakeBridge(listOf(capability("pt-BR", "en"))))
            .inspect(pair("pt-PT", "en")) shouldBe AndroidSystemTranslationInspection.UnsupportedPair
    }

    @Test
    fun `only losing the translated pair's capability cancels translation and frees the translator`() = runTest {
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
        bridge.listener?.invoke(capability("de", "pl", AndroidSystemCapabilityState.Unavailable))
        runCurrent()
        bridge.cancellation.cancelled shouldBe false

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

    private class FakeBridge(
        private val capabilities: List<AndroidTranslationManagerCapability>,
        var translator: FakeTranslator? = null,
    ) : AndroidTranslationManagerBridge {
        var listener: ((AndroidTranslationManagerCapability) -> Unit)? = null
        var closedRegistrations = 0
        var createdPair: Pair<String, String>? = null
        val cancellation = FakeCancellation()

        override suspend fun capabilities(): List<AndroidTranslationManagerCapability> = capabilities

        override suspend fun openSettings() = AndroidSystemPlatformSetup.Opened

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
            LanguageTag.require(source),
            LanguageTag.require(target),
        )

        fun capability(
            source: String,
            target: String,
            state: AndroidSystemCapabilityState = AndroidSystemCapabilityState.OnDevice,
        ) = AndroidTranslationManagerCapability(source, target, state)
    }
}
