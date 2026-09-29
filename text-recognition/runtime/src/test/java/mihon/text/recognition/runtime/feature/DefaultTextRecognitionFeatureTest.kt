package mihon.text.recognition.runtime.feature

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import mihon.model.artifacts.api.ModelArtifactStore
import mihon.model.artifacts.api.descriptor.ModelArtifactDescriptor
import mihon.model.artifacts.api.descriptor.ModelArtifactId
import mihon.model.artifacts.api.download.ModelArtifactDownloadApproval
import mihon.model.artifacts.api.state.InstalledModelArtifact
import mihon.model.artifacts.api.state.ModelArtifactState
import mihon.model.artifacts.api.state.StoredModelArtifact
import mihon.text.recognition.api.image.ImageRect
import mihon.text.recognition.api.image.ImageSize
import mihon.text.recognition.api.pipeline.TextRecognitionPipeline
import mihon.text.recognition.api.preparation.TextRecognitionPreparation
import mihon.text.recognition.api.request.TextRecognitionRequest
import mihon.text.recognition.api.result.TextRecognitionExecution
import mihon.text.recognition.runtime.EXAMPLE_PROVIDER
import mihon.text.recognition.runtime.FakeDetector
import mihon.text.recognition.runtime.FakePageImage
import mihon.text.recognition.runtime.FakeRecognizer
import mihon.text.recognition.runtime.JAPANESE
import mihon.text.recognition.runtime.cache.TextDetectionCache
import mihon.text.recognition.runtime.cache.TextRecognitionResultCache
import mihon.text.recognition.runtime.contribution
import mihon.text.recognition.runtime.execution.CachedRecognitionExecutor
import mihon.text.recognition.runtime.model
import mihon.text.recognition.runtime.pipeline.PageTextDetection
import mihon.text.recognition.runtime.preset
import mihon.text.recognition.runtime.registry.TextRecognitionComponentRegistry
import mihon.text.recognition.runtime.selection.ProfileTextRecognitionPreferences
import mihon.text.recognition.runtime.selection.TextRecognitionPipelineResolver
import mihon.text.recognition.spi.component.DetectedTextRegionKind
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import java.io.File

class DefaultTextRecognitionFeatureTest {

    @TempDir
    lateinit var cacheDirectory: File

    private val strip = FakePageImage(ImageSize(1200, 1800))
    private val bubble = ImageRect(100, 100, 500, 400)
    private val bubbleText = ImageRect(150, 150, 450, 350)
    private val detector = FakeDetector(
        page = strip,
        objects = listOf(
            bubble to DetectedTextRegionKind.Bubble,
            bubbleText to DetectedTextRegionKind.BubbleText,
        ),
        declaredModels = listOf(model("example.detector-model")),
    )
    private val recognizer = FakeRecognizer(
        page = strip,
        texts = mapOf(bubbleText to "上"),
        declaredModels = listOf(model("example.recognizer-model")),
    )
    private val stagedPreset = preset(
        id = "staged",
        pipeline = TextRecognitionPipeline(detector.catalogEntry.id, recognizer.catalogEntry.id),
    )
    private val registry = TextRecognitionComponentRegistry(
        listOf(
            contribution(EXAMPLE_PROVIDER, listOf(detector, recognizer), presets = listOf(stagedPreset)),
        ),
    )
    private val preferences = ProfileTextRecognitionPreferences(InMemoryPreferenceStore())
    private val modelStore = FakeModelArtifactStore(detector.declaredModels + recognizer.declaredModels)

    @Test
    fun `a repeated request is answered from the cache until a component's revision changes`() = runTest {
        val feature = feature()
        val first = feature.recognizeReady(TextRecognitionRequest(strip, JAPANESE))
        val detectorRuns = detector.runs
        val recognizerRuns = recognizer.runs

        feature().recognizeReady(TextRecognitionRequest(strip, JAPANESE)) shouldBe first
        detector.runs shouldBe detectorRuns
        recognizer.runs shouldBe recognizerRuns

        recognizer.processingRevision = 2
        feature.recognizeReady(TextRecognitionRequest(strip, JAPANESE))
        recognizer.runs shouldBe recognizerRuns * 2

        detector.processingRevision = 2
        feature.recognizeReady(TextRecognitionRequest(strip, JAPANESE))
        detector.runs shouldBe detectorRuns * 2
    }

    @Test
    fun `a model removed after preparation is reported as a changed preparation`() = runTest {
        val feature = feature()
        val ready = feature.prepare(TextRecognitionRequest(strip, JAPANESE))
            .shouldBeInstanceOf<TextRecognitionPreparation.Ready>()
        modelStore.installed -= detector.declaredModels.single()

        feature.recognize(ready.recognition) shouldBe TextRecognitionExecution.PreparationChanged(
            TextRecognitionPreparation.ModelsRequired(JAPANESE, stagedPreset.pipeline, detector.declaredModels),
        )
    }

    private fun feature(registry: TextRecognitionComponentRegistry = this.registry) = DefaultTextRecognitionFeature(
        registry = registry,
        resolver = TextRecognitionPipelineResolver(registry),
        preferences = preferences,
        modelStore = modelStore,
        executor = CachedRecognitionExecutor(
            cache = TextRecognitionResultCache({ cacheDirectory }, maximumBytes = 1_000_000),
            ioDispatcher = Dispatchers.Unconfined,
            inferenceDispatcher = Dispatchers.Unconfined,
        ),
        detection = PageTextDetection(TextDetectionCache(maximumPages = 4)),
    )

    private suspend fun DefaultTextRecognitionFeature.recognizeReady(request: TextRecognitionRequest) =
        recognize(prepare(request).shouldBeInstanceOf<TextRecognitionPreparation.Ready>().recognition)
            .shouldBeInstanceOf<TextRecognitionExecution.Success>()
            .result

    private class FakeModelArtifactStore(installed: List<ModelArtifactDescriptor>) : ModelArtifactStore {
        val installed = installed.toMutableSet()

        override suspend fun installed(descriptor: ModelArtifactDescriptor): InstalledModelArtifact? =
            descriptor.takeIf { it in installed }?.let { InstalledModelArtifact(it, File("models")) }

        override fun observe(descriptor: ModelArtifactDescriptor): Flow<ModelArtifactState> = emptyFlow()

        override fun download(approval: ModelArtifactDownloadApproval) = Unit

        override fun cancel(artifact: ModelArtifactDescriptor) = Unit

        override suspend fun delete(artifact: ModelArtifactId) = Unit

        override fun observeStored(): Flow<List<StoredModelArtifact>> = emptyFlow()
    }
}
