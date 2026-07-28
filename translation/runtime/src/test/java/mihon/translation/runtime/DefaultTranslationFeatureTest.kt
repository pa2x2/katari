package mihon.translation.runtime

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import mihon.translation.api.KnownTranslationEngine
import mihon.translation.api.ReadyTranslation
import mihon.translation.api.ResolvedTranslationRequest
import mihon.translation.api.TranslationEngineBuildAvailability
import mihon.translation.api.TranslationEngineChoiceReason
import mihon.translation.api.TranslationEngineId
import mihon.translation.api.TranslationEngineSelection
import mihon.translation.api.TranslationExecution
import mihon.translation.api.TranslationFailureReason
import mihon.translation.api.TranslationInvocationPolicy
import mihon.translation.api.TranslationLanguageTag
import mihon.translation.api.TranslationPreparation
import mihon.translation.api.TranslationProviderId
import mihon.translation.api.TranslationProviderPresentation
import mihon.translation.api.TranslationRejectionReason
import mihon.translation.api.TranslationRequest
import mihon.translation.api.TranslationResult
import mihon.translation.api.TranslationSourceLanguageSelection
import mihon.translation.api.TranslationTargetLanguageSelection
import mihon.translation.spi.ReadyTranslationEngineRequest
import mihon.translation.spi.TranslationEngine
import mihon.translation.spi.TranslationEngineExecution
import mihon.translation.spi.TranslationEnginePreparation
import org.junit.jupiter.api.Test

class DefaultTranslationFeatureTest {
    @Test
    fun `fake engine drives preparation and successful execution without provider implementation types`() = runTest {
        val engine = FakeTranslationEngine(
            execution = TranslationEngineExecution.Success("Hola"),
        )
        val feature = feature(engine)

        val preparation = feature.prepare(explicitRequest()) as TranslationPreparation.Ready
        val execution = feature.translate(preparation.translation)

        engine.preparedRequest shouldBe ResolvedTranslationRequest(
            text = "Hello",
            sourceLanguage = ENGLISH,
            targetLanguage = SPANISH,
            engine = ENGINE_ID,
        )
        execution shouldBe TranslationExecution.Success(
            TranslationResult(
                translatedText = "Hola",
                sourceLanguage = ENGLISH,
                targetLanguage = SPANISH,
                presentation = PRESENTATION,
            ),
        )
    }

    @Test
    fun `provider preparation changes are returned as typed API states`() = runTest {
        val engine = FakeTranslationEngine(
            preparation = TranslationEnginePreparation.SystemSetupRequired(
                mihon.translation.api.TranslationSystemSetupReason.ServiceDisabled,
            ),
        )

        feature(engine).prepare(explicitRequest()) shouldBe TranslationPreparation.SystemSetupRequired(
            engine = ENGINE_ID,
            presentation = PRESENTATION,
            reason = mihon.translation.api.TranslationSystemSetupReason.ServiceDisabled,
        )
    }

    @Test
    fun `ready handles are process local and revalidate engine registration`() = runTest {
        val engine = FakeTranslationEngine()
        val registry = DefaultTranslationEngineRegistry(listOf(engine))
        val feature = feature(registry)
        val ready = (feature.prepare(explicitRequest()) as TranslationPreparation.Ready).translation

        feature(emptyRegistry()).translate(ready) shouldBe TranslationExecution.PreparationChanged(
            TranslationPreparation.EngineChoiceRequired(
                reason = TranslationEngineChoiceReason.SelectedEngineUnavailable(ENGINE_ID),
                engines = emptyList(),
            ),
        )
        feature.translate(object : ReadyTranslation {}) shouldBe TranslationExecution.Failed(
            TranslationFailureReason.InvalidReadyTranslation,
        )
    }

    @Test
    fun `input limits count Unicode code points and never truncate`() = runTest {
        val engine = FakeTranslationEngine(maximumInputCodePoints = 2)
        val request = explicitRequest(text = "😀😀😀")

        feature(engine).prepare(request) shouldBe TranslationPreparation.Rejected(
            TranslationRejectionReason.InputTooLarge(
                actualCodePoints = 3,
                maximumCodePoints = 2,
            ),
        )
        engine.preparedRequest shouldBe null
    }

    @Test
    fun `coroutine cancellation is not converted into a provider failure`() = runTest {
        val engine = FakeTranslationEngine(
            executionBlock = { awaitCancellation() },
        )
        val feature = feature(engine)
        val ready = (feature.prepare(explicitRequest()) as TranslationPreparation.Ready).translation

        val execution = async { feature.translate(ready) }
        execution.cancel()

        shouldThrow<CancellationException> { execution.await() }
    }

    private fun feature(engine: TranslationEngine): DefaultTranslationFeature {
        return feature(DefaultTranslationEngineRegistry(listOf(engine)))
    }

    private fun feature(registry: DefaultTranslationEngineRegistry): DefaultTranslationFeature {
        return DefaultTranslationFeature(
            engineRegistry = registry,
            knownEngineCatalog = registry,
            sourceLanguageDetectors = emptyList(),
            defaultTargetLanguageResolver = TranslationDefaultTargetLanguageResolver { null },
        )
    }

    private fun emptyRegistry() = DefaultTranslationEngineRegistry(emptyList())

    private fun explicitRequest(text: String = "Hello") = TranslationRequest(
        text = text,
        sourceLanguage = TranslationSourceLanguageSelection.Explicit(ENGLISH),
        targetLanguage = TranslationTargetLanguageSelection.Explicit(SPANISH),
        engine = TranslationEngineSelection.Explicit(ENGINE_ID),
    )

    private class FakeTranslationEngine(
        private val preparation: TranslationEnginePreparation = TranslationEnginePreparation.Ready(FakeReady),
        private val execution: TranslationEngineExecution = TranslationEngineExecution.Success("translated"),
        private val executionBlock: (suspend () -> TranslationEngineExecution)? = null,
        override val maximumInputCodePoints: Int? = null,
    ) : TranslationEngine {
        override val catalogEntry = KNOWN_ENGINE
        override val presentation = PRESENTATION

        var preparedRequest: ResolvedTranslationRequest? = null

        override suspend fun prepare(request: ResolvedTranslationRequest): TranslationEnginePreparation {
            preparedRequest = request
            return preparation
        }

        override suspend fun translate(ready: ReadyTranslationEngineRequest): TranslationEngineExecution {
            return executionBlock?.invoke() ?: execution
        }
    }

    private data object FakeReady : ReadyTranslationEngineRequest

    private companion object {
        val ENGINE_ID = TranslationEngineId("fake")
        val PROVIDER_ID = TranslationProviderId("fake")
        val ENGLISH = TranslationLanguageTag.require("en")
        val SPANISH = TranslationLanguageTag.require("es")
        val PRESENTATION = TranslationProviderPresentation(
            providerId = PROVIDER_ID,
            providerName = "Fake provider",
            engineName = "Fake engine",
            invocationPolicy = TranslationInvocationPolicy.Immediate,
        )
        val KNOWN_ENGINE = KnownTranslationEngine(
            id = ENGINE_ID,
            providerId = PROVIDER_ID,
            providerName = "Fake provider",
            engineName = "Fake engine",
            buildAvailability = TranslationEngineBuildAvailability.Included,
        )
    }
}
