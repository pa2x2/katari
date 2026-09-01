package mihon.entry.interactions.book.reader.speech

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import mihon.language.api.tag.LanguageTag
import mihon.translation.ui.presentation.TranslationResultSpeechTarget
import mihon.tts.api.TtsFeature
import mihon.tts.ui.playback.ShortFormSpeechController
import mihon.tts.ui.playback.ShortFormSpeechFailure
import mihon.tts.ui.playback.ShortFormSpeechPhase
import mihon.tts.ui.playback.ShortFormSpeechRequest
import mihon.tts.ui.playback.ShortFormSpeechState

internal typealias BookShortFormSpeechPhase = ShortFormSpeechPhase
internal typealias BookShortFormSpeechRequest = ShortFormSpeechRequest<BookShortFormSpeechOwner>
internal typealias BookShortFormSpeechState = ShortFormSpeechState<BookShortFormSpeechOwner>
internal typealias BookShortFormSpeechFailure = ShortFormSpeechFailure

internal sealed interface BookShortFormSpeechOwner {
    data class Selection(val identity: String) : BookShortFormSpeechOwner

    data class TranslationResult(
        val target: TranslationResultSpeechTarget,
    ) : BookShortFormSpeechOwner
}

internal class BookShortFormSpeechController(
    feature: TtsFeature,
    scope: CoroutineScope,
    onFailure: (BookShortFormSpeechFailure) -> Unit,
    onLanguageResolved: (LanguageTag) -> Unit = {},
) : AutoCloseable {
    private val delegate = ShortFormSpeechController<BookShortFormSpeechOwner>(
        feature = feature,
        scope = scope,
        onFailure = onFailure,
        onLanguageResolved = onLanguageResolved,
    )
    val state: StateFlow<BookShortFormSpeechState> = delegate.state

    fun toggle(request: BookShortFormSpeechRequest) = delegate.toggle(request)

    fun onSelectionChanged(selectionIdentity: String) {
        val owner = state.value.owner as? BookShortFormSpeechOwner.Selection ?: return
        if (owner.identity != selectionIdentity) delegate.stopPlayback()
    }

    fun clearSelection(selectionIdentity: String? = null) {
        val owner = state.value.owner as? BookShortFormSpeechOwner.Selection ?: return
        if (selectionIdentity == null || owner.identity == selectionIdentity) delegate.stopPlayback()
    }

    fun clearTranslationResult() {
        if (state.value.owner is BookShortFormSpeechOwner.TranslationResult) delegate.stopPlayback()
    }

    fun stopPlayback() = delegate.stopPlayback()

    override fun close() = delegate.close()
}
