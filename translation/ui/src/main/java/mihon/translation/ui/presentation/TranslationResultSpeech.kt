package mihon.translation.ui.presentation

import mihon.language.api.tag.LanguageTag

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
