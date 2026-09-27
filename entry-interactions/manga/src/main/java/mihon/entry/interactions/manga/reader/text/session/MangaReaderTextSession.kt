package mihon.entry.interactions.manga.reader.text.session

import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import mihon.entry.interactions.manga.reader.text.overlay.overlayArea
import mihon.entry.interactions.manga.reader.text.overlay.sampleTextBackground
import mihon.entry.interactions.manga.reader.text.surface.MangaPageTextDecoration
import mihon.entry.interactions.manga.reader.text.surface.MangaPageTextSurface
import mihon.entry.interactions.manga.reader.text.translation.MangaPageTranslation
import mihon.entry.interactions.manga.reader.text.translation.MangaPageTranslator
import mihon.language.api.tag.LanguageTag
import mihon.model.artifacts.api.ModelArtifactStore
import mihon.model.artifacts.api.descriptor.ModelArtifactDescriptor
import mihon.model.artifacts.api.download.ModelArtifactDownloadApproval
import mihon.model.artifacts.api.state.ModelArtifactState
import mihon.text.recognition.api.TextRecognitionFeature
import mihon.text.recognition.api.component.TextRecognitionComponentId
import mihon.text.recognition.api.host.TextRecognitionPlatformModelsResult
import mihon.text.recognition.api.image.ImageRect
import mihon.text.recognition.api.image.TextRecognitionImage
import mihon.text.recognition.api.preparation.TextRecognitionPreparation
import mihon.text.recognition.api.preparation.TextRecognitionUnavailableReason
import mihon.text.recognition.api.request.TextRecognitionRequest
import mihon.text.recognition.api.request.TextRecognitionScope
import mihon.text.recognition.api.result.RecognizedTextRegion
import mihon.text.recognition.api.result.TextRecognitionExecution
import mihon.text.recognition.api.result.TextRecognitionResult

/**
 * Recognizes the text of the pages a reader displays while text features are active.
 *
 * The session recognizes only pages it is told are visible, stops at the first prerequisite the user must resolve,
 * and resumes on its own once that prerequisite is met.
 */
internal class MangaReaderTextSession(
    private val recognition: TextRecognitionFeature,
    private val modelStore: ModelArtifactStore,
    private val translator: MangaPageTranslator,
    private val installPlatformModels: suspend (
        TextRecognitionComponentId,
        LanguageTag,
    ) -> TextRecognitionPlatformModelsResult,
    private val scope: CoroutineScope,
    private val declaredLanguage: suspend () -> LanguageTag?,
    private val sampleBackground: suspend (TextRecognitionImage, ImageRect) -> Int = ::sampleTextBackground,
) {
    private val mutableState = MutableStateFlow(MangaReaderTextState())
    val state: StateFlow<MangaReaderTextState> = mutableState.asStateFlow()

    private var chosenLanguage: LanguageTag? = null
    private var visible: List<MangaPageTextSurface> = emptyList()
    private var ahead: List<MangaPageTextSurface> = emptyList()
    private val jobs = mutableMapOf<ReaderPage, Job>()
    private val translationJobs = mutableMapOf<ReaderPage, Job>()
    private var modelWait: Job? = null

    fun setActive(active: Boolean) {
        if (mutableState.value.active == active) return
        if (!active) {
            cancelRecognition()
            cancelTranslation()
            mutableState.update { MangaReaderTextState(language = it.language, overlay = it.overlay) }
            return
        }
        mutableState.update { it.copy(active = true, blocker = null) }
        recognizeVisible()
    }

    /** Draws translations over recognized text, or goes back to translating tapped text only. */
    fun setOverlay(enabled: Boolean) {
        if (mutableState.value.overlay == enabled) return
        mutableState.update { it.copy(overlay = enabled, showOriginal = false, translationIssue = null) }
        if (enabled) translateRecognized() else cancelTranslation()
    }

    fun toggleOriginal() {
        mutableState.update { it.copy(showOriginal = !it.showOriginal) }
    }

    /**
     * Recognizes [surfaces] that have not been recognized yet and stops work for pages no longer on screen. With
     * translations drawn over pages, [preloaded] pages are processed too, after the visible ones.
     */
    fun onVisibleSurfaces(
        surfaces: List<MangaPageTextSurface>,
        preloaded: List<MangaPageTextSurface> = emptyList(),
    ) {
        visible = surfaces
        ahead = preloaded.filter { candidate -> surfaces.none { it.page == candidate.page } }
        mutableState.update { it.copy(visiblePages = surfaces.map(MangaPageTextSurface::page)) }
        val pages = (surfaces + ahead).map(MangaPageTextSurface::page).toSet()
        translationJobs.keys.filterNot(pages::contains).forEach { page ->
            translationJobs.remove(page)?.cancel()
            mutableState.update { it.copy(translating = it.translating - page) }
        }
        jobs.keys.filterNot(pages::contains).forEach { page ->
            jobs.remove(page)?.cancel()
            mutableState.update { state ->
                if (state.pages[page] == MangaPageTextStatus.Recognizing) {
                    state.copy(pages = state.pages - page)
                } else {
                    state
                }
            }
        }
        recognizeVisible()
    }

    fun chooseLanguage(language: LanguageTag) {
        chosenLanguage = language
        resume()
    }

    /** Downloads approved models and resumes recognition when all of them are installed. */
    fun approveModels(approvals: List<ModelArtifactDownloadApproval>) {
        approvals.forEach(modelStore::download)
        modelWait?.cancel()
        modelWait = scope.launch {
            val models = approvals.map { it.artifact }
            combine(models.map(modelStore::observe)) { states -> states.all { it is ModelArtifactState.Installed } }
                .first { it }
            resume()
        }
    }

    /** Has the platform install models the user approved, and resumes recognition once they are installed. */
    fun approvePlatformModels(blocker: MangaReaderTextBlocker.PlatformModelsRequired) {
        mutableState.update { it.copy(blocker = blocker.copy(installing = true)) }
        modelWait?.cancel()
        modelWait = scope.launch {
            when (val result = installPlatformModels(blocker.component, blocker.language)) {
                TextRecognitionPlatformModelsResult.Installed -> resume()
                is TextRecognitionPlatformModelsResult.Failed ->
                    mutableState.update { it.copy(blocker = MangaReaderTextBlocker.Unavailable(result.reason)) }
            }
        }
    }

    /** Installation states of [models], in order, for showing download progress. */
    fun observeModels(models: List<ModelArtifactDescriptor>): Flow<List<ModelArtifactState>> =
        combine(models.map(modelStore::observe)) { it.toList() }

    /** Re-evaluates prerequisites, for example after the user returned from settings. */
    fun resume() {
        if (!mutableState.value.active) return
        mutableState.update { state ->
            state.copy(
                blocker = null,
                translationIssue = null,
                pages = state.pages.filterValues { it is MangaPageTextStatus.Recognized },
            )
        }
        recognizeVisible()
        translateRecognized()
    }

    fun result(page: ReaderPage): TextRecognitionResult? =
        (mutableState.value.pages[page] as? MangaPageTextStatus.Recognized)?.result

    /** The recognized region whose bubble (or, without one, whose text) contains image point ([x], [y]). */
    fun regionAt(page: ReaderPage, x: Int, y: Int): RecognizedTextRegion? =
        result(page)?.regions?.firstOrNull { region -> (region.container ?: region.bounds).contains(x, y) }

    fun highlight(page: ReaderPage, bounds: ImageRect) {
        mutableState.update { it.copy(highlighted = MangaPageTextHighlight(page, bounds)) }
    }

    fun clearHighlight() {
        mutableState.update { it.copy(highlighted = null) }
    }

    /** Recognizes only the text inside [area] of [surface], for an area the user outlined. */
    suspend fun recognizeArea(surface: MangaPageTextSurface, area: ImageRect): TextRecognitionResult? {
        val image = surface.displayedImage() ?: return null
        return image.use {
            val request = TextRecognitionRequest(image, language(), TextRecognitionScope.Region(area))
            when (val preparation = recognition.prepare(request)) {
                is TextRecognitionPreparation.Ready ->
                    (recognition.recognize(preparation.recognition) as? TextRecognitionExecution.Success)?.result
                else -> {
                    block(preparation)
                    null
                }
            }
        }
    }

    fun decoration(page: ReaderPage): Flow<MangaPageTextDecoration?> = state
        .map { state ->
            val result = (state.pages[page] as? MangaPageTextStatus.Recognized)?.result
            if (!state.active || result == null) return@map null
            val overlays = if (state.overlay && !state.showOriginal) state.overlays[page].orEmpty() else emptyList()
            val translated = overlays.mapTo(HashSet()) { it.source }
            MangaPageTextDecoration(
                imageSize = result.imageSize,
                regions = result.regions.filterNot { it.bounds in translated }.map { it.container ?: it.bounds },
                highlighted = state.highlighted?.takeIf { it.page == page }?.bounds,
                overlays = overlays,
            )
        }
        .distinctUntilChanged()

    private fun recognizeVisible() {
        val state = mutableState.value
        if (!state.active || state.blocker != null) return
        (if (state.overlay) visible + ahead else visible).forEach { surface ->
            val page = surface.page
            if (page in state.pages || jobs[page]?.isActive == true) return@forEach
            jobs[page] = scope.launch {
                try {
                    recognize(surface)
                } finally {
                    jobs.remove(page, coroutineContext.job)
                }
            }
        }
    }

    private suspend fun recognize(surface: MangaPageTextSurface) {
        val page = surface.page
        val image = surface.displayedImage() ?: return
        image.use {
            val preparation = recognition.prepare(TextRecognitionRequest(image, language()))
            if (preparation !is TextRecognitionPreparation.Ready) {
                block(preparation)
                return
            }
            mutableState.update { it.copy(language = preparation.language) }
            setStatus(page, MangaPageTextStatus.Recognizing)
            val status = when (val execution = recognition.recognize(preparation.recognition)) {
                is TextRecognitionExecution.Success -> MangaPageTextStatus.Recognized(execution.result)
                is TextRecognitionExecution.PreparationChanged -> {
                    mutableState.update { it.copy(pages = it.pages - page) }
                    block(execution.preparation)
                    return
                }
                is TextRecognitionExecution.Failed -> MangaPageTextStatus.Failed(execution.message)
            }
            setStatus(page, status)
        }
        if (mutableState.value.overlay) translateRecognized()
    }

    /** Translates recognized on-screen and preloaded pages that have no translations yet, on-screen pages first. */
    private fun translateRecognized() {
        val state = mutableState.value
        if (!state.active || !state.overlay || state.translationIssue != null) return
        (visible + ahead).forEach { surface ->
            val page = surface.page
            val result = (state.pages[page] as? MangaPageTextStatus.Recognized)?.result ?: return@forEach
            if (page in state.overlays || translationJobs[page]?.isActive == true) return@forEach
            translationJobs[page] = scope.launch {
                var completed = false
                try {
                    completed = translate(surface, result)
                } finally {
                    translationJobs.remove(page, coroutineContext.job)
                    // An interrupted page starts over later instead of keeping a partial set of translations.
                    mutableState.update { state ->
                        state.copy(
                            translating = state.translating - page,
                            overlays = if (completed) state.overlays else state.overlays - page,
                        )
                    }
                }
            }
        }
    }

    /** Translates every region of [result]; returns whether the page is complete. */
    private suspend fun translate(surface: MangaPageTextSurface, result: TextRecognitionResult): Boolean {
        val page = surface.page
        val image = surface.displayedImage() ?: return false
        mutableState.update { it.copy(translating = it.translating + page) }
        image.use {
            val pageText = result.regions.joinToString("\n") { it.text }
            val overlays = mutableListOf<MangaPageTextOverlay>()
            for (region in result.regions) {
                val text = when (val translation = translator.translate(region.text, result.language, pageText)) {
                    is MangaPageTranslation.Translated -> translation.text
                    MangaPageTranslation.Skipped -> continue
                    MangaPageTranslation.SetupRequired -> {
                        reportTranslationIssue(MangaPageTranslationIssue.SetupRequired)
                        return false
                    }
                    MangaPageTranslation.EngineUnsupported -> {
                        reportTranslationIssue(MangaPageTranslationIssue.EngineUnsupported)
                        return false
                    }
                }
                overlays += MangaPageTextOverlay(
                    source = region.bounds,
                    area = overlayArea(region),
                    text = text,
                    background = sampleBackground(image, region.bounds),
                )
                // Translations appear bubble by bubble instead of all at once.
                mutableState.update { it.copy(overlays = it.overlays + (page to overlays.toList())) }
            }
            mutableState.update { it.copy(overlays = it.overlays + (page to overlays.toList())) }
        }
        return true
    }

    private fun reportTranslationIssue(issue: MangaPageTranslationIssue) {
        cancelTranslation()
        mutableState.update { it.copy(translationIssue = issue) }
    }

    private fun cancelTranslation() {
        translationJobs.values.forEach(Job::cancel)
        translationJobs.clear()
        mutableState.update { it.copy(translating = emptySet()) }
    }

    private suspend fun language(): LanguageTag? = chosenLanguage ?: declaredLanguage()

    private fun setStatus(page: ReaderPage, status: MangaPageTextStatus) {
        mutableState.update { state -> state.copy(pages = state.pages + (page to status)) }
    }

    private fun block(preparation: TextRecognitionPreparation) {
        val blocker = when (preparation) {
            is TextRecognitionPreparation.Ready -> return
            is TextRecognitionPreparation.LanguageRequired ->
                MangaReaderTextBlocker.LanguageRequired(preparation.supportedLanguages)
            is TextRecognitionPreparation.ModelsRequired -> MangaReaderTextBlocker.ModelsRequired(preparation.models)
            is TextRecognitionPreparation.PlatformModelsRequired -> MangaReaderTextBlocker.PlatformModelsRequired(
                component = preparation.component,
                language = preparation.language,
                description = preparation.description,
                approximateSizeBytes = preparation.approximateSizeBytes,
            )
            is TextRecognitionPreparation.PipelineChoiceRequired ->
                MangaReaderTextBlocker.PipelineChoiceRequired(preparation.language)
            is TextRecognitionPreparation.Unavailable -> when (val reason = preparation.reason) {
                is TextRecognitionUnavailableReason.UnsupportedLanguage ->
                    MangaReaderTextBlocker.UnsupportedLanguage(reason.language)
                is TextRecognitionUnavailableReason.ComponentUnavailable ->
                    MangaReaderTextBlocker.Unavailable(reason.reason)
            }
        }
        cancelRecognition()
        mutableState.update { it.copy(blocker = blocker) }
    }

    private fun cancelRecognition() {
        jobs.values.forEach(Job::cancel)
        jobs.clear()
        modelWait?.cancel()
        modelWait = null
        mutableState.update { state ->
            state.copy(pages = state.pages.filterValues { it !is MangaPageTextStatus.Recognizing })
        }
    }
}
