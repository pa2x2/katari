package mihon.entry.interactions.manga.reader.text.session

import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import mihon.language.api.tag.LanguageTag
import mihon.model.artifacts.api.descriptor.ModelArtifactDescriptor
import mihon.text.recognition.api.image.ImageRect
import mihon.text.recognition.api.result.TextRecognitionResult

/**
 * Text features of one reader session.
 *
 * [pages] is keyed by page identity: a reloaded chapter produces new page objects and starts over.
 */
internal data class MangaReaderTextState(
    val active: Boolean = false,
    val language: LanguageTag? = null,
    val blocker: MangaReaderTextBlocker? = null,
    val visiblePages: List<ReaderPage> = emptyList(),
    val pages: Map<ReaderPage, MangaPageTextStatus> = emptyMap(),
    val highlighted: MangaPageTextHighlight? = null,
    /** Whether recognized text is translated and drawn over the page, rather than translated when tapped. */
    val overlay: Boolean = false,
    /** Temporarily reveals the original text under the drawn translations. */
    val showOriginal: Boolean = false,
    val overlays: Map<ReaderPage, List<MangaPageTextOverlay>> = emptyMap(),
    val translating: Set<ReaderPage> = emptySet(),
    val translationIssue: MangaPageTranslationIssue? = null,
) {
    /** What the reader should tell the user about the pages on screen. */
    val progress: MangaReaderTextProgress
        get() {
            blocker?.let { return MangaReaderTextProgress.Blocked(it) }
            if (overlay) translationIssue?.let { return MangaReaderTextProgress.TranslationUnavailable(it) }
            val statuses = visiblePages.map { pages[it] }
            val recognized = statuses.filterIsInstance<MangaPageTextStatus.Recognized>()
            return when {
                statuses.any { it == MangaPageTextStatus.Recognizing } -> MangaReaderTextProgress.Recognizing
                overlay && visiblePages.any { it in translating } -> MangaReaderTextProgress.Translating
                statuses.any { it is MangaPageTextStatus.Failed } -> MangaReaderTextProgress.Failed(
                    statuses.filterIsInstance<MangaPageTextStatus.Failed>().first().message,
                )
                recognized.any { it.result.regions.isNotEmpty() } -> MangaReaderTextProgress.Ready
                recognized.isNotEmpty() && recognized.size == statuses.size -> MangaReaderTextProgress.NoText
                else -> MangaReaderTextProgress.Waiting
            }
        }
}

internal sealed interface MangaReaderTextProgress {
    data class Blocked(val blocker: MangaReaderTextBlocker) : MangaReaderTextProgress

    /** No visible page has a displayed image to recognize yet. */
    data object Waiting : MangaReaderTextProgress

    data object Recognizing : MangaReaderTextProgress

    data object Translating : MangaReaderTextProgress

    /** Pages are recognized, but translations cannot be drawn on them. */
    data class TranslationUnavailable(val issue: MangaPageTranslationIssue) : MangaReaderTextProgress

    data object Ready : MangaReaderTextProgress

    data object NoText : MangaReaderTextProgress

    data class Failed(val message: String?) : MangaReaderTextProgress
}

/** A prerequisite the user must resolve before pages can be recognized. */
internal sealed interface MangaReaderTextBlocker {
    /** The manga does not declare one language; the user chooses which of [languages] its text is in. */
    data class LanguageRequired(val languages: List<LanguageTag>) : MangaReaderTextBlocker

    /** Recognition needs [models]; nothing downloads until the user approves them. */
    data class ModelsRequired(val models: List<ModelArtifactDescriptor>) : MangaReaderTextBlocker

    /** The profile has no usable pipeline for [language]; it is chosen in settings. */
    data class PipelineChoiceRequired(val language: LanguageTag) : MangaReaderTextBlocker

    data class UnsupportedLanguage(val language: LanguageTag) : MangaReaderTextBlocker

    data class Unavailable(val reason: String) : MangaReaderTextBlocker
}

internal sealed interface MangaPageTextStatus {
    data object Recognizing : MangaPageTextStatus

    data class Recognized(val result: TextRecognitionResult) : MangaPageTextStatus

    data class Failed(val message: String?) : MangaPageTextStatus
}

/**
 * A translation to draw over recognized text.
 *
 * @property source the recognized region the translation replaces.
 * @property area where the translation is drawn: the text and, inside a speech bubble, room around it.
 * @property background the color the original text is printed on, used to cover it.
 */
internal data class MangaPageTextOverlay(
    val source: ImageRect,
    val area: ImageRect,
    val text: String,
    val background: Int,
)

internal enum class MangaPageTranslationIssue {
    /** The translation engine needs the user (setup, consent, or language data) first. */
    SetupRequired,

    /** The chosen engine opens its own surface or needs an action per text, so nothing can be drawn. */
    EngineUnsupported,
}

internal data class MangaPageTextHighlight(
    val page: ReaderPage,
    val bounds: ImageRect,
)
