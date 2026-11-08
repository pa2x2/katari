package app.katari.entry.interactions.manga.reader.text.session

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Rect
import app.katari.entry.interactions.manga.reader.text.geometry.MangaDisplayedPageGeometry
import app.katari.entry.interactions.manga.reader.text.image.DisplayedPageImage
import app.katari.entry.interactions.manga.reader.text.surface.MangaPageTextDecoration
import app.katari.entry.interactions.manga.reader.text.surface.MangaPageTextSurface
import app.katari.language.api.tag.LanguageTag
import app.katari.model.artifacts.api.ModelArtifactStore
import app.katari.model.artifacts.api.descriptor.ModelArtifactDescriptor
import app.katari.model.artifacts.api.descriptor.ModelArtifactId
import app.katari.model.artifacts.api.download.ModelArtifactDownloadApproval
import app.katari.model.artifacts.api.state.InstalledModelArtifact
import app.katari.model.artifacts.api.state.ModelArtifactState
import app.katari.model.artifacts.api.state.StoredModelArtifact
import app.katari.text.recognition.api.TextRecognitionFeature
import app.katari.text.recognition.api.component.TextRecognitionComponentId
import app.katari.text.recognition.api.image.ImageContentKey
import app.katari.text.recognition.api.image.ImageRect
import app.katari.text.recognition.api.image.ImageSize
import app.katari.text.recognition.api.pipeline.TextRecognitionPipeline
import app.katari.text.recognition.api.preparation.ReadyTextRecognition
import app.katari.text.recognition.api.preparation.TextRecognitionPreparation
import app.katari.text.recognition.api.preparation.TextRecognitionSetupPreparation
import app.katari.text.recognition.api.request.TextRecognitionRequest
import app.katari.text.recognition.api.request.TextRecognitionSetupRequest
import app.katari.text.recognition.api.result.RecognizedTextRegion
import app.katari.text.recognition.api.result.TextOrientation
import app.katari.text.recognition.api.result.TextRecognitionExecution
import app.katari.text.recognition.api.result.TextRecognitionResult
import app.katari.text.recognition.api.result.TextRegionKind
import app.katari.translation.api.TranslationFeature
import app.katari.translation.api.engine.TranslationEngineId
import app.katari.translation.api.engine.TranslationProviderId
import app.katari.translation.api.preparation.ReadyTranslation
import app.katari.translation.api.preparation.TranslationPreparation
import app.katari.translation.api.preparation.TranslationRoutePreparation
import app.katari.translation.api.provider.TranslationInvocationPolicy
import app.katari.translation.api.provider.TranslationProviderPresentation
import app.katari.translation.api.request.ResolvedTranslationRequest
import app.katari.translation.api.request.ResolvedTranslationRoute
import app.katari.translation.api.request.TranslationBatch
import app.katari.translation.api.request.TranslationRequest
import app.katari.translation.api.request.TranslationRouteRequest
import app.katari.translation.api.result.TranslationBatchUpdate
import app.katari.translation.api.result.TranslationExecution
import app.katari.translation.api.result.TranslationResult
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

internal val JAPANESE = LanguageTag.require("ja")
private val PAGE_SIZE = ImageSize(1200, 1800)
private val PIPELINE = TextRecognitionPipeline(
    TextRecognitionComponentId("example.detector"),
    TextRecognitionComponentId("example.recognizer"),
)

/** A visible page whose image is always available. */
internal class FakeSurface(private val index: Int, chapter: ReaderChapter? = null) : MangaPageTextSurface {
    override val page = ReaderPage(index).also { page -> chapter?.let { page.chapter = it } }

    override suspend fun displayedImage(): DisplayedPageImage = object : DisplayedPageImage {
        override val key = ImageContentKey("page-$index")
        override val size = PAGE_SIZE
        override val geometry = MangaDisplayedPageGeometry(PAGE_SIZE.bounds)
        override val rawContent = ImageContentKey("raw-page-$index")
        override suspend fun decodeRegion(region: ImageRect, sampleSize: Int): Bitmap = mockk(relaxed = true)
        override fun close() = Unit
    }

    override fun imageToWindow(rect: ImageRect, imageSize: ImageSize): Rect? = null

    override fun windowToImage(x: Float, y: Float, imageSize: ImageSize): Pair<Int, Int>? = null

    override fun setTextDecoration(decoration: MangaPageTextDecoration?) = Unit
}

/**
 * Prepares every request as ready and answers every recognition with the [texts] of the image, none unless set;
 * recognition waits for [release] when it is set, and an image in [held] for its own release, so tests can observe
 * work in progress.
 */
internal class FakeTextRecognition : TextRecognitionFeature {
    var release: CompletableDeferred<Unit>? = null
    val held = mutableMapOf<ImageContentKey, CompletableDeferred<Unit>>()
    var texts: Map<ImageContentKey, List<String>> = emptyMap()
    val recognized = mutableListOf<ImageContentKey>()

    override suspend fun prepare(request: TextRecognitionRequest): TextRecognitionPreparation =
        TextRecognitionPreparation.Ready(Ready(request), request.language ?: JAPANESE, PIPELINE)

    override suspend fun prepare(setup: TextRecognitionSetupRequest): TextRecognitionSetupPreparation =
        TextRecognitionSetupPreparation.Ready(setup.language ?: JAPANESE, PIPELINE)

    override suspend fun recognize(ready: ReadyTextRecognition): TextRecognitionExecution {
        release?.await()
        val request = (ready as Ready).request
        held[request.image.key]?.await()
        recognized += request.image.key
        val regions = texts[request.image.key].orEmpty().mapIndexed { index, text ->
            RecognizedTextRegion(
                bounds = ImageRect(0, index * 100, 100, index * 100 + 50),
                text = text,
                kind = TextRegionKind.SpeechBubble,
                orientation = TextOrientation.Vertical,
            )
        }
        return TextRecognitionExecution.Success(
            TextRecognitionResult(request.image.key, request.image.size, JAPANESE, regions),
        )
    }

    private class Ready(val request: TextRecognitionRequest) : ReadyTextRecognition
}

/** A store in which no model is installed. */
internal class FakeModelStore : ModelArtifactStore {
    override fun observe(descriptor: ModelArtifactDescriptor): Flow<ModelArtifactState> =
        flowOf(ModelArtifactState.NotInstalled)

    override suspend fun installed(descriptor: ModelArtifactDescriptor): InstalledModelArtifact? = null

    override fun download(approval: ModelArtifactDownloadApproval) = Unit

    override fun cancel(artifact: ModelArtifactDescriptor) = Unit

    override suspend fun delete(artifact: ModelArtifactId) = Unit

    override fun observeStored(): Flow<List<StoredModelArtifact>> = emptyFlow()
}

/** Translates text by upper-casing it, and remembers the [batches] it was given. */
internal class FakeTranslation : TranslationFeature {
    val batches = mutableListOf<TranslationBatch>()

    override suspend fun prepare(request: TranslationRequest): TranslationPreparation {
        return TranslationPreparation.Ready(
            translation = Ready(request.text),
            request = ResolvedTranslationRequest(
                text = request.text,
                sourceLanguage = JAPANESE,
                targetLanguage = ENGLISH,
                engine = TranslationEngineId("example"),
            ),
            presentation = PRESENTATION,
        )
    }

    override suspend fun prepareRoute(route: TranslationRouteRequest): TranslationRoutePreparation =
        TranslationRoutePreparation.Ready(
            route = ResolvedTranslationRoute(route.sourceLanguage, ENGLISH, TranslationEngineId("example")),
            presentation = PRESENTATION,
        )

    override fun translateBatch(batch: TranslationBatch): Flow<TranslationBatchUpdate> = flow {
        batches += batch
        batch.segments.forEachIndexed { index, text ->
            emit(TranslationBatchUpdate.Translated(index, text.uppercase()))
        }
    }

    override suspend fun translate(ready: ReadyTranslation): TranslationExecution {
        val text = (ready as Ready).text
        return TranslationExecution.Success(TranslationResult(text.uppercase(), JAPANESE, ENGLISH, PRESENTATION))
    }

    private class Ready(val text: String) : ReadyTranslation

    private companion object {
        val ENGLISH = LanguageTag.require("en")
        val PRESENTATION = TranslationProviderPresentation(
            providerId = TranslationProviderId("example"),
            providerName = "Example",
            engineName = "Example",
            invocationPolicy = TranslationInvocationPolicy.Immediate,
        )
    }
}
