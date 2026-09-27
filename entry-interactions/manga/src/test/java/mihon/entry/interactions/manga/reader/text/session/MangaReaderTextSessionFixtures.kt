package mihon.entry.interactions.manga.reader.text.session

import android.graphics.Bitmap
import android.graphics.RectF
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import mihon.entry.interactions.manga.reader.text.image.DisplayedPageImage
import mihon.entry.interactions.manga.reader.text.surface.MangaPageTextDecoration
import mihon.entry.interactions.manga.reader.text.surface.MangaPageTextSurface
import mihon.language.api.tag.LanguageTag
import mihon.model.artifacts.api.ModelArtifactStore
import mihon.model.artifacts.api.descriptor.ModelArtifactDescriptor
import mihon.model.artifacts.api.descriptor.ModelArtifactFile
import mihon.model.artifacts.api.descriptor.ModelArtifactHosting
import mihon.model.artifacts.api.descriptor.ModelArtifactId
import mihon.model.artifacts.api.descriptor.ModelArtifactLicense
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
import mihon.text.recognition.api.request.TextRecognitionRequest
import mihon.text.recognition.api.result.RecognizedTextRegion
import mihon.text.recognition.api.result.TextOrientation
import mihon.text.recognition.api.result.TextRecognitionExecution
import mihon.text.recognition.api.result.TextRecognitionResult
import mihon.text.recognition.api.result.TextRegionKind
import java.io.File

internal val JAPANESE = LanguageTag.require("ja")
internal val PAGE_SIZE = ImageSize(1200, 1800)
internal val PIPELINE = TextRecognitionPipeline.Staged(
    TextRecognitionComponentId("example.detector"),
    TextRecognitionComponentId("example.recognizer"),
)
internal val MODEL = ModelArtifactDescriptor(
    id = ModelArtifactId("example.model"),
    revision = "r1",
    displayName = "Example model",
    files = listOf(ModelArtifactFile("model.onnx", "https://models.example/model.onnx", 1, "0".repeat(64))),
    license = ModelArtifactLicense("Apache-2.0", "https://license.example"),
    hosting = ModelArtifactHosting.Upstream("https://models.example"),
)

internal fun bubble(text: String, bounds: ImageRect, container: ImageRect) = RecognizedTextRegion(
    bounds = bounds,
    text = text,
    kind = TextRegionKind.SpeechBubble,
    orientation = TextOrientation.Vertical,
    container = container,
)

/** A visible page whose image is always available. */
internal class FakeSurface(private val index: Int) : MangaPageTextSurface {
    override val page = ReaderPage(index)
    var decoration: MangaPageTextDecoration? = null

    override suspend fun displayedImage(): DisplayedPageImage = object : DisplayedPageImage {
        override val key = ImageContentKey("page-$index")
        override val size = PAGE_SIZE
        override suspend fun decodeRegion(region: ImageRect, sampleSize: Int): Bitmap = mockk(relaxed = true)
        override fun close() = Unit
    }

    override fun imageToWindow(rect: ImageRect, imageSize: ImageSize): RectF? = null

    override fun windowToImage(x: Float, y: Float, imageSize: ImageSize): Pair<Int, Int>? = null

    override fun setTextDecoration(decoration: MangaPageTextDecoration?) {
        this.decoration = decoration
    }
}

/**
 * Prepares with [preparation] and answers every recognition with [regions]; recognition waits for [release] when it
 * is set, so tests can observe work in progress.
 */
internal class FakeTextRecognition : TextRecognitionFeature {
    var preparation: (TextRecognitionRequest) -> TextRecognitionPreparation = { request ->
        TextRecognitionPreparation.Ready(Ready(request), JAPANESE, PIPELINE)
    }
    var regions: List<RecognizedTextRegion> = emptyList()
    var release: CompletableDeferred<Unit>? = null
    val recognized = mutableListOf<ImageContentKey>()

    override suspend fun prepare(request: TextRecognitionRequest) = preparation(request)

    override suspend fun recognize(ready: ReadyTextRecognition): TextRecognitionExecution {
        release?.await()
        val request = (ready as Ready).request
        recognized += request.image.key
        return TextRecognitionExecution.Success(
            TextRecognitionResult(request.image.key, request.image.size, JAPANESE, regions),
        )
    }

    private class Ready(val request: TextRecognitionRequest) : ReadyTextRecognition
}

internal class FakeModelStore : ModelArtifactStore {
    val states = mutableMapOf<ModelArtifactDescriptor, MutableStateFlow<ModelArtifactState>>()
    val approved = mutableListOf<ModelArtifactDownloadApproval>()

    fun state(model: ModelArtifactDescriptor) = states.getOrPut(model) {
        MutableStateFlow(ModelArtifactState.NotInstalled)
    }

    override fun observe(descriptor: ModelArtifactDescriptor): Flow<ModelArtifactState> = state(descriptor)

    override suspend fun installed(descriptor: ModelArtifactDescriptor): InstalledModelArtifact? =
        (state(descriptor).value as? ModelArtifactState.Installed)?.artifact

    override fun download(approval: ModelArtifactDownloadApproval) {
        approved += approval
    }

    override fun cancel(artifact: ModelArtifactDescriptor) = Unit

    override suspend fun delete(artifact: ModelArtifactId) = Unit

    override fun observeStored(): Flow<List<StoredModelArtifact>> = emptyFlow()

    fun install(model: ModelArtifactDescriptor) {
        state(model).value = ModelArtifactState.Installed(InstalledModelArtifact(model, File("models")))
    }
}
