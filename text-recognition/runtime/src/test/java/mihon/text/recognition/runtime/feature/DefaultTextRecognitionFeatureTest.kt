package mihon.text.recognition.runtime.feature

import io.kotest.matchers.collections.shouldContainExactly
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
import mihon.text.recognition.api.component.TextRecognitionBuildAvailability
import mihon.text.recognition.api.component.TextRecognitionComponentRole
import mihon.text.recognition.api.image.ImageRect
import mihon.text.recognition.api.image.ImageSize
import mihon.text.recognition.api.pipeline.TextRecognitionPipeline
import mihon.text.recognition.api.pipeline.TextRecognitionPipelineSelection
import mihon.text.recognition.api.pipeline.TextRecognitionPreset
import mihon.text.recognition.api.pipeline.TextRecognitionPresetId
import mihon.text.recognition.api.preparation.TextRecognitionPipelineChoiceReason
import mihon.text.recognition.api.preparation.TextRecognitionPreparation
import mihon.text.recognition.api.request.TextRecognitionRequest
import mihon.text.recognition.api.request.TextRecognitionScope
import mihon.text.recognition.api.result.TextRecognitionExecution
import mihon.text.recognition.api.result.TextRegionKind
import mihon.text.recognition.runtime.FakeDetector
import mihon.text.recognition.runtime.FakePageImage
import mihon.text.recognition.runtime.FakeRecognizer
import mihon.text.recognition.runtime.JAPANESE
import mihon.text.recognition.runtime.cache.TextRecognitionResultCache
import mihon.text.recognition.runtime.execution.CachedRecognitionExecutor
import mihon.text.recognition.runtime.knownComponent
import mihon.text.recognition.runtime.model
import mihon.text.recognition.runtime.registry.TextRecognitionComponentRegistry
import mihon.text.recognition.runtime.selection.ProfileTextRecognitionPreferences
import mihon.text.recognition.runtime.selection.TextRecognitionPipelineResolver
import mihon.text.recognition.spi.component.DetectedTextRegionKind
import mihon.text.recognition.spi.contribution.TextRecognitionComponentContribution
import mihon.text.recognition.spi.contribution.TextRecognitionPresetContribution
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import java.io.File

class DefaultTextRecognitionFeatureTest {

    @TempDir
    lateinit var cacheDirectory: File

    // A webtoon strip read in several tiles. The middle bubble straddles the boundary between the first two tiles.
    private val strip = FakePageImage(ImageSize(900, 4000))
    private val topBubble = ImageRect(100, 100, 500, 400)
    private val topText = ImageRect(150, 150, 450, 350)
    private val middleBubble = ImageRect(400, 1000, 800, 1300)
    private val middleText = ImageRect(450, 1050, 750, 1250)
    private val narration = ImageRect(50, 3500, 850, 3600)
    private val detector = FakeDetector(
        page = strip,
        objects = listOf(
            topBubble to DetectedTextRegionKind.Bubble,
            topText to DetectedTextRegionKind.BubbleText,
            middleBubble to DetectedTextRegionKind.Bubble,
            middleText to DetectedTextRegionKind.BubbleText,
            narration to DetectedTextRegionKind.FreeText,
        ),
        models = listOf(model("example.detector-model")),
    )
    private val recognizer = FakeRecognizer(
        page = strip,
        texts = mapOf(topText to "上", middleText to "中", narration to "語り"),
        models = listOf(model("example.recognizer-model")),
    )
    private val excludedEngine = knownComponent(
        id = "excluded.engine",
        role = TextRecognitionComponentRole.Engine,
        buildAvailability = TextRecognitionBuildAvailability.NotIncluded("Not in this build"),
    )
    private val stagedPreset = TextRecognitionPreset(
        id = TextRecognitionPresetId("staged"),
        displayName = "Staged",
        description = "Detector and recognizer",
        languages = setOf(JAPANESE),
        pipeline = TextRecognitionPipeline.Staged(detector.catalogEntry.id, recognizer.catalogEntry.id),
    )
    private val excludedPreset = TextRecognitionPreset(
        id = TextRecognitionPresetId("excluded"),
        displayName = "Excluded",
        description = "Engine missing from this build",
        languages = setOf(JAPANESE),
        pipeline = TextRecognitionPipeline.Engine(excludedEngine.id),
    )
    private val registry = TextRecognitionComponentRegistry(
        contributions = listOf(
            TextRecognitionComponentContribution(detector),
            TextRecognitionComponentContribution(recognizer),
            TextRecognitionComponentContribution(catalogEntry = excludedEngine),
        ),
        presetContributions = listOf(
            TextRecognitionPresetContribution(excludedPreset, order = 0),
            TextRecognitionPresetContribution(stagedPreset, order = 10),
        ),
    )
    private val preferences = ProfileTextRecognitionPreferences(InMemoryPreferenceStore())
    private val modelStore = FakeModelArtifactStore(detector.models + recognizer.models)

    @Test
    fun `a request without a language asks for one the build can read`() = runTest {
        feature().prepare(TextRecognitionRequest(strip, language = null)) shouldBe
            TextRecognitionPreparation.LanguageRequired(listOf(JAPANESE))
    }

    @Test
    fun `the default preset skips presets whose components this build excludes`() = runTest {
        val preparation = feature().prepare(TextRecognitionRequest(strip, JAPANESE))

        preparation.shouldBeInstanceOf<TextRecognitionPreparation.Ready>().pipeline shouldBe stagedPreset.pipeline
    }

    @Test
    fun `an explicit choice of an excluded component asks for another pipeline`() = runTest {
        preferences.selection(JAPANESE).set(TextRecognitionPipelineSelection.Custom(excludedPreset.pipeline))

        val preparation = feature().prepare(TextRecognitionRequest(strip, JAPANESE))

        preparation.shouldBeInstanceOf<TextRecognitionPreparation.PipelineChoiceRequired>().reason shouldBe
            TextRecognitionPipelineChoiceReason.SelectedComponentUnavailable(excludedEngine.id)
    }

    @Test
    fun `only models that are not installed are required`() = runTest {
        modelStore.installed -= recognizer.models.single()

        feature().prepare(TextRecognitionRequest(strip, JAPANESE)) shouldBe TextRecognitionPreparation.ModelsRequired(
            language = JAPANESE,
            pipeline = stagedPreset.pipeline,
            models = recognizer.models,
        )
    }

    @Test
    fun `whole-image recognition reads every text once in reading order and in source coordinates`() = runTest {
        val feature = feature()

        val result = feature.recognizeReady(TextRecognitionRequest(strip, JAPANESE))

        result.regions.map { Triple(it.text, it.bounds, it.container) } shouldContainExactly listOf(
            Triple("上", topText, topBubble),
            Triple("中", middleText, middleBubble),
            Triple("語り", narration, null),
        )
        result.regions.map { it.kind } shouldContainExactly
            listOf(TextRegionKind.SpeechBubble, TextRegionKind.SpeechBubble, TextRegionKind.FreeText)
    }

    @Test
    fun `a repeated request is answered without running the pipeline again`() = runTest {
        val first = feature().recognizeReady(TextRecognitionRequest(strip, JAPANESE))
        val detectorRuns = detector.runs
        val recognizerRuns = recognizer.runs

        val second = feature().recognizeReady(TextRecognitionRequest(strip, JAPANESE))

        second shouldBe first
        detector.runs shouldBe detectorRuns
        recognizer.runs shouldBe recognizerRuns
    }

    @Test
    fun `an outlined area without detected text is read as a whole`() = runTest {
        val outline = ImageRect(40, 3480, 860, 3620)
        val silentDetector = FakeDetector(strip, emptyList(), models = detector.models)
        val silentRegistry = TextRecognitionComponentRegistry(
            contributions = listOf(
                TextRecognitionComponentContribution(silentDetector),
                TextRecognitionComponentContribution(recognizer),
            ),
            presetContributions = listOf(TextRecognitionPresetContribution(stagedPreset)),
        )

        val result = feature(silentRegistry).recognizeReady(
            TextRecognitionRequest(strip, JAPANESE, TextRecognitionScope.Region(outline)),
        )

        result.regions.map { it.text to it.bounds } shouldContainExactly listOf("語り" to outline)
    }

    @Test
    fun `a model removed after preparation is reported as a changed preparation`() = runTest {
        val feature = feature()
        val ready = feature.prepare(TextRecognitionRequest(strip, JAPANESE))
            .shouldBeInstanceOf<TextRecognitionPreparation.Ready>()
        modelStore.installed -= detector.models.single()

        feature.recognize(ready.recognition) shouldBe TextRecognitionExecution.PreparationChanged(
            TextRecognitionPreparation.ModelsRequired(JAPANESE, stagedPreset.pipeline, detector.models),
        )
    }

    private fun feature(registry: TextRecognitionComponentRegistry = this.registry) = DefaultTextRecognitionFeature(
        registry = registry,
        resolver = TextRecognitionPipelineResolver(registry, preferences),
        modelStore = modelStore,
        executor = CachedRecognitionExecutor(
            cache = TextRecognitionResultCache({ cacheDirectory }, maximumBytes = 1_000_000),
            ioDispatcher = Dispatchers.Unconfined,
            inferenceDispatcher = Dispatchers.Unconfined,
        ),
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
