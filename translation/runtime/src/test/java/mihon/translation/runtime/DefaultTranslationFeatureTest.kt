package mihon.translation.runtime

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import mihon.language.api.identification.TextLanguageCandidate
import mihon.language.api.identification.TextLanguageDetection
import mihon.language.api.identification.TextLanguageDetector
import mihon.language.api.identification.TextLanguageDetectorId
import mihon.language.api.tag.LanguageTag
import mihon.translation.api.engine.KnownTranslationEngine
import mihon.translation.api.engine.TranslationEngineArtwork
import mihon.translation.api.engine.TranslationEngineBuildAvailability
import mihon.translation.api.engine.TranslationEngineDetails
import mihon.translation.api.engine.TranslationEngineId
import mihon.translation.api.engine.TranslationEngineSelection
import mihon.translation.api.engine.TranslationProviderId
import mihon.translation.api.language.TranslationLanguageSupport
import mihon.translation.api.language.TranslationLanguageSupportInspection
import mihon.translation.api.preparation.ReadyTranslation
import mihon.translation.api.preparation.TranslationEngineChoiceReason
import mihon.translation.api.preparation.TranslationPreparation
import mihon.translation.api.preparation.TranslationRejectionReason
import mihon.translation.api.preparation.TranslationSystemSetupReason
import mihon.translation.api.preparation.TranslationTargetChoiceReason
import mihon.translation.api.preparation.TranslationUnavailableReason
import mihon.translation.api.provider.TranslationInvocationPolicy
import mihon.translation.api.provider.TranslationProviderOutputMode
import mihon.translation.api.provider.TranslationProviderPresentation
import mihon.translation.api.request.ResolvedTranslationRequest
import mihon.translation.api.request.TranslationRequest
import mihon.translation.api.request.TranslationSourceLanguageSelection
import mihon.translation.api.request.TranslationTargetLanguageSelection
import mihon.translation.api.result.TranslationExecution
import mihon.translation.api.result.TranslationFailureReason
import mihon.translation.api.result.TranslationResult
import mihon.translation.runtime.feature.DefaultTranslationFeature
import mihon.translation.runtime.feature.TranslationDefaultTargetLanguageResolver
import mihon.translation.runtime.registry.DefaultTranslationEngineRegistry
import mihon.translation.spi.contribution.TranslationEngineContribution
import mihon.translation.spi.engine.ReadyTranslationEngineRequest
import mihon.translation.spi.engine.TranslationEngine
import mihon.translation.spi.engine.TranslationEngineDeviceAvailability
import mihon.translation.spi.engine.TranslationEngineExecution
import mihon.translation.spi.engine.TranslationEnginePreparation
import org.junit.jupiter.api.Test

class DefaultTranslationFeatureTest {
    @Test
    fun `provider surfaces are a typed execution outcome and must match declared output mode`() = runTest {
        val surfacePresentation = PRESENTATION.copy(
            outputMode = TranslationProviderOutputMode.ProviderSurface,
        )
        val engine = FakeTranslationEngine(
            execution = TranslationEngineExecution.ProviderSurfaceOpened,
            presentation = surfacePresentation,
        )
        val feature = feature(engine)
        val ready = (feature.prepare(explicitRequest()) as TranslationPreparation.Ready).translation

        feature.translate(ready) shouldBe TranslationExecution.ProviderSurfaceOpened(surfacePresentation)

        val invalidEngine = FakeTranslationEngine(
            execution = TranslationEngineExecution.ProviderSurfaceOpened,
        )
        val invalidFeature = feature(invalidEngine)
        val invalidReady =
            (invalidFeature.prepare(explicitRequest()) as TranslationPreparation.Ready).translation
        invalidFeature.translate(invalidReady) shouldBe TranslationExecution.Failed(
            TranslationFailureReason.ProviderFailure(
                engine = ENGINE_ID,
                message = "Translation provider returned an incompatible output mode",
            ),
        )
    }

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
                TranslationSystemSetupReason.ServiceDisabled,
            ),
        )

        feature(engine).prepare(explicitRequest()) shouldBe TranslationPreparation.SystemSetupRequired(
            engine = ENGINE_ID,
            presentation = PRESENTATION,
            reason = TranslationSystemSetupReason.ServiceDisabled,
        )
    }

    @Test
    fun `ready handles are process local and revalidate engine registration`() = runTest {
        val engine = FakeTranslationEngine()
        val registry =
            DefaultTranslationEngineRegistry(listOf(TranslationEngineContribution(engine)))
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

    @Test
    fun `profile engine remains selected when absent and request override wins when present`() = runTest {
        val available = FakeTranslationEngine(catalogEntry = knownEngine("available"))
        val missing = TranslationEngineId("missing")
        val registry = DefaultTranslationEngineRegistry(
            contributions = listOf(
                TranslationEngineContribution(available),
                TranslationEngineContribution(catalogEntry = knownEngine(missing.value)),
            ),
        )
        val feature = feature(
            registry = registry,
            selectedEngine = { missing },
        )

        feature.prepare(profileEngineRequest()) shouldBe TranslationPreparation.EngineChoiceRequired(
            reason = TranslationEngineChoiceReason.SelectedEngineUnavailable(missing),
            engines = registry.knownEngines,
        )
        val overridden = feature.prepare(
            explicitRequest().copy(engine = TranslationEngineSelection.Explicit(available.catalogEntry.id)),
        ) as TranslationPreparation.Ready
        overridden.request.engine shouldBe available.catalogEntry.id
    }

    @Test
    fun `profile request requires a choice when no engine is configured`() = runTest {
        val registry = DefaultTranslationEngineRegistry(
            listOf(TranslationEngineContribution(FakeTranslationEngine())),
        )
        val feature = feature(
            registry = registry,
            selectedEngine = { null },
        )

        feature.prepare(profileEngineRequest()) shouldBe TranslationPreparation.EngineChoiceRequired(
            reason = TranslationEngineChoiceReason.NoEngineConfigured,
            engines = registry.knownEngines,
        )
    }

    @Test
    fun `explicit engine preparation never falls back`() = runTest {
        val selected = FakeTranslationEngine(
            catalogEntry = knownEngine("selected"),
            preparation = TranslationEnginePreparation.Unavailable(
                TranslationUnavailableReason.ServiceMissing,
            ),
        )
        val fallback = FakeTranslationEngine(catalogEntry = knownEngine("fallback"))
        val feature = feature(
            DefaultTranslationEngineRegistry(
                listOf(
                    TranslationEngineContribution(selected),
                    TranslationEngineContribution(fallback),
                ),
            ),
        )
        val request = explicitRequest().copy(
            engine = TranslationEngineSelection.Explicit(selected.catalogEntry.id),
        )

        feature.prepare(request) shouldBe TranslationPreparation.Unavailable(
            TranslationUnavailableReason.ServiceMissing,
        )
        fallback.preparationCount shouldBe 0
    }

    @Test
    fun `equal source and target requires a per-request target choice`() = runTest {
        val request = explicitRequest().copy(
            targetLanguage = TranslationTargetLanguageSelection.Explicit(ENGLISH),
        )

        feature(FakeTranslationEngine()).prepare(request) shouldBe TranslationPreparation.TargetLanguageRequired(
            sourceLanguage = ENGLISH,
            reason = TranslationTargetChoiceReason.SourceEqualsTarget,
        )
    }

    @Test
    fun `execution revalidates preparation and never runs a stale provider handle`() = runTest {
        val engine = FakeTranslationEngine(
            revalidation = TranslationEnginePreparation.SystemSetupRequired(
                TranslationSystemSetupReason.ServiceDisabled,
            ),
        )
        val feature = feature(engine)
        val ready = (feature.prepare(explicitRequest()) as TranslationPreparation.Ready).translation

        feature.translate(ready) shouldBe TranslationExecution.PreparationChanged(
            TranslationPreparation.SystemSetupRequired(
                engine = ENGINE_ID,
                presentation = PRESENTATION,
                reason = TranslationSystemSetupReason.ServiceDisabled,
            ),
        )
        engine.translationCount shouldBe 0
    }

    @Test
    fun `provider failure never retries another ready engine`() = runTest {
        val selected = FakeTranslationEngine(
            catalogEntry = knownEngine("selected"),
            execution = TranslationEngineExecution.Failed("provider failed"),
        )
        val fallback = FakeTranslationEngine(
            catalogEntry = knownEngine("fallback"),
        )
        val feature = feature(
            DefaultTranslationEngineRegistry(
                listOf(
                    TranslationEngineContribution(selected),
                    TranslationEngineContribution(fallback),
                ),
            ),
        )
        val request = explicitRequest().copy(
            engine = TranslationEngineSelection.Explicit(selected.catalogEntry.id),
        )
        val ready = (feature.prepare(request) as TranslationPreparation.Ready).translation

        feature.translate(ready) shouldBe TranslationExecution.Failed(
            TranslationFailureReason.ProviderFailure(
                engine = selected.catalogEntry.id,
                message = "provider failed",
            ),
        )
        selected.translationCount shouldBe 1
        fallback.translationCount shouldBe 0
    }

    @Test
    fun `automatic source detection reports a chooser when every detector is inconclusive`() = runTest {
        val feature = feature(
            registry = DefaultTranslationEngineRegistry(
                listOf(TranslationEngineContribution(FakeTranslationEngine())),
            ),
            textLanguageDetectors = listOf(
                FakeDetector("unavailable", TextLanguageDetection.Unavailable("not available")),
                FakeDetector("undetermined", TextLanguageDetection.Undetermined),
            ),
        )
        val request = explicitRequest().copy(sourceLanguage = TranslationSourceLanguageSelection.Automatic)

        feature.prepare(request) shouldBe TranslationPreparation.SourceUndetermined()
    }

    @Test
    fun `automatic source ambiguity preserves ranked suggestions for correction`() = runTest {
        val feature = feature(
            registry = DefaultTranslationEngineRegistry(
                listOf(TranslationEngineContribution(FakeTranslationEngine())),
            ),
            textLanguageDetectors = listOf(
                FakeDetector(
                    "ambiguous",
                    TextLanguageDetection.Detected(
                        language = ENGLISH,
                        confidence = 0.4f,
                        alternatives = listOf(TextLanguageCandidate(SPANISH, 0.35f)),
                    ),
                ),
            ),
        )
        val request = explicitRequest().copy(sourceLanguage = TranslationSourceLanguageSelection.Automatic)

        feature.prepare(request) shouldBe TranslationPreparation.SourceUndetermined(
            suggestedLanguages = listOf(ENGLISH, SPANISH),
        )
    }

    private fun feature(engine: TranslationEngine): DefaultTranslationFeature {
        return feature(DefaultTranslationEngineRegistry(listOf(TranslationEngineContribution(engine))))
    }

    private fun feature(
        registry: DefaultTranslationEngineRegistry,
        selectedEngine: suspend () -> TranslationEngineId? = { ENGINE_ID },
        textLanguageDetectors: List<TextLanguageDetector> = emptyList(),
    ): DefaultTranslationFeature {
        return DefaultTranslationFeature(
            engineRegistry = registry,
            knownEngineCatalog = registry,
            textLanguageDetectors = textLanguageDetectors,
            defaultTargetLanguageResolver = TranslationDefaultTargetLanguageResolver { null },
            selectedEngine = selectedEngine,
        )
    }

    private fun emptyRegistry() =
        DefaultTranslationEngineRegistry(emptyList<TranslationEngineContribution>())

    private fun explicitRequest(text: String = "Hello") = TranslationRequest(
        text = text,
        sourceLanguage = TranslationSourceLanguageSelection.Explicit(ENGLISH),
        targetLanguage = TranslationTargetLanguageSelection.Explicit(SPANISH),
        engine = TranslationEngineSelection.Explicit(ENGINE_ID),
    )

    private fun profileEngineRequest(text: String = "Hello") = explicitRequest(text).copy(
        engine = TranslationEngineSelection.ProfileDefault,
    )

    private class FakeTranslationEngine(
        private val preparation: TranslationEnginePreparation = TranslationEnginePreparation.Ready(FakeReady),
        private val revalidation: TranslationEnginePreparation? = null,
        private val execution: TranslationEngineExecution = TranslationEngineExecution.Success("translated"),
        private val executionBlock: (suspend () -> TranslationEngineExecution)? = null,
        override val maximumInputCodePoints: Int? = null,
        override val catalogEntry: KnownTranslationEngine = KNOWN_ENGINE,
        override val presentation: TranslationProviderPresentation = presentation(catalogEntry),
    ) : TranslationEngine {
        var preparedRequest: ResolvedTranslationRequest? = null
        var preparationCount = 0
        var translationCount = 0

        override suspend fun inspectDevice() = TranslationEngineDeviceAvailability.Available

        override suspend fun inspectLanguageSupport() =
            TranslationLanguageSupportInspection.Available(
                TranslationLanguageSupport.AnyLanguage,
            )

        override suspend fun prepare(request: ResolvedTranslationRequest): TranslationEnginePreparation {
            preparedRequest = request
            preparationCount += 1
            return preparation
        }

        override suspend fun revalidate(ready: ReadyTranslationEngineRequest): TranslationEnginePreparation {
            return revalidation ?: TranslationEnginePreparation.Ready(ready)
        }

        override suspend fun translate(ready: ReadyTranslationEngineRequest): TranslationEngineExecution {
            translationCount += 1
            return executionBlock?.invoke() ?: execution
        }
    }

    private data object FakeReady : ReadyTranslationEngineRequest

    private class FakeDetector(
        id: String,
        private val result: TextLanguageDetection,
    ) : TextLanguageDetector {
        override val id = TextLanguageDetectorId("fake-detector-$id")

        override suspend fun detect(text: String): TextLanguageDetection = result
    }

    private companion object {
        val ENGINE_ID = TranslationEngineId("fake")
        val PROVIDER_ID = TranslationProviderId("fake")
        val ENGLISH = LanguageTag.require("en")
        val SPANISH = LanguageTag.require("es")
        val PRESENTATION = TranslationProviderPresentation(
            providerId = PROVIDER_ID,
            providerName = "Fake provider",
            engineName = "Fake engine",
            invocationPolicy = TranslationInvocationPolicy.Immediate,
        )
        val FAKE_ARTWORK = TranslationEngineArtwork.Bundled(1)
        val FAKE_DETAILS = TranslationEngineDetails(
            description = "Fake engine description",
            processingLocation = "Fake processing location",
            privacyDescription = "Fake privacy description",
        )
        val KNOWN_ENGINE = KnownTranslationEngine(
            id = ENGINE_ID,
            providerId = PROVIDER_ID,
            providerName = "Fake provider",
            engineName = "Fake engine",
            buildAvailability = TranslationEngineBuildAvailability.Included,
            artwork = FAKE_ARTWORK,
            details = FAKE_DETAILS,
        )

        fun knownEngine(id: String) = KnownTranslationEngine(
            id = TranslationEngineId(id),
            providerId = TranslationProviderId(id),
            providerName = id,
            engineName = id,
            buildAvailability = TranslationEngineBuildAvailability.Included,
            artwork = FAKE_ARTWORK,
            details = FAKE_DETAILS,
        )

        fun presentation(engine: KnownTranslationEngine) = TranslationProviderPresentation(
            providerId = engine.providerId,
            providerName = engine.providerName,
            engineName = engine.engineName,
            invocationPolicy = TranslationInvocationPolicy.Immediate,
        )
    }
}
