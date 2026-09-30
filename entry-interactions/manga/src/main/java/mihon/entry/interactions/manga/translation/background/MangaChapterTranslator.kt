package mihon.entry.interactions.manga.translation.background

import logcat.LogPriority
import mihon.entry.interactions.manga.reader.text.geometry.MangaPageTransform
import mihon.entry.interactions.manga.reader.text.image.DisplayedStillImage
import mihon.entry.interactions.manga.reader.text.image.MangaDisplayedPageImage
import mihon.entry.interactions.manga.translation.artifact.MangaChapterTranslation
import mihon.entry.interactions.manga.translation.artifact.MangaChapterTranslationSetup
import mihon.entry.interactions.manga.translation.artifact.MangaTranslatedPage
import mihon.entry.interactions.manga.translation.artifact.MangaTranslatedRegion
import mihon.entry.interactions.manga.translation.context.mangaPageContext
import mihon.entry.interactions.manga.translation.pages.MangaDownloadedPage
import mihon.text.recognition.api.TextRecognitionFeature
import mihon.text.recognition.api.preparation.TextRecognitionPreparation
import mihon.text.recognition.api.preparation.TextRecognitionSetupPreparation
import mihon.text.recognition.api.request.TextRecognitionPriority
import mihon.text.recognition.api.request.TextRecognitionRequest
import mihon.text.recognition.api.request.TextRecognitionSetupRequest
import mihon.text.recognition.api.result.TextRecognitionExecution
import mihon.translation.api.TranslationFeature
import mihon.translation.api.engine.TranslationEngineSelection
import mihon.translation.api.preparation.TranslationRoutePreparation
import mihon.translation.api.request.TranslationBatch
import mihon.translation.api.request.TranslationContext
import mihon.translation.api.request.TranslationRouteRequest
import mihon.translation.api.request.TranslationTargetLanguageSelection
import mihon.translation.api.request.TranslationWorkContext
import mihon.translation.api.result.TranslationBatchUpdate
import okio.ByteString.Companion.toByteString
import tachiyomi.core.common.util.system.logcat

/**
 * Recognizes and translates the raw pages of a chapter with exactly the given setup, without asking anyone.
 *
 * The texts of a page are translated together, in light of the series and of the page before. A page that cannot be
 * decoded or recognized is left out, and a text that cannot be translated is kept without a translation; the reader
 * fills both in live. Anything that needs the user, such as a deleted model, stops the whole chapter instead.
 */
internal class MangaChapterTranslator(
    private val recognition: TextRecognitionFeature,
    private val translation: TranslationFeature,
) {
    suspend fun translate(
        pages: List<MangaDownloadedPage>,
        setup: MangaChapterTranslationSetup,
        work: TranslationWorkContext?,
        onPageDone: (done: Int) -> Unit,
    ): MangaChapterTranslationOutcome {
        if (!isReady(setup)) return MangaChapterTranslationOutcome.SetupRequired
        val translated = mutableListOf<MangaTranslatedPage>()
        var lastSkip: String? = null
        var textBefore = emptyList<String>()
        pages.forEachIndexed { index, page ->
            when (val outcome = translatePage(page, setup, mangaPageContext(work, textBefore))) {
                is PageOutcome.Translated -> {
                    translated += outcome.page
                    textBefore = outcome.page.regions.map { it.region.text }
                }
                is PageOutcome.Skipped -> {
                    logcat(LogPriority.WARN) { "Page ${page.fileName} left for the reader: ${outcome.reason}" }
                    lastSkip = outcome.reason
                    textBefore = emptyList()
                }
                PageOutcome.SetupRequired -> return MangaChapterTranslationOutcome.SetupRequired
            }
            onPageDone(index + 1)
        }
        if (translated.isEmpty()) {
            logcat(LogPriority.WARN) { "No page of ${pages.size} could be recognized" }
            return MangaChapterTranslationOutcome.NothingRecognized(lastSkip)
        }
        return MangaChapterTranslationOutcome.Translated(MangaChapterTranslation(setup, translated))
    }

    private suspend fun isReady(setup: MangaChapterTranslationSetup): Boolean {
        val recognitionSetup = recognition.prepare(TextRecognitionSetupRequest(setup.pageLanguage, setup.pipeline))
        if (recognitionSetup !is TextRecognitionSetupPreparation.Ready) return false
        val route = translation.prepareRoute(
            TranslationRouteRequest(
                sourceLanguage = setup.route.sourceLanguage,
                targetLanguage = TranslationTargetLanguageSelection.Explicit(setup.route.targetLanguage),
                engine = TranslationEngineSelection.Explicit(setup.route.engine),
            ),
        )
        return route is TranslationRoutePreparation.Ready && route.presentation.answersInline()
    }

    private suspend fun translatePage(
        page: MangaDownloadedPage,
        setup: MangaChapterTranslationSetup,
        context: TranslationContext,
    ): PageOutcome {
        val still = DisplayedStillImage(
            encoded = page.read().toByteString(),
            cropBorders = false,
            transform = MangaPageTransform.None,
            rawContent = null,
        )
        val image = MangaDisplayedPageImage.open(still) ?: return PageOutcome.Skipped("unsupported image")
        val result = image.use {
            val request = TextRecognitionRequest(
                image = image,
                language = setup.pageLanguage,
                pipeline = setup.pipeline,
                priority = TextRecognitionPriority.Background,
            )
            val ready = recognition.prepare(request) as? TextRecognitionPreparation.Ready
                ?: return PageOutcome.SetupRequired
            when (val execution = recognition.recognize(ready.recognition)) {
                is TextRecognitionExecution.Success -> execution.result
                is TextRecognitionExecution.PreparationChanged -> return PageOutcome.SetupRequired
                is TextRecognitionExecution.Failed -> return PageOutcome.Skipped(execution.message)
            }
        }
        val translations = arrayOfNulls<String>(result.regions.size)
        var blocked = false
        translation.translateBatch(TranslationBatch(setup.route, result.regions.map { it.text }, context))
            .collect { update ->
                when (update) {
                    is TranslationBatchUpdate.Translated -> translations[update.index] = update.text
                    is TranslationBatchUpdate.Failed -> Unit
                    is TranslationBatchUpdate.Blocked -> blocked = true
                }
            }
        if (blocked) return PageOutcome.SetupRequired
        val regions = result.regions.mapIndexed { index, region -> MangaTranslatedRegion(region, translations[index]) }
        return PageOutcome.Translated(MangaTranslatedPage(page.fileName, image.rawContent, image.size, regions))
    }

    private sealed interface PageOutcome {
        data class Translated(val page: MangaTranslatedPage) : PageOutcome

        data class Skipped(val reason: String?) : PageOutcome

        data object SetupRequired : PageOutcome
    }
}

internal sealed interface MangaChapterTranslationOutcome {
    data class Translated(val translation: MangaChapterTranslation) : MangaChapterTranslationOutcome

    /** The setup needs something from the user, such as a model download or a disclosure. */
    data object SetupRequired : MangaChapterTranslationOutcome

    /** No page could be recognized; [reason] is why the last one could not. */
    data class NothingRecognized(val reason: String?) : MangaChapterTranslationOutcome
}
