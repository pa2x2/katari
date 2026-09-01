package mihon.tts.runtime.feature

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import mihon.language.api.identification.TextLanguageCandidate
import mihon.language.api.identification.TextLanguageDetection
import mihon.language.api.identification.TextLanguageDetector
import mihon.language.api.identification.TextLanguageDetectorId
import mihon.language.api.tag.LanguageTag
import mihon.tts.api.engine.TtsEngineId
import mihon.tts.api.preparation.TtsLanguageChoiceReason
import mihon.tts.api.preparation.TtsPreparation
import mihon.tts.api.request.TtsRequest
import mihon.tts.runtime.audio.TtsAudioFocus
import mihon.tts.runtime.preference.ProfileTtsPreferences
import mihon.tts.spi.engine.KnownTtsEngineCatalog
import mihon.tts.spi.engine.TtsEngine
import mihon.tts.spi.engine.TtsEngineRegistry
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore

class DefaultTtsFeatureLanguageResolutionTest {
    @Test
    fun `ranked ambiguity reaches the language chooser`() = runBlocking<Unit> {
        val english = LanguageTag.require("en")
        val french = LanguageTag.require("fr")
        val feature = DefaultTtsFeature(
            engineRegistry = EmptyEngineRegistry,
            knownEngineCatalog = object : KnownTtsEngineCatalog {
                override val knownEngines = emptyList<mihon.tts.api.engine.KnownTtsEngine>()
            },
            textLanguageDetectors = listOf(
                FixedDetector(
                    TextLanguageDetection.Detected(
                        language = english,
                        confidence = 0.4f,
                        alternatives = listOf(TextLanguageCandidate(french, 0.35f)),
                    ),
                ),
            ),
            preferences = ProfileTtsPreferences(InMemoryPreferenceStore(), initialEngine = null),
            selectedEngine = { error("Language resolution must finish before engine selection") },
            scope = this,
            audioFocus = object : TtsAudioFocus {
                override fun request() = true
                override fun abandon() = Unit
            },
        )

        feature.prepare(TtsRequest("ambiguous")) shouldBe TtsPreparation.LanguageChoiceRequired(
            reason = TtsLanguageChoiceReason.Ambiguous,
            suggestedLanguages = listOf(english, french),
        )
    }

    private class FixedDetector(
        private val result: TextLanguageDetection,
    ) : TextLanguageDetector {
        override val id = TextLanguageDetectorId("fixed")
        override suspend fun detect(text: String) = result
    }

    private data object EmptyEngineRegistry : TtsEngineRegistry {
        override val engines: List<TtsEngine> = emptyList()
        override fun find(engine: TtsEngineId): TtsEngine? = null
    }
}
