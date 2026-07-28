package mihon.translation.ui.session

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import mihon.translation.api.ReadyTranslation
import mihon.translation.api.ResolvedTranslationRequest
import mihon.translation.api.TranslationEngineId
import mihon.translation.api.TranslationExecution
import mihon.translation.api.TranslationFeature
import mihon.translation.api.TranslationInvocationPolicy
import mihon.translation.api.TranslationLanguageTag
import mihon.translation.api.TranslationPreparation
import mihon.translation.api.TranslationProviderId
import mihon.translation.api.TranslationProviderPresentation
import mihon.translation.api.TranslationRequest
import mihon.translation.api.TranslationResult
import mihon.translation.api.TranslationSourceLanguageSelection
import mihon.translation.api.TranslationTargetLanguageSelection
import org.junit.jupiter.api.Test

class TranslationSessionControllerTest {

    @Test
    fun `selection settling prepares only the latest changed request`() = runTest {
        val feature = FakeTranslationFeature()
        val controller = TranslationSessionController(feature, backgroundScope)

        controller.submit(input("first"))
        advanceTimeBy(249)
        feature.preparedTexts shouldBe emptyList()

        controller.submit(input("second"))
        advanceTimeBy(250)
        runCurrent()

        feature.preparedTexts shouldContainExactly listOf("second")
        (controller.state.value as TranslationSessionState.Ready).input.request.text shouldBe "second"
    }

    @Test
    fun `immediate provider executes automatically and publishes its result`() = runTest {
        val feature = FakeTranslationFeature(invocationPolicy = TranslationInvocationPolicy.Immediate)
        val controller = TranslationSessionController(feature, backgroundScope, selectionSettleDelayMillis = 0)

        controller.submit(input("hello"))
        runCurrent()

        feature.translatedTexts shouldContainExactly listOf("hello")
        val success = controller.state.value as TranslationSessionState.Success
        success.result.translatedText shouldBe "translated hello"
    }

    @Test
    fun `explicit provider waits for its declared action`() = runTest {
        val feature = FakeTranslationFeature(
            invocationPolicy = TranslationInvocationPolicy.ExplicitAction("Translate"),
        )
        val controller = TranslationSessionController(feature, backgroundScope, selectionSettleDelayMillis = 0)

        controller.submit(input("hello"))
        runCurrent()

        controller.state.value.shouldBeInstanceOf<TranslationSessionState.Ready>()
        feature.translatedTexts shouldBe emptyList()

        controller.execute()
        runCurrent()

        feature.translatedTexts shouldContainExactly listOf("hello")
        controller.state.value.shouldBeInstanceOf<TranslationSessionState.Success>()
    }

    @Test
    fun `cancelled non-cooperative preparation cannot publish stale state`() = runTest {
        val firstGate = CompletableDeferred<Unit>()
        val secondGate = CompletableDeferred<Unit>()
        val feature = FakeTranslationFeature(
            prepareOverride = { request ->
                withContext(NonCancellable) {
                    when (request.text) {
                        "first" -> firstGate.await()
                        "second" -> secondGate.await()
                    }
                }
                ready(request)
            },
        )
        val controller = TranslationSessionController(feature, backgroundScope, selectionSettleDelayMillis = 0)

        controller.submit(input("first"))
        runCurrent()
        controller.submit(input("second"))
        runCurrent()

        secondGate.complete(Unit)
        runCurrent()
        (controller.state.value as TranslationSessionState.Ready).input.request.text shouldBe "second"

        firstGate.complete(Unit)
        runCurrent()
        (controller.state.value as TranslationSessionState.Ready).input.request.text shouldBe "second"
    }

    @Test
    fun `anchor-only update does not repeat provider work`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val feature = FakeTranslationFeature(
            prepareOverride = { request ->
                gate.await()
                ready(request)
            },
        )
        val controller = TranslationSessionController(feature, backgroundScope, selectionSettleDelayMillis = 0)
        controller.submit(input("hello"))
        runCurrent()

        val anchor = TranslationSelectionAnchor(10f, 20f, 30f, 40f)
        controller.updateAnchor(anchor)
        gate.complete(Unit)
        runCurrent()

        feature.preparedTexts shouldContainExactly listOf("hello")
        (controller.state.value as TranslationSessionState.Ready).input.anchor shouldBe anchor
    }

    @Test
    fun `preparation change after execution waits for user instead of looping`() = runTest {
        val feature = FakeTranslationFeature(
            invocationPolicy = TranslationInvocationPolicy.Immediate,
            executionOverride = { ready(it) },
        )
        val controller = TranslationSessionController(feature, backgroundScope, selectionSettleDelayMillis = 0)

        controller.submit(input("hello"))
        runCurrent()

        feature.translatedTexts shouldContainExactly listOf("hello")
        controller.state.value.shouldBeInstanceOf<TranslationSessionState.Ready>()
    }

    @Test
    fun `dismissal cancels work and removes session text from state`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val feature = FakeTranslationFeature(
            prepareOverride = { request ->
                gate.await()
                ready(request)
            },
        )
        val controller = TranslationSessionController(feature, backgroundScope, selectionSettleDelayMillis = 0)

        controller.submit(input("private selection"))
        runCurrent()
        controller.dismiss()
        gate.complete(Unit)
        runCurrent()

        controller.state.value shouldBe TranslationSessionState.Hidden
    }

    private class FakeTranslationFeature(
        private val invocationPolicy: TranslationInvocationPolicy =
            TranslationInvocationPolicy.ExplicitAction("Translate"),
        private val prepareOverride: (suspend (TranslationRequest) -> TranslationPreparation)? = null,
        private val executionOverride: ((TranslationRequest) -> TranslationPreparation)? = null,
    ) : TranslationFeature {
        val preparedTexts = mutableListOf<String>()
        val translatedTexts = mutableListOf<String>()

        override suspend fun prepare(request: TranslationRequest): TranslationPreparation {
            preparedTexts += request.text
            return prepareOverride?.invoke(request) ?: ready(request, invocationPolicy)
        }

        override suspend fun translate(ready: ReadyTranslation): TranslationExecution {
            val fake = ready as FakeReadyTranslation
            translatedTexts += fake.request.text
            executionOverride?.let {
                return TranslationExecution.PreparationChanged(it(fake.request))
            }
            return TranslationExecution.Success(
                TranslationResult(
                    translatedText = "translated ${fake.request.text}",
                    sourceLanguage = SOURCE,
                    targetLanguage = TARGET,
                    presentation = fake.preparation.presentation,
                ),
            )
        }
    }

    private companion object {
        val SOURCE = TranslationLanguageTag.require("en")
        val TARGET = TranslationLanguageTag.require("pl")
        val ENGINE = TranslationEngineId("fake")
        val PROVIDER = TranslationProviderId("fake")

        fun input(text: String): TranslationSessionInput {
            return TranslationSessionInput(
                request = TranslationRequest(
                    text = text,
                    sourceLanguage = TranslationSourceLanguageSelection.Explicit(SOURCE),
                    targetLanguage = TranslationTargetLanguageSelection.Explicit(TARGET),
                ),
            )
        }

        fun ready(
            request: TranslationRequest,
            invocationPolicy: TranslationInvocationPolicy =
                TranslationInvocationPolicy.ExplicitAction("Translate"),
        ): TranslationPreparation.Ready {
            val preparation = TranslationPreparation.Ready(
                translation = PendingReadyTranslation(),
                request = ResolvedTranslationRequest(
                    text = request.text,
                    sourceLanguage = SOURCE,
                    targetLanguage = TARGET,
                    engine = ENGINE,
                ),
                presentation = TranslationProviderPresentation(
                    providerId = PROVIDER,
                    providerName = "Fake",
                    engineName = "Fake engine",
                    invocationPolicy = invocationPolicy,
                ),
            )
            return preparation.copy(
                translation = FakeReadyTranslation(request, preparation),
            )
        }
    }

    private class PendingReadyTranslation : ReadyTranslation

    private data class FakeReadyTranslation(
        val request: TranslationRequest,
        val preparation: TranslationPreparation.Ready,
    ) : ReadyTranslation
}
