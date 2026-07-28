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
import mihon.translation.spi.TranslationAutomaticSelectionPriority
import mihon.translation.spi.TranslationEngine
import mihon.translation.spi.TranslationEngineExecution
import mihon.translation.spi.TranslationEnginePreparation
import mihon.translation.spi.TranslationSourceLanguageDetection
import mihon.translation.spi.TranslationSourceLanguageDetector
import mihon.translation.spi.TranslationSourceLanguageDetectorId
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

    @Test
    fun `automatic resolution prefers ready priority and ready always beats setup`() = runTest {
        val system = FakeTranslationEngine(
            catalogEntry = knownEngine("android-system"),
            automaticSelectionPriority = TranslationAutomaticSelectionPriority(ready = 20, setup = 0),
        )
        val mlKit = FakeTranslationEngine(
            catalogEntry = knownEngine("mlkit"),
            automaticSelectionPriority = TranslationAutomaticSelectionPriority(ready = 10, setup = 20),
        )

        val bothReady = feature(DefaultTranslationEngineRegistry(listOf(system, mlKit)))
            .prepare(automaticEngineRequest()) as TranslationPreparation.Ready

        bothReady.request.engine shouldBe system.catalogEntry.id

        val systemSetup = FakeTranslationEngine(
            catalogEntry = knownEngine("android-system"),
            preparation = TranslationEnginePreparation.SystemSetupRequired(
                mihon.translation.api.TranslationSystemSetupReason.ServiceDisabled,
            ),
            automaticSelectionPriority = TranslationAutomaticSelectionPriority(ready = 20, setup = 100),
        )
        val mlKitReady = FakeTranslationEngine(
            catalogEntry = knownEngine("mlkit"),
            automaticSelectionPriority = TranslationAutomaticSelectionPriority(ready = 0, setup = 0),
        )

        val readyBeatsSetup = feature(DefaultTranslationEngineRegistry(listOf(systemSetup, mlKitReady)))
            .prepare(automaticEngineRequest()) as TranslationPreparation.Ready

        readyBeatsSetup.request.engine shouldBe mlKitReady.catalogEntry.id
    }

    @Test
    fun `automatic resolution prefers ML Kit setup priority when no engine is ready`() = runTest {
        val system = FakeTranslationEngine(
            catalogEntry = knownEngine("android-system"),
            preparation = TranslationEnginePreparation.SystemSetupRequired(
                mihon.translation.api.TranslationSystemSetupReason.ServiceDisabled,
            ),
            automaticSelectionPriority = TranslationAutomaticSelectionPriority(ready = 20, setup = 10),
        )
        val mlKit = FakeTranslationEngine(
            catalogEntry = knownEngine("mlkit"),
            preparation = TranslationEnginePreparation.SystemSetupRequired(
                mihon.translation.api.TranslationSystemSetupReason.LanguageModelsRequired,
            ),
            automaticSelectionPriority = TranslationAutomaticSelectionPriority(ready = 10, setup = 20),
        )

        feature(DefaultTranslationEngineRegistry(listOf(system, mlKit)))
            .prepare(automaticEngineRequest()) shouldBe TranslationPreparation.SystemSetupRequired(
            engine = mlKit.catalogEntry.id,
            presentation = mlKit.presentation,
            reason = mihon.translation.api.TranslationSystemSetupReason.LanguageModelsRequired,
        )
    }

    @Test
    fun `saved explicit engine remains selected when absent and request override wins when present`() = runTest {
        val available = FakeTranslationEngine(catalogEntry = knownEngine("available"))
        val missing = TranslationEngineId("missing")
        val registry = DefaultTranslationEngineRegistry(
            engines = listOf(available),
            knownEngines = listOf(available.catalogEntry, knownEngine(missing.value)),
        )
        val feature = feature(
            registry = registry,
            preferredEngineSelection = { TranslationEngineSelection.Explicit(missing) },
        )

        feature.prepare(automaticEngineRequest()) shouldBe TranslationPreparation.EngineChoiceRequired(
            reason = TranslationEngineChoiceReason.SelectedEngineUnavailable(missing),
            engines = registry.knownEngines,
        )
        val overridden = feature.prepare(
            explicitRequest().copy(engine = TranslationEngineSelection.Explicit(available.catalogEntry.id)),
        ) as TranslationPreparation.Ready
        overridden.request.engine shouldBe available.catalogEntry.id
    }

    @Test
    fun `explicit engine preparation never falls back`() = runTest {
        val selected = FakeTranslationEngine(
            catalogEntry = knownEngine("selected"),
            preparation = TranslationEnginePreparation.Unavailable(
                mihon.translation.api.TranslationUnavailableReason.ServiceMissing,
            ),
        )
        val fallback = FakeTranslationEngine(catalogEntry = knownEngine("fallback"))
        val feature = feature(DefaultTranslationEngineRegistry(listOf(selected, fallback)))
        val request = explicitRequest().copy(
            engine = TranslationEngineSelection.Explicit(selected.catalogEntry.id),
        )

        feature.prepare(request) shouldBe TranslationPreparation.Unavailable(
            mihon.translation.api.TranslationUnavailableReason.ServiceMissing,
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
            reason = mihon.translation.api.TranslationTargetChoiceReason.SourceEqualsTarget,
        )
    }

    @Test
    fun `execution revalidates preparation and never runs a stale provider handle`() = runTest {
        val engine = FakeTranslationEngine(
            revalidation = TranslationEnginePreparation.SystemSetupRequired(
                mihon.translation.api.TranslationSystemSetupReason.ServiceDisabled,
            ),
        )
        val feature = feature(engine)
        val ready = (feature.prepare(explicitRequest()) as TranslationPreparation.Ready).translation

        feature.translate(ready) shouldBe TranslationExecution.PreparationChanged(
            TranslationPreparation.SystemSetupRequired(
                engine = ENGINE_ID,
                presentation = PRESENTATION,
                reason = mihon.translation.api.TranslationSystemSetupReason.ServiceDisabled,
            ),
        )
        engine.translationCount shouldBe 0
    }

    @Test
    fun `provider failure never retries another ready engine`() = runTest {
        val selected = FakeTranslationEngine(
            catalogEntry = knownEngine("selected"),
            execution = TranslationEngineExecution.Failed("provider failed"),
            automaticSelectionPriority = TranslationAutomaticSelectionPriority(ready = 20),
        )
        val fallback = FakeTranslationEngine(
            catalogEntry = knownEngine("fallback"),
            automaticSelectionPriority = TranslationAutomaticSelectionPriority(ready = 10),
        )
        val feature = feature(DefaultTranslationEngineRegistry(listOf(selected, fallback)))
        val ready = (feature.prepare(automaticEngineRequest()) as TranslationPreparation.Ready).translation

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
            registry = DefaultTranslationEngineRegistry(listOf(FakeTranslationEngine())),
            sourceLanguageDetectors = listOf(
                FakeDetector("unavailable", TranslationSourceLanguageDetection.Unavailable("not available")),
                FakeDetector("undetermined", TranslationSourceLanguageDetection.Undetermined),
            ),
        )
        val request = explicitRequest().copy(sourceLanguage = TranslationSourceLanguageSelection.Automatic)

        feature.prepare(request) shouldBe TranslationPreparation.SourceUndetermined()
    }

    private fun feature(engine: TranslationEngine): DefaultTranslationFeature {
        return feature(DefaultTranslationEngineRegistry(listOf(engine)))
    }

    private fun feature(
        registry: DefaultTranslationEngineRegistry,
        preferredEngineSelection: () -> TranslationEngineSelection = {
            TranslationEngineSelection.Automatic
        },
        sourceLanguageDetectors: List<TranslationSourceLanguageDetector> = emptyList(),
    ): DefaultTranslationFeature {
        return DefaultTranslationFeature(
            engineRegistry = registry,
            knownEngineCatalog = registry,
            sourceLanguageDetectors = sourceLanguageDetectors,
            defaultTargetLanguageResolver = TranslationDefaultTargetLanguageResolver { null },
            preferredEngineSelection = preferredEngineSelection,
        )
    }

    private fun emptyRegistry() = DefaultTranslationEngineRegistry(emptyList())

    private fun explicitRequest(text: String = "Hello") = TranslationRequest(
        text = text,
        sourceLanguage = TranslationSourceLanguageSelection.Explicit(ENGLISH),
        targetLanguage = TranslationTargetLanguageSelection.Explicit(SPANISH),
        engine = TranslationEngineSelection.Explicit(ENGINE_ID),
    )

    private fun automaticEngineRequest(text: String = "Hello") = explicitRequest(text).copy(
        engine = TranslationEngineSelection.Automatic,
    )

    private class FakeTranslationEngine(
        private val preparation: TranslationEnginePreparation = TranslationEnginePreparation.Ready(FakeReady),
        private val revalidation: TranslationEnginePreparation? = null,
        private val execution: TranslationEngineExecution = TranslationEngineExecution.Success("translated"),
        private val executionBlock: (suspend () -> TranslationEngineExecution)? = null,
        override val maximumInputCodePoints: Int? = null,
        override val catalogEntry: KnownTranslationEngine = KNOWN_ENGINE,
        override val automaticSelectionPriority: TranslationAutomaticSelectionPriority =
            TranslationAutomaticSelectionPriority(),
    ) : TranslationEngine {
        override val presentation = presentation(catalogEntry)

        var preparedRequest: ResolvedTranslationRequest? = null
        var preparationCount = 0
        var translationCount = 0

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
        private val result: TranslationSourceLanguageDetection,
    ) : TranslationSourceLanguageDetector {
        override val id = TranslationSourceLanguageDetectorId("fake-detector-$id")

        override suspend fun detect(text: String): TranslationSourceLanguageDetection = result
    }

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

        fun knownEngine(id: String) = KnownTranslationEngine(
            id = TranslationEngineId(id),
            providerId = TranslationProviderId(id),
            providerName = id,
            engineName = id,
            buildAvailability = TranslationEngineBuildAvailability.Included,
        )

        fun presentation(engine: KnownTranslationEngine) = TranslationProviderPresentation(
            providerId = engine.providerId,
            providerName = engine.providerName,
            engineName = engine.engineName,
            invocationPolicy = TranslationInvocationPolicy.Immediate,
        )
    }
}
