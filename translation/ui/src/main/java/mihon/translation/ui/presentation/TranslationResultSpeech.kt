package mihon.translation.ui.presentation

import mihon.language.api.tag.LanguageTag
import mihon.translation.ui.session.TranslationSessionResult

enum class TranslationResultSpeechSide {
    Source,
    Target,
}

enum class TranslationResultSpeechPhase {
    Preparing,
    Speaking,
}

data class TranslationResultSpeechTarget(
    val side: TranslationResultSpeechSide,
    val text: String,
    val language: LanguageTag,
) {
    init {
        require(text.isNotBlank())
    }
}

data class TranslationResultSpeechState(
    val activeTarget: TranslationResultSpeechTarget? = null,
    val phase: TranslationResultSpeechPhase? = null,
) {
    init {
        require((activeTarget == null) == (phase == null))
    }
}

/** The original and the translated text of this result, as the popup's speech buttons offer them. */
fun TranslationSessionResult.speechTargets(): Set<TranslationResultSpeechTarget> = setOf(
    TranslationResultSpeechTarget(
        side = TranslationResultSpeechSide.Source,
        text = input.request.text,
        language = result.sourceLanguage,
    ),
    TranslationResultSpeechTarget(
        side = TranslationResultSpeechSide.Target,
        text = result.translatedText,
        language = result.targetLanguage,
    ),
)
