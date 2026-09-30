package mihon.translation.runtime

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
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
import mihon.translation.api.preparation.TranslationPreparation
import mihon.translation.api.preparation.TranslationRejectionReason
import mihon.translation.api.preparation.TranslationSystemSetupReason
import mihon.translation.api.preparation.TranslationUnavailableReason
import mihon.translation.api.provider.TranslationInvocationPolicy
import mihon.translation.api.provider.TranslationProviderPresentation
import mihon.translation.api.request.ResolvedTranslationRoute
import mihon.translation.api.request.TranslationBatch
import mihon.translation.api.request.TranslationContext
import mihon.translation.api.request.TranslationRequest
import mihon.translation.api.request.TranslationSourceLanguageSelection
import mihon.translation.api.request.TranslationTargetLanguageSelection
import mihon.translation.api.request.TranslationWorkContext
import mihon.translation.api.result.TranslationBatchUpdate
import mihon.translation.api.result.TranslationExecution
import mihon.translation.api.result.TranslationFailureReason
import mihon.translation.runtime.cache.TranslationResultCache
import mihon.translation.runtime.feature.DefaultTranslationFeature
import mihon.translation.runtime.feature.TranslationDefaultTargetLanguageResolver
import mihon.translation.runtime.registry.DefaultTranslationEngineRegistry
import mihon.translation.spi.contribution.TranslationEngineContribution
import mihon.translation.spi.engine.ContextualTranslationEngine
import mihon.translation.spi.engine.ReadyTranslationEngineRequest
import mihon.translation.spi.engine.TranslationContextElement
import mihon.translation.spi.engine.TranslationEngine
import mihon.translation.spi.engine.TranslationEngineBatchExecution
import mihon.translation.spi.engine.TranslationEngineDeviceAvailability
import mihon.translation.spi.engine.TranslationEngineExecution
import mihon.translation.spi.engine.TranslationEnginePreparation
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class DefaultTranslationFeatureTest {
    @TempDir
    lateinit var cacheDirectory: File

    @Test
    fun `a repeated translation is answered from the cache but a failed one is translated again`() = runTest {
        val engine = FakeTranslationEngine()
        val registry = DefaultTranslationEngineRegistry(listOf(TranslationEngineContribution(engine)))
        val cache = TranslationResultCache({ cacheDirectory }, maximumBytes = 1_000_000)
        val first = feature(registry, resultCache = cache)
        val firstResult = first.translate(
            (first.prepare(explicitRequest()) as TranslationPreparation.Ready).translation,
        )

        val second = feature(registry, resultCache = TranslationResultCache({ cacheDirectory }, 1_000_000))
        val secondResult = second.translate(
            (second.prepare(explicitRequest()) as TranslationPreparation.Ready).translation,
        )

        secondResult shouldBe firstResult
        engine.translationCount shouldBe 1

        val failing = FakeTranslationEngine(execution = TranslationEngineExecution.Failed("offline"))
        val failingFeature = feature(
            DefaultTranslationEngineRegistry(listOf(TranslationEngineContribution(failing))),
            resultCache = TranslationResultCache({ cacheDirectory }, 1_000_000),
        )
        repeat(2) {
            failingFeature.translate(
                (failingFeature.prepare(explicitRequest(text = "Goodbye")) as TranslationPreparation.Ready).translation,
            )
        }
        failing.translationCount shouldBe 2
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
        engine.preparedRoute shouldBe null
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
    fun `an unavailable or failing selected engine is never replaced by another`() = runTest {
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

        val failing = FakeTranslationEngine(
            catalogEntry = knownEngine("failing"),
            execution = TranslationEngineExecution.Failed("provider failed"),
        )
        val failingFeature = feature(
            DefaultTranslationEngineRegistry(
                listOf(TranslationEngineContribution(failing), TranslationEngineContribution(fallback)),
            ),
        )
        val ready = (
            failingFeature.prepare(
                explicitRequest().copy(engine = TranslationEngineSelection.Explicit(failing.catalogEntry.id)),
            ) as TranslationPreparation.Ready
            ).translation

        failingFeature.translate(ready) shouldBe TranslationExecution.Failed(
            TranslationFailureReason.ProviderFailure(engine = failing.catalogEntry.id, message = "provider failed"),
        )
        fallback.translationCount shouldBe 0
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
    fun `a translation is reused in another context only when the engine read nothing that differs`() = runTest {
        val plain = FakeTranslationEngine()
        val contextual = FakeContextualEngine()
        val feature = feature(
            DefaultTranslationEngineRegistry(
                listOf(TranslationEngineContribution(plain), TranslationEngineContribution(contextual)),
            ),
            resultCache = TranslationResultCache({ cacheDirectory }, maximumBytes = 1_000_000),
        )
        val morning = TranslationContext(TranslationWorkContext("First work"), precedingText = listOf("Morning"))
        val otherWork = morning.copy(work = TranslationWorkContext("Second work"))
        val evening = morning.copy(precedingText = listOf("Evening"))

        listOf(morning, evening).forEach { context ->
            feature.translateBatch(TranslationBatch(route(plain), listOf("Hello"), context)).toList() shouldBe
                listOf(TranslationBatchUpdate.Translated(0, "translated"))
        }
        plain.translationCount shouldBe 1

        listOf(morning, otherWork, evening).forEach { context ->
            feature.translateBatch(TranslationBatch(route(contextual), listOf("Hello", "World"), context)).toList()
        }
        // The engine reads preceding text but not the work, so only the evening differs for it.
        contextual.contexts shouldBe listOf(
            TranslationContext(precedingText = listOf("Morning")),
            TranslationContext(precedingText = listOf("Evening")),
        )
    }

    @Test
    fun `translations that do not answer every text are dropped instead of landing on the wrong texts`() = runTest {
        val contextual = FakeContextualEngine(answer = { segments -> segments.drop(1) })
        val feature = feature(
            DefaultTranslationEngineRegistry(listOf(TranslationEngineContribution(contextual))),
            resultCache = TranslationResultCache({ cacheDirectory }, maximumBytes = 1_000_000),
        )
        val batch = TranslationBatch(route(contextual), listOf("Hello", "World"))

        repeat(2) {
            feature.translateBatch(batch).toList() shouldBe
                listOf(TranslationBatchUpdate.Failed(0), TranslationBatchUpdate.Failed(1))
        }
        contextual.contexts.size shouldBe 2
    }

    @Test
    fun `a call the engine fails ends the batch with its reason instead of passing for untranslated texts`() = runTest {
        val contextual = FakeContextualEngine(failure = "Quota is used up")
        val feature = feature(DefaultTranslationEngineRegistry(listOf(TranslationEngineContribution(contextual))))

        feature.translateBatch(TranslationBatch(route(contextual), listOf("Hello", "World"))).toList() shouldBe
            listOf(
                TranslationBatchUpdate.EngineFailed(
                    TranslationFailureReason.ProviderFailure(contextual.catalogEntry.id, "Quota is used up"),
                ),
            )
    }

    private fun route(engine: TranslationEngine) = ResolvedTranslationRoute(ENGLISH, SPANISH, engine.catalogEntry.id)

    private fun feature(engine: TranslationEngine): DefaultTranslationFeature {
        return feature(DefaultTranslationEngineRegistry(listOf(TranslationEngineContribution(engine))))
    }

    private fun feature(
        registry: DefaultTranslationEngineRegistry,
        resultCache: TranslationResultCache? = null,
    ): DefaultTranslationFeature {
        return DefaultTranslationFeature(
            engineRegistry = registry,
            knownEngineCatalog = registry,
            textLanguageDetectors = emptyList(),
            defaultTargetLanguageResolver = TranslationDefaultTargetLanguageResolver { null },
            selectedEngine = { ENGINE_ID },
            resultCache = resultCache,
            ioDispatcher = Dispatchers.Unconfined,
        )
    }

    private fun explicitRequest(text: String = "Hello") = TranslationRequest(
        text = text,
        sourceLanguage = TranslationSourceLanguageSelection.Explicit(ENGLISH),
        targetLanguage = TranslationTargetLanguageSelection.Explicit(SPANISH),
        engine = TranslationEngineSelection.Explicit(ENGINE_ID),
    )

    private class FakeTranslationEngine(
        private val preparation: TranslationEnginePreparation = TranslationEnginePreparation.Ready(FakeReady),
        private val revalidation: TranslationEnginePreparation? = null,
        private val execution: TranslationEngineExecution = TranslationEngineExecution.Success("translated"),
        private val executionBlock: (suspend () -> TranslationEngineExecution)? = null,
        override val maximumInputCodePoints: Int? = null,
        override val catalogEntry: KnownTranslationEngine = KNOWN_ENGINE,
    ) : TranslationEngine {
        override val presentation = presentation(catalogEntry)
        var preparedRoute: ResolvedTranslationRoute? = null
        var preparationCount = 0
        var translationCount = 0

        override suspend fun inspectDevice() = TranslationEngineDeviceAvailability.Available

        override suspend fun inspectLanguageSupport() =
            TranslationLanguageSupportInspection.Available(
                TranslationLanguageSupport.AnyLanguage,
            )

        override suspend fun prepare(route: ResolvedTranslationRoute): TranslationEnginePreparation {
            preparedRoute = route
            preparationCount += 1
            return preparation
        }

        override suspend fun revalidate(ready: ReadyTranslationEngineRequest): TranslationEnginePreparation {
            return revalidation ?: TranslationEnginePreparation.Ready(ready)
        }

        override suspend fun translate(ready: ReadyTranslationEngineRequest, text: String): TranslationEngineExecution {
            translationCount += 1
            return executionBlock?.invoke() ?: execution
        }
    }

    private class FakeContextualEngine(
        private val answer: (List<String>) -> List<String> = { segments -> segments.map { "translated $it" } },
        private val failure: String? = null,
    ) : ContextualTranslationEngine {
        override val catalogEntry = knownEngine("contextual")
        override val presentation = presentation(catalogEntry)
        override val maximumInputCodePoints: Int? = null
        override val contextSupport = setOf(TranslationContextElement.PrecedingText)
        override val maximumBatchSegments = 10
        val contexts = mutableListOf<TranslationContext>()

        override suspend fun inspectDevice() = TranslationEngineDeviceAvailability.Available

        override suspend fun inspectLanguageSupport() =
            TranslationLanguageSupportInspection.Available(TranslationLanguageSupport.AnyLanguage)

        override suspend fun prepare(route: ResolvedTranslationRoute): TranslationEnginePreparation =
            TranslationEnginePreparation.Ready(FakeReady)

        override suspend fun revalidate(ready: ReadyTranslationEngineRequest): TranslationEnginePreparation =
            TranslationEnginePreparation.Ready(ready)

        override suspend fun translate(
            ready: ReadyTranslationEngineRequest,
            segments: List<String>,
            context: TranslationContext,
        ): TranslationEngineBatchExecution {
            contexts += context
            failure?.let { return TranslationEngineBatchExecution.Failed(it) }
            return TranslationEngineBatchExecution.Success(answer(segments))
        }
    }

    private data object FakeReady : ReadyTranslationEngineRequest

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
