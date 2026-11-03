package mihon.language.runtime.identification

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import mihon.language.api.identification.TextLanguageDetection
import mihon.language.api.identification.TextLanguageDetector
import mihon.language.api.identification.TextLanguageDetectorId
import mihon.language.api.identification.TextLanguageResolutionContext
import mihon.language.api.tag.LanguageTag
import org.junit.jupiter.api.Test

class AutomaticTextLanguageResolverTest {
    @Test
    fun `short same-script selection defers to surrounding prose despite an accepted isolated guess`() = runTest {
        val detector = FixedDetector(
            mapOf(
                "valley" to detected(SOMALI, 0.7f),
                "Out there in a valley at the foot of a hill." to detected(ENGLISH, 0.95f),
            ),
        )
        val resolver = AutomaticTextLanguageResolver(listOf(detector))

        resolver.resolve(
            text = "valley",
            context = TextLanguageResolutionContext(
                surroundingText = "Out there in a valley at the foot of a hill.",
                declaredLanguages = listOf(ENGLISH),
            ),
        ) shouldBe AutomaticTextLanguageResolution.Resolved(ENGLISH)
    }

    @Test
    fun `short selection in a different script remains authoritative`() = runTest {
        val detector = FixedDetector(
            mapOf(
                "猫" to detected(JAPANESE, 0.85f),
                "The cat waited beside the door." to detected(ENGLISH, 0.95f),
            ),
        )
        val resolver = AutomaticTextLanguageResolver(listOf(detector))

        resolver.resolve(
            text = "猫",
            context = TextLanguageResolutionContext(
                surroundingText = "The cat waited beside the door.",
                sessionLanguage = ENGLISH,
            ),
        ) shouldBe AutomaticTextLanguageResolution.Resolved(JAPANESE)
    }

    @Test
    fun `learned session language resolves weak selections`() = runTest {
        val detector = FixedDetector(
            mapOf("Paris" to detected(FRENCH, 0.3f)),
        )
        val resolver = AutomaticTextLanguageResolver(listOf(detector))

        resolver.resolve(
            text = "Paris",
            context = TextLanguageResolutionContext(
                surroundingText = "The party reached Paris before nightfall.",
                sessionLanguage = ENGLISH,
            ),
        ) shouldBe AutomaticTextLanguageResolution.Resolved(ENGLISH)
    }

    private class FixedDetector(
        private val results: Map<String, TextLanguageDetection>,
    ) : TextLanguageDetector {
        override val id = TextLanguageDetectorId("recording")

        override suspend fun detect(text: String): TextLanguageDetection =
            results[text] ?: TextLanguageDetection.Undetermined
    }

    private companion object {
        val ENGLISH = LanguageTag.require("en")
        val FRENCH = LanguageTag.require("fr")
        val JAPANESE = LanguageTag.require("ja")
        val SOMALI = LanguageTag.require("so")

        fun detected(language: LanguageTag, confidence: Float) =
            TextLanguageDetection.Detected(language, confidence)
    }
}
