package eu.kanade.tachiyomi.ui.translator.session

import mihon.language.api.tag.LanguageTag
import mihon.translation.api.engine.TranslationEngineId
import mihon.translation.api.engine.TranslationEngineSelection
import mihon.translation.api.engine.TranslationEngineState
import mihon.translation.api.request.TranslationSourceLanguageSelection
import mihon.translation.api.request.TranslationTargetLanguageSelection
import mihon.translation.ui.presentation.TranslationResultSpeechState
import mihon.translation.ui.session.TranslationLanguageSupportState
import mihon.translation.ui.session.TranslationSessionState

internal enum class TranslatorPicker {
    SourceLanguage,
    TargetLanguage,
    Engine,
}

internal data class TranslatorState(
    val text: String,
    val sourceLanguage: TranslationSourceLanguageSelection = TranslationSourceLanguageSelection.Automatic,
    val targetLanguage: TranslationTargetLanguageSelection = TranslationTargetLanguageSelection.Default,
    val profileTargetLanguage: LanguageTag? = null,
    val engine: TranslationEngineSelection = TranslationEngineSelection.ProfileDefault,
    val profileEngine: TranslationEngineId? = null,
    val engineSelectionResolved: Boolean = false,
    val engines: List<TranslationEngineState> = emptyList(),
    val languageSupport: TranslationLanguageSupportState = TranslationLanguageSupportState.Idle,
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
    data object SpeechFailed : TranslatorEvent
}
