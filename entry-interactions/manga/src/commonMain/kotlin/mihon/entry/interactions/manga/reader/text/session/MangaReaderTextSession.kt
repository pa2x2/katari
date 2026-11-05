package mihon.entry.interactions.manga.reader.text.session

import eu.kanade.tachiyomi.ui.reader.model.InsertPage
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
import mihon.entry.interactions.manga.reader.text.stored.MangaStoredPage
import mihon.entry.interactions.manga.reader.text.stored.pageShownBy
import mihon.entry.interactions.manga.reader.text.surface.MangaPageTextDecoration
import mihon.entry.interactions.manga.reader.text.surface.MangaPageTextSurface
import mihon.entry.interactions.manga.reader.text.translation.MangaPageTranslation
import mihon.entry.interactions.manga.reader.text.translation.MangaPageTranslationIssue
import mihon.entry.interactions.manga.reader.text.translation.MangaPageTranslator
import mihon.entry.interactions.manga.translation.artifact.MangaChapterTranslation
import mihon.entry.interactions.manga.translation.context.mangaPageContext
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
import mihon.translation.api.request.TranslationWorkContext

/**
 * Recognizes the text of the pages a reader displays while text features are active.
 *
 * The session recognizes only pages it is told are visible, stops at the first prerequisite the user must resolve,
 * and resumes on its own once that prerequisite is met. Pages are read in the [pageLanguage] kept for the series, or
 * else in the source's [declaredLanguage], which is null until the series is known or when the source spans several
 * languages; a different page language reads them again.
 *
 * Pages with a [storedTranslation] made from the same raw file are drawn from it instead, whatever the current
 * settings, and only its gaps are translated live. Live translations are made in light of the series, described by
 * [work], and of the page read before.
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
    declaredLanguage: Flow<LanguageTag?>,
    pageLanguage: Flow<LanguageTag?>,
    private val storedTranslation: suspend (ReaderPage) -> MangaChapterTranslation? = { null },
    private val work: () -> TranslationWorkContext? = { null },
    private val sampleBackground: suspend (TextRecognitionImage, ImageRect) -> Int = ::sampleTextBackground,
) {
    private val mutableState = MutableStateFlow(MangaReaderTextState())
    val state: StateFlow<MangaReaderTextState> = mutableState.asStateFlow()

    private var keptLanguage: LanguageTag? = null
    private var declared: LanguageTag? = null
    private var visible: List<MangaPageTextSurface> = emptyList()
    private var ahead: List<MangaPageTextSurface> = emptyList()
    private val jobs = mutableMapOf<ReaderPage, Job>()
    private val translationJobs = mutableMapOf<ReaderPage, Job>()
    private var modelWait: Job? = null

    /** Translations drawn from storage, which translating again with other choices keeps. */
    private val storedOverlays = mutableMapOf<ReaderPage, List<MangaPageTextOverlay>>()

    /** Stored pages with regions the stored translation lacks, and those still to be translated live. */
    private val storedGaps = mutableSetOf<ReaderPage>()
    private val pendingGaps = mutableSetOf<ReaderPage>()

    init {
        scope.launch {
            pageLanguage.distinctUntilChanged().collect { kept ->
                keptLanguage = kept
                usePageLanguage()
            }
        }
        scope.launch {
            declaredLanguage.distinctUntilChanged().collect { language ->
                declared = language
                usePageLanguage()
            }
        }
        scope.launch { translator.choicesChanged.collect { translateAgain() } }
    }

    fun setActive(active: Boolean) {
        if (mutableState.value.active == active) return
        if (!active) {
            cancelRecognition()
            cancelTranslation()
            forgetStoredPages()
            mutableState.update {
                MangaReaderTextState(
                    language = it.language,
                    declaredLanguage = it.declaredLanguage,
                    languageKept = it.languageKept,
                    overlay = it.overlay,
                )
            }
            return
        }
        mutableState.update { it.copy(active = true, blocker = null) }
        recognizeVisible()
    }

    /** Processes pages drawn from a stored translation live again, after that translation was deleted. */
    fun reloadStoredPages() {
        val stored = storedOverlays.keys + mutableState.value.storedSetups.keys
        stored.forEach { translationJobs.remove(it)?.cancel() }
        forgetStoredPages()
        mutableState.update { state ->
            state.copy(
                pages = state.pages - stored,
                overlays = state.overlays - stored,
                translating = state.translating - stored,
            )
        }
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
    suspend fun recognizeArea(image: TextRecognitionImage, area: ImageRect): TextRecognitionResult? {
        val request = TextRecognitionRequest(image, language(), TextRecognitionScope.Region(area))
        return when (val preparation = recognition.prepare(request)) {
            is TextRecognitionPreparation.Ready ->
                (recognition.recognize(preparation.recognition) as? TextRecognitionExecution.Success)?.result
            else -> {
                block(preparation)
                null
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
            val stored = storedTranslation(page)?.pageShownBy(image)
            if (stored != null) {
                showStored(page, image, stored)
                if (mutableState.value.overlay) translateRecognized()
                return
            }
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

    private suspend fun showStored(page: ReaderPage, image: TextRecognitionImage, stored: MangaStoredPage) {
        val overlays = stored.result.regions.mapNotNull { region ->
            stored.translations[region.bounds]?.let { text ->
                MangaPageTextOverlay(
                    source = region.bounds,
                    area = overlayArea(region),
                    text = text,
                    background = sampleBackground(image, region.bounds),
                )
            }
        }
        storedOverlays[page] = overlays
        if (overlays.size < stored.result.regions.size) {
            storedGaps += page
            pendingGaps += page
        }
        mutableState.update {
            it.copy(
                pages = it.pages + (page to MangaPageTextStatus.Recognized(stored.result)),
                overlays = it.overlays + (page to overlays),
                storedSetups = it.storedSetups + (page to stored.setup),
            )
        }
    }

    private fun forgetStoredPages() {
        storedOverlays.clear()
        storedGaps.clear()
        pendingGaps.clear()
        mutableState.update { it.copy(storedSetups = emptyMap()) }
    }

    /**
     * Translates recognized on-screen and preloaded pages that have no translations yet, and the gaps of stored ones,
     * on-screen pages first.
     */
    private fun translateRecognized() {
        val state = mutableState.value
        if (!state.active || !state.overlay || state.translationIssue != null) return
        (visible + ahead).forEach { surface ->
            val page = surface.page
            val result = (state.pages[page] as? MangaPageTextStatus.Recognized)?.result ?: return@forEach
            val translated = page in state.overlays && page !in pendingGaps
            if (translated || translationJobs[page]?.isActive == true) return@forEach
            translationJobs[page] = scope.launch {
                var completed = false
                try {
                    completed = translate(surface, result)
                } finally {
                    translationJobs.remove(page, coroutineContext.job)
                    // An interrupted page starts over later instead of keeping a partial set of translations, apart
                    // from those it has stored.
                    if (completed) pendingGaps -= page
                    mutableState.update { state ->
                        state.copy(
                            translating = state.translating - page,
                            overlays = when {
                                completed -> state.overlays
                                else -> storedOverlays[page]?.let { state.overlays + (page to it) }
                                    ?: (state.overlays - page)
                            },
                        )
                    }
                }
            }
        }
    }

    /**
     * Translates the regions of [result] without a stored translation, together and in light of what is read before
     * them; returns whether the page is complete.
     */
    private suspend fun translate(surface: MangaPageTextSurface, result: TextRecognitionResult): Boolean {
        val page = surface.page
        val image = surface.displayedImage() ?: return false
        mutableState.update { it.copy(translating = it.translating + page) }
        image.use {
            val overlays = storedOverlays[page].orEmpty().toMutableList()
            val stored = overlays.mapTo(HashSet()) { it.source }
            val regions = result.regions.filterNot { it.bounds in stored }
            val readBefore = textBefore(page) + result.regions.takeWhile { it.bounds in stored }.map { it.text }
            val context = mangaPageContext(work(), readBefore)
            var issue: MangaPageTranslationIssue? = null
            translator.translate(regions.map { it.text }, result.language, context).collect { translation ->
                when (translation) {
                    is MangaPageTranslation.Translated -> {
                        val region = regions[translation.index]
                        overlays += MangaPageTextOverlay(
                            source = region.bounds,
                            area = overlayArea(region),
                            text = translation.text,
                            background = sampleBackground(image, region.bounds),
                        )
                        // Translations appear as soon as the engine has them instead of once the page is done.
                        mutableState.update { it.copy(overlays = it.overlays + (page to overlays.toList())) }
                    }
                    is MangaPageTranslation.Blocked -> issue = translation.issue
                }
            }
            issue?.let {
                reportTranslationIssue(it)
                return false
            }
            mutableState.update { it.copy(overlays = it.overlays + (page to overlays.toList())) }
        }
        return true
    }

    /**
     * The text of the page read right before [page] in its chapter, in reading order, or nothing when that page is
     * not among those recognized here. A page still being recognized is waited for, so that reading pages in order
     * gives each the same context however fast the pages around it are recognized.
     */
    private suspend fun textBefore(page: ReaderPage): List<String> {
        fun ReaderPage.isReadBefore(): Boolean = when {
            page is InsertPage -> this === page.parent
            else -> index == page.index - 1 && chapter == page.chapter
        }

        // The second half of a split page is read after its first half.
        fun Collection<ReaderPage>.readBefore(): ReaderPage? =
            filter { it.isReadBefore() }.let { pages -> pages.firstOrNull { it is InsertPage } ?: pages.firstOrNull() }

        jobs.keys.readBefore()?.let { jobs[it]?.join() }
        val pages = mutableState.value.pages
        val recognized = pages.keys.readBefore()?.let { pages[it] } as? MangaPageTextStatus.Recognized
        return recognized?.result?.regions.orEmpty().map { it.text }
    }

    /** Replaces live translations with ones made with the current target and engine; stored ones stay. */
    private fun translateAgain() {
        cancelTranslation()
        pendingGaps += storedGaps
        mutableState.update { it.copy(overlays = storedOverlays.toMap(), translationIssue = null) }
        translateRecognized()
    }

    private fun reportTranslationIssue(issue: MangaPageTranslationIssue) {
        cancelTranslation()
        mutableState.update { it.copy(translationIssue = issue) }
    }

    private fun cancelTranslation() {
        translationJobs.cancelAll()
        mutableState.update { it.copy(translating = emptySet()) }
    }

    private fun language(): LanguageTag? = keptLanguage ?: declared

    /** Reads pages in the kept language, or else in the declared one, starting over when that changes the language. */
    private fun usePageLanguage() {
        val language = language()
        val previous = mutableState.value
        mutableState.update { it.copy(declaredLanguage = declared, languageKept = keptLanguage != null) }
        if (language != null && language == previous.language) return
        cancelRecognition()
        cancelTranslation()
        forgetStoredPages()
        mutableState.update {
            it.copy(
                language = language,
                blocker = null,
                translationIssue = null,
                pages = emptyMap(),
                overlays = emptyMap(),
                highlighted = null,
            )
        }
        recognizeVisible()
    }

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
        jobs.cancelAll()
        modelWait?.cancel()
        modelWait = null
        mutableState.update { state ->
            state.copy(pages = state.pages.filterValues { it !is MangaPageTextStatus.Recognizing })
        }
    }

    /**
     * Cancels every job and forgets it. A cancelled job may finish on the spot and take itself out of the map, so the
     * map is emptied before any job is cancelled instead of while they are gone through.
     */
    private fun MutableMap<ReaderPage, Job>.cancelAll() {
        val cancelled = values.toList()
        clear()
        cancelled.forEach(Job::cancel)
    }
}
