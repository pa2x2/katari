package mihon.translation.ui.session

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import mihon.language.api.tag.LanguageTag
import mihon.translation.api.availability.TranslationDeviceAvailability
import mihon.translation.api.engine.KnownTranslationEngine
import mihon.translation.api.engine.TranslationEngineId
import mihon.translation.api.engine.TranslationEngineInspection
import mihon.translation.api.host.TranslationHostActionResult
import mihon.translation.api.host.TranslationHostActions
import mihon.translation.api.language.TranslationLanguageSupport
import mihon.translation.api.language.TranslationLanguageSupportInspection
import mihon.translation.api.model.TranslationModelDescriptor
import mihon.translation.api.provider.TranslationProviderDisclosure
import mihon.translation.api.request.TranslationTargetLanguageSelection
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore.InMemoryPreference
import tachiyomi.core.common.preference.Preference

@OptIn(ExperimentalCoroutinesApi::class)
class TranslationLanguageSupportControllerTest {
    @Test
    fun `new engine selection discards the prior lookup and late result`() = runTest {
        val first = CompletableDeferred<TranslationLanguageSupportInspection>()
        val second = CompletableDeferred<TranslationLanguageSupportInspection>()
        val host = FakeHostActions { engine ->
            when (engine) {
                FIRST_ENGINE -> first.await()
                SECOND_ENGINE -> second.await()
                else -> error("Unexpected engine")
            }
        }
        val controller = TranslationLanguageSupportController(host, backgroundScope)

        controller.load(FIRST_ENGINE)
        runCurrent()
        controller.load(SECOND_ENGINE)
        runCurrent()
        second.complete(AVAILABLE)
        runCurrent()
        first.complete(
            TranslationLanguageSupportInspection.Available(
                TranslationLanguageSupport.ByRole(setOf(FRENCH), setOf(ENGLISH)),
            ),
        )
        runCurrent()

        controller.state.value shouldBe TranslationLanguageSupportState.Available(
            SECOND_ENGINE,
            SUPPORT,
        )
    }

    @Test
    fun `wedged capability lookup becomes retryable without retaining stale support`() = runTest {
        val host = FakeHostActions {
            awaitCancellation()
        }
        val controller = TranslationLanguageSupportController(
            hostActions = host,
            scope = backgroundScope,
            timeoutMillis = 1_000,
        )

        controller.load(FIRST_ENGINE)
        runCurrent()
        advanceTimeBy(1_000)
        runCurrent()

        controller.state.value shouldBe TranslationLanguageSupportState.Unavailable(
            FIRST_ENGINE,
            "Translation languages could not be loaded in time",
        )
    }

    private class FakeHostActions(
        private val inspect: suspend (TranslationEngineId) -> TranslationLanguageSupportInspection,
    ) : TranslationHostActions {
        override val knownEngines: List<KnownTranslationEngine> = emptyList()
        override val selectedEngine: Preference<TranslationEngineId> =
            InMemoryPreference("engine", null, FIRST_ENGINE)
        override val defaultTargetLanguage: Preference<TranslationTargetLanguageSelection> =
            InMemoryPreference("target", null, TranslationTargetLanguageSelection.Default)

        override suspend fun deviceAvailability() = TranslationDeviceAvailability.Available

        override suspend fun inspectEngines() = TranslationEngineInspection(emptyList(), null)

        override suspend fun inspectLanguageSupport(
            engine: TranslationEngineId,
        ): TranslationLanguageSupportInspection = inspect(engine)

        override suspend fun acknowledgeProviderDisclosure(
            engine: TranslationEngineId,
            disclosure: TranslationProviderDisclosure,
        ) = TranslationHostActionResult.Completed

        override suspend fun downloadModels(
            engine: TranslationEngineId,
            models: List<TranslationModelDescriptor>,
            allowMeteredNetwork: Boolean,
        ) = TranslationHostActionResult.Completed

        override fun supportsSetup(engine: TranslationEngineId) = false

        override suspend fun openSetup(engine: TranslationEngineId) =
            TranslationHostActionResult.SetupUnsupported

        override fun setSelectedEngine(engine: TranslationEngineId) = Unit

        override fun setDefaultTargetLanguage(language: LanguageTag?) = Unit
    }

    private companion object {
        val FIRST_ENGINE = TranslationEngineId("first")
        val SECOND_ENGINE = TranslationEngineId("second")
        val ENGLISH = LanguageTag.require("en")
        val FRENCH = LanguageTag.require("fr")
        val SUPPORT = TranslationLanguageSupport.ByRole(
            sourceLanguages = setOf(ENGLISH),
            targetLanguages = setOf(FRENCH),
        )
        val AVAILABLE = TranslationLanguageSupportInspection.Available(SUPPORT)
    }
}
