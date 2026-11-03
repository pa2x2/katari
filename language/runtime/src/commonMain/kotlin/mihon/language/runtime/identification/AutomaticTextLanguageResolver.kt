package mihon.language.runtime.identification

import mihon.language.api.identification.TextLanguageCandidate
import mihon.language.api.identification.TextLanguageDetection
import mihon.language.api.identification.TextLanguageDetector
import mihon.language.api.identification.TextLanguageResolutionContext
import mihon.language.api.tag.LanguageTag
import java.util.Locale

class AutomaticTextLanguageResolver(
    private val detectors: List<TextLanguageDetector>,
) {
    suspend fun resolve(
        text: String,
        context: TextLanguageResolutionContext = TextLanguageResolutionContext(),
    ): AutomaticTextLanguageResolution {
        val selectedEvidence = detect(text)
        val surroundingText = context.surroundingText?.takeUnless { it == text }
        val selectedLanguage = selectedEvidence.acceptedLanguage()
        val selectionNeedsContext = surroundingText != null && text.hasFewerThan(MINIMUM_STANDALONE_WORDS)
        if (!selectionNeedsContext || text.usesDifferentScriptFrom(surroundingText)) {
            selectedLanguage?.let {
                return AutomaticTextLanguageResolution.Resolved(it)
            }
        }

        val sessionLanguage = context.sessionLanguage
        if (sessionLanguage != null &&
            (selectedLanguage == null || selectedLanguage.sameBaseLanguageAs(sessionLanguage))
        ) {
            return AutomaticTextLanguageResolution.Resolved(sessionLanguage)
        }

        val surroundingEvidence = surroundingText?.let { detect(it) }.orEmpty()
        surroundingEvidence.acceptedLanguage()?.let {
            return AutomaticTextLanguageResolution.Resolved(it)
        }

        sessionLanguage?.let {
            return AutomaticTextLanguageResolution.Resolved(it)
        }

        if (selectionNeedsContext && context.declaredLanguages.isEmpty()) {
            selectedLanguage?.let {
                return AutomaticTextLanguageResolution.Resolved(it)
            }
        }

        val candidates = (selectedEvidence + surroundingEvidence).distinctBy(TextLanguageCandidate::language)
        context.declaredLanguages.firstNotNullOfOrNull { declared ->
            candidates.firstOrNull { candidate -> candidate.language.sameBaseLanguageAs(declared) }
                ?.let { declared }
        }?.let {
            return AutomaticTextLanguageResolution.Resolved(it)
        }

        if (candidates.isEmpty() && context.declaredLanguages.size == 1) {
            return AutomaticTextLanguageResolution.Resolved(context.declaredLanguages.single())
        }

        return AutomaticTextLanguageResolution.Undetermined(
            suggestedLanguages = (
                candidates.map(
                    TextLanguageCandidate::language,
                ) + context.declaredLanguages
                ).distinct(),
        )
    }

    private suspend fun detect(text: String): List<TextLanguageCandidate> {
        var firstEvidence: List<TextLanguageCandidate> = emptyList()
        detectors.forEach { detector ->
            when (val detection = detector.detect(text)) {
                is TextLanguageDetection.Detected -> {
                    val candidates = detection.candidates
                    if (firstEvidence.isEmpty()) firstEvidence = candidates
                    if (candidates.acceptedLanguage() != null) return candidates
                }
                TextLanguageDetection.Undetermined,
                is TextLanguageDetection.Unavailable,
                -> Unit
            }
        }
        return firstEvidence
    }

    private fun List<TextLanguageCandidate>.acceptedLanguage(): LanguageTag? {
        val first = firstOrNull() ?: return null
        val confidence = first.confidence
        return first.language.takeIf {
            confidence == null || confidence >= MINIMUM_CONFIDENCE
        }
    }

    private fun LanguageTag.sameBaseLanguageAs(other: LanguageTag): Boolean {
        return Locale.forLanguageTag(value).language == Locale.forLanguageTag(other.value).language
    }

    private fun String.hasFewerThan(minimumWords: Int): Boolean {
        return trim().split(Regex("\\s+")).count(String::isNotBlank) < minimumWords
    }

    private fun String.usesDifferentScriptFrom(other: String): Boolean {
        val selectedScript = dominantScript() ?: return false
        val surroundingScript = other.dominantScript() ?: return false
        return selectedScript != surroundingScript
    }

    private fun String.dominantScript(): Character.UnicodeScript? {
        return codePoints()
            .mapToObj(Character.UnicodeScript::of)
            .filter { script ->
                script != Character.UnicodeScript.COMMON &&
                    script != Character.UnicodeScript.INHERITED &&
                    script != Character.UnicodeScript.UNKNOWN
            }
            .toList()
            .groupingBy { it }
            .eachCount()
            .maxByOrNull(Map.Entry<Character.UnicodeScript, Int>::value)
            ?.key
    }

    private companion object {
        const val MINIMUM_CONFIDENCE = 0.5f
        const val MINIMUM_STANDALONE_WORDS = 3
    }
}

sealed interface AutomaticTextLanguageResolution {
    data class Resolved(
        val language: LanguageTag,
    ) : AutomaticTextLanguageResolution

    data class Undetermined(
        val suggestedLanguages: List<LanguageTag>,
    ) : AutomaticTextLanguageResolution
}
