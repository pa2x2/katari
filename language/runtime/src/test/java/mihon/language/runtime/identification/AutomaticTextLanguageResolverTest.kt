package mihon.language.runtime.identification

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import mihon.language.api.identification.TextLanguageCandidate
import mihon.language.api.identification.TextLanguageDetection
import mihon.language.api.identification.TextLanguageDetector
import mihon.language.api.identification.TextLanguageDetectorId
import mihon.language.api.identification.TextLanguageResolutionContext
import mihon.language.api.tag.LanguageTag
import org.junit.jupiter.api.Test

class AutomaticTextLanguageResolverTest {
    @Test
    fun `informative selected text overrides conflicting session context`() = runTest {
        val detector = RecordingDetector(
            mapOf(
                "bonjour tout le monde" to detected(FRENCH, 0.9f),
                "an English paragraph" to detected(ENGLISH, 0.95f),
            ),
        )
        val resolver = AutomaticTextLanguageResolver(listOf(detector))

        resolver.resolve(
            text = "bonjour tout le monde",
            context = TextLanguageResolutionContext(
                surroundingText = "an English paragraph",
                sessionLanguage = ENGLISH,
            ),
        ) shouldBe AutomaticTextLanguageResolution.Resolved(FRENCH)
        detector.inputs shouldBe listOf("bonjour tout le monde")
    }

    @Test
    fun `short same-script selection defers to surrounding prose despite an accepted isolated guess`() = runTest {
        val detector = RecordingDetector(
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
        val detector = RecordingDetector(
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
        detector.inputs shouldBe listOf("猫")
    }

    @Test
    fun `learned session language resolves weak selections without redetecting surrounding prose`() = runTest {
        val detector = RecordingDetector(
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
        detector.inputs shouldBe listOf("Paris")
    }

    @Test
    fun `strong local context replaces a conflicting learned session language`() = runTest {
        val detector = RecordingDetector(
            mapOf(
                "bonjour" to detected(FRENCH, 0.75f),
                "Nous avons dit bonjour à nos voisins." to detected(FRENCH, 0.95f),
            ),
        )
        val resolver = AutomaticTextLanguageResolver(listOf(detector))

        resolver.resolve(
            text = "bonjour",
            context = TextLanguageResolutionContext(
                surroundingText = "Nous avons dit bonjour à nos voisins.",
                sessionLanguage = ENGLISH,
            ),
        ) shouldBe AutomaticTextLanguageResolution.Resolved(FRENCH)
        detector.inputs shouldBe listOf("bonjour", "Nous avons dit bonjour à nos voisins.")
    }

    @Test
    fun `surrounding prose resolves an inconclusive short selection`() = runTest {
        val detector = RecordingDetector(
            mapOf(
                "Tower" to detected(ENGLISH, 0.3f),
                "La tour dominait toute la vallée." to detected(FRENCH, 0.85f),
            ),
        )
        val resolver = AutomaticTextLanguageResolver(listOf(detector))

        resolver.resolve(
            text = "Tower",
            context = TextLanguageResolutionContext(
                surroundingText = "La tour dominait toute la vallée.",
            ),
        ) shouldBe AutomaticTextLanguageResolution.Resolved(FRENCH)
    }

    @Test
    fun `declared language corroborates weak ranked evidence and ambiguity remains visible otherwise`() = runTest {
        val candidates = TextLanguageDetection.Detected(
            language = FRENCH,
            confidence = 0.4f,
            alternatives = listOf(TextLanguageCandidate(ENGLISH, 0.35f)),
        )
        val resolver = AutomaticTextLanguageResolver(
            listOf(RecordingDetector(mapOf("name" to candidates))),
        )

        resolver.resolve(
            text = "name",
            context = TextLanguageResolutionContext(declaredLanguages = listOf(ENGLISH)),
        ) shouldBe AutomaticTextLanguageResolution.Resolved(ENGLISH)
        resolver.resolve("name") shouldBe AutomaticTextLanguageResolution.Undetermined(
            suggestedLanguages = listOf(FRENCH, ENGLISH),
        )
    }

    private class RecordingDetector(
        private val results: Map<String, TextLanguageDetection>,
    ) : TextLanguageDetector {
        override val id = TextLanguageDetectorId("recording")
        val inputs = mutableListOf<String>()

        override suspend fun detect(text: String): TextLanguageDetection {
            inputs += text
            return results[text] ?: TextLanguageDetection.Undetermined
        }
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
