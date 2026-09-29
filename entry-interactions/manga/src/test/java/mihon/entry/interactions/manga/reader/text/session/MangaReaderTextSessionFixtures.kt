package mihon.entry.interactions.manga.reader.text.session

import android.graphics.Bitmap
import android.graphics.RectF
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import mihon.entry.interactions.manga.reader.text.image.DisplayedPageImage
import mihon.entry.interactions.manga.reader.text.surface.MangaPageTextDecoration
import mihon.entry.interactions.manga.reader.text.surface.MangaPageTextSurface
import mihon.language.api.tag.LanguageTag
import mihon.model.artifacts.api.ModelArtifactStore
import mihon.model.artifacts.api.descriptor.ModelArtifactDescriptor
import mihon.model.artifacts.api.descriptor.ModelArtifactId
import mihon.model.artifacts.api.download.ModelArtifactDownloadApproval
import mihon.model.artifacts.api.state.InstalledModelArtifact
import mihon.model.artifacts.api.state.ModelArtifactState
import mihon.model.artifacts.api.state.StoredModelArtifact
import mihon.text.recognition.api.TextRecognitionFeature
import mihon.text.recognition.api.component.TextRecognitionComponentId
import mihon.text.recognition.api.image.ImageContentKey
import mihon.text.recognition.api.image.ImageRect
import mihon.text.recognition.api.image.ImageSize
import mihon.text.recognition.api.pipeline.TextRecognitionPipeline
import mihon.text.recognition.api.preparation.ReadyTextRecognition
import mihon.text.recognition.api.preparation.TextRecognitionPreparation
import mihon.text.recognition.api.preparation.TextRecognitionSetupPreparation
import mihon.text.recognition.api.request.TextRecognitionRequest
import mihon.text.recognition.api.request.TextRecognitionSetupRequest
import mihon.text.recognition.api.result.TextRecognitionExecution
import mihon.text.recognition.api.result.TextRecognitionResult
import mihon.translation.api.TranslationFeature
import mihon.translation.api.engine.TranslationEngineId
import mihon.translation.api.engine.TranslationProviderId
import mihon.translation.api.preparation.ReadyTranslation
import mihon.translation.api.preparation.TranslationPreparation
import mihon.translation.api.preparation.TranslationRoutePreparation
import mihon.translation.api.provider.TranslationInvocationPolicy
import mihon.translation.api.provider.TranslationProviderPresentation
import mihon.translation.api.request.ResolvedTranslationRequest
import mihon.translation.api.request.ResolvedTranslationRoute
import mihon.translation.api.request.TranslationRequest
import mihon.translation.api.request.TranslationRouteRequest
import mihon.translation.api.result.TranslationExecution
import mihon.translation.api.result.TranslationResult

internal val JAPANESE = LanguageTag.require("ja")
private val PAGE_SIZE = ImageSize(1200, 1800)
private val PIPELINE = TextRecognitionPipeline(
    TextRecognitionComponentId("example.detector"),
    TextRecognitionComponentId("example.recognizer"),
)

/** A visible page whose image is always available. */
internal class FakeSurface(private val index: Int) : MangaPageTextSurface {
    override val page = ReaderPage(index)

    override suspend fun displayedImage(): DisplayedPageImage = object : DisplayedPageImage {
        override val key = ImageContentKey("page-$index")
        override val size = PAGE_SIZE
        override suspend fun decodeRegion(region: ImageRect, sampleSize: Int): Bitmap = mockk(relaxed = true)
        override fun close() = Unit
    }

    override fun imageToWindow(rect: ImageRect, imageSize: ImageSize): RectF? = null

    override fun windowToImage(x: Float, y: Float, imageSize: ImageSize): Pair<Int, Int>? = null

    override fun setTextDecoration(decoration: MangaPageTextDecoration?) = Unit
}

/**
 * Prepares every request as ready and answers every recognition without regions; recognition waits for [release]
 * when it is set, so tests can observe work in progress.
 */
internal class FakeTextRecognition : TextRecognitionFeature {
    var release: CompletableDeferred<Unit>? = null
    val recognized = mutableListOf<ImageContentKey>()

    override suspend fun prepare(request: TextRecognitionRequest): TextRecognitionPreparation =
        TextRecognitionPreparation.Ready(Ready(request), request.language ?: JAPANESE, PIPELINE)

    override suspend fun prepare(setup: TextRecognitionSetupRequest): TextRecognitionSetupPreparation =
        TextRecognitionSetupPreparation.Ready(setup.language ?: JAPANESE, PIPELINE)

    override suspend fun recognize(ready: ReadyTextRecognition): TextRecognitionExecution {
        release?.await()
        val request = (ready as Ready).request
        recognized += request.image.key
        return TextRecognitionExecution.Success(
            TextRecognitionResult(request.image.key, request.image.size, JAPANESE, emptyList()),
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

/** Translates text by upper-casing it. */
internal class FakeTranslation : TranslationFeature {
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
