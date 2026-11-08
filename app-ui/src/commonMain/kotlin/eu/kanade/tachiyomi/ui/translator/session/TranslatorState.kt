package eu.kanade.tachiyomi.ui.translator.session

import app.katari.language.api.tag.LanguageTag
import app.katari.translation.api.engine.TranslationEngineId
import app.katari.translation.api.engine.TranslationEngineSelection
import app.katari.translation.api.engine.TranslationEngineState
import app.katari.translation.api.language.TranslationDefaultTarget
import app.katari.translation.api.request.TranslationSourceLanguageSelection
import app.katari.translation.api.request.TranslationTargetLanguageSelection
import app.katari.translation.ui.presentation.TranslationResultSpeechState
import app.katari.translation.ui.session.TranslationLanguageSupportState
import app.katari.translation.ui.session.TranslationSessionState

internal enum class TranslatorPicker {
    SourceLanguage,
    TargetLanguage,
    Engine,
}

internal data class TranslatorState(
    val text: String,
    val sourceLanguage: TranslationSourceLanguageSelection = TranslationSourceLanguageSelection.Automatic,
    val targetLanguage: TranslationTargetLanguageSelection = TranslationTargetLanguageSelection.Default,
    val defaultTarget: TranslationDefaultTarget? = null,
    val engine: TranslationEngineSelection = TranslationEngineSelection.ProfileDefault,
    val profileEngine: TranslationEngineId? = null,
    val engineSelectionResolved: Boolean = false,
    val engines: List<TranslationEngineState> = emptyList(),
    val languageSupport: TranslationLanguageSupportState = TranslationLanguageSupportState.Idle,
    val recentLanguages: List<LanguageTag> = emptyList(),
    val session: TranslationSessionState = TranslationSessionState.Hidden,
    val picker: TranslatorPicker? = null,
    val speech: TranslationResultSpeechState = TranslationResultSpeechState(),
) {
    val activeEngine: TranslationEngineId?
        get() = when (val selection = engine) {
            is TranslationEngineSelection.Explicit -> selection.engine
            TranslationEngineSelection.ProfileDefault -> profileEngine
        }

    val explicitSourceLanguage: LanguageTag?
        get() = (sourceLanguage as? TranslationSourceLanguageSelection.Explicit)?.language

    val explicitTargetLanguage: LanguageTag?
        get() = (targetLanguage as? TranslationTargetLanguageSelection.Explicit)?.language
}

internal sealed interface TranslatorEvent {
    /** [explanation] is the speech engine's own words for the user, when it gave any. */
    data class SpeechFailed(val explanation: String?) : TranslatorEvent

    /** Emitted when the current selections cannot be exchanged by the swap control. */
    data object SwapUnavailable : TranslatorEvent
}
