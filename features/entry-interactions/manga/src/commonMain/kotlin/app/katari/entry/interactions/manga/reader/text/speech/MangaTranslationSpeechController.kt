package app.katari.entry.interactions.manga.reader.text.speech

import app.katari.translation.ui.presentation.TranslationResultSpeechPhase
import app.katari.translation.ui.presentation.TranslationResultSpeechState
import app.katari.translation.ui.presentation.TranslationResultSpeechTarget
import app.katari.translation.ui.presentation.speechTargets
import app.katari.translation.ui.session.TranslationSessionState
import app.katari.translation.ui.session.displayedSessionResult
import app.katari.tts.api.TtsFeature
import app.katari.tts.api.request.TtsLanguageSelection
import app.katari.tts.ui.playback.ShortFormSpeechController
import app.katari.tts.ui.playback.ShortFormSpeechFailure
import app.katari.tts.ui.playback.ShortFormSpeechPhase
import app.katari.tts.ui.playback.ShortFormSpeechRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

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
