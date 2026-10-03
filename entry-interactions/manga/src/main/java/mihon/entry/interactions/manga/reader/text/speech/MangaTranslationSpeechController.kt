package mihon.entry.interactions.manga.reader.text.speech

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import mihon.translation.ui.presentation.TranslationResultSpeechPhase
import mihon.translation.ui.presentation.TranslationResultSpeechState
import mihon.translation.ui.presentation.TranslationResultSpeechTarget
import mihon.translation.ui.presentation.speechTargets
import mihon.translation.ui.session.TranslationSessionState
import mihon.translation.ui.session.displayedSessionResult
import mihon.tts.api.TtsFeature
import mihon.tts.api.request.TtsLanguageSelection
import mihon.tts.ui.playback.ShortFormSpeechController
import mihon.tts.ui.playback.ShortFormSpeechFailure
import mihon.tts.ui.playback.ShortFormSpeechPhase
import mihon.tts.ui.playback.ShortFormSpeechRequest

/**
 * Reads the translation popup's original and translated text aloud. Speech stops as soon as the popup no longer
 * shows the text being read: another bubble was tapped, its languages or engine changed, or the popup closed.
 */
internal class MangaTranslationSpeechController(
    feature: TtsFeature,
    private val translation: StateFlow<TranslationSessionState>,
    scope: CoroutineScope,
    onFailure: (ShortFormSpeechFailure) -> Unit,
) : AutoCloseable {
    private val delegate = ShortFormSpeechController<TranslationResultSpeechTarget>(
        feature = feature,
        scope = scope,
        onFailure = onFailure,
    )

    val state: StateFlow<TranslationResultSpeechState> = delegate.state
        .map { speech ->
            TranslationResultSpeechState(
                activeTarget = speech.owner,
                phase = when (speech.phase) {
                    ShortFormSpeechPhase.Idle -> null
                    ShortFormSpeechPhase.Preparing -> TranslationResultSpeechPhase.Preparing
                    ShortFormSpeechPhase.Speaking -> TranslationResultSpeechPhase.Speaking
                },
            )
        }
        .stateIn(scope, SharingStarted.Eagerly, TranslationResultSpeechState())

    private val translationJob = translation
        .onEach { delegate.stopIfOwnerChanged(it.displayedSpeechTargets()) }
        .launchIn(scope)

    fun toggle(target: TranslationResultSpeechTarget) {
        if (target !in translation.value.displayedSpeechTargets()) return
        delegate.toggle(
            ShortFormSpeechRequest(
                owner = target,
                text = target.text,
                language = TtsLanguageSelection.Explicit(target.language),
            ),
        )
    }

    fun stopPlayback() = delegate.stopPlayback()

    override fun close() {
        translationJob.cancel()
        delegate.close()
    }
}

private fun TranslationSessionState.displayedSpeechTargets(): Set<TranslationResultSpeechTarget> =
    displayedSessionResult()?.speechTargets().orEmpty()
