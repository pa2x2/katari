package mihon.text.recognition.runtime

import android.graphics.Bitmap
import io.mockk.mockk
import mihon.language.api.tag.LanguageTag
import mihon.model.artifacts.api.descriptor.ModelArtifactDescriptor
import mihon.model.artifacts.api.descriptor.ModelArtifactFile
import mihon.model.artifacts.api.descriptor.ModelArtifactHosting
import mihon.model.artifacts.api.descriptor.ModelArtifactId
import mihon.model.artifacts.api.descriptor.ModelArtifactLicense
import mihon.text.recognition.api.component.KnownTextRecognitionComponent
import mihon.text.recognition.api.component.TextRecognitionComponentId
import mihon.text.recognition.api.component.TextRecognitionComponentRole
import mihon.text.recognition.api.image.ImageContentKey
import mihon.text.recognition.api.image.ImageRect
import mihon.text.recognition.api.image.ImageSize
import mihon.text.recognition.api.image.TextRecognitionImage
import mihon.text.recognition.api.pipeline.TextRecognitionPipeline
import mihon.text.recognition.api.pipeline.TextRecognitionPreset
import mihon.text.recognition.api.pipeline.TextRecognitionPresetId
import mihon.text.recognition.api.provider.KnownTextRecognitionProvider
import mihon.text.recognition.api.provider.TextRecognitionBuildAvailability
import mihon.text.recognition.api.provider.TextRecognitionProviderId
import mihon.text.recognition.spi.component.DetectedTextRegion
import mihon.text.recognition.spi.component.DetectedTextRegionKind
import mihon.text.recognition.spi.component.RecognizedCropText
import mihon.text.recognition.spi.component.TextDetector
import mihon.text.recognition.spi.component.TextRecognitionComponent
import mihon.text.recognition.spi.component.TextRecognitionComponentAvailability
import mihon.text.recognition.spi.component.TextRecognizer
import mihon.text.recognition.spi.contribution.TextRecognitionProviderContribution
import mihon.text.recognition.spi.model.TextRecognitionModels

internal val JAPANESE = LanguageTag.require("ja")

internal val EXAMPLE_PROVIDER = provider("example")
internal val EXCLUDED_PROVIDER = provider("excluded", TextRecognitionBuildAvailability.NotIncluded("Not in this build"))

internal fun provider(
    id: String,
    buildAvailability: TextRecognitionBuildAvailability = TextRecognitionBuildAvailability.Included,
) = KnownTextRecognitionProvider(
    id = TextRecognitionProviderId(id),
    name = id,
    description = "Example engine",
    processingLocation = "On this device",
    buildAvailability = buildAvailability,
)

internal fun knownComponent(
    id: String,
    role: TextRecognitionComponentRole,
    languages: Set<LanguageTag> = if (role == TextRecognitionComponentRole.Detector) emptySet() else setOf(JAPANESE),
    provider: KnownTextRecognitionProvider = EXAMPLE_PROVIDER,
) = KnownTextRecognitionComponent(
    id = TextRecognitionComponentId(id),
    provider = provider.id,
    role = role,
    displayName = id,
    description = "Example component",
    languages = languages,
)

internal fun preset(
    id: String,
    pipeline: TextRecognitionPipeline,
    languages: Set<LanguageTag> = setOf(JAPANESE),
    provider: KnownTextRecognitionProvider = EXAMPLE_PROVIDER,
) = TextRecognitionPreset(
    id = TextRecognitionPresetId(id),
    provider = provider.id,
    displayName = id,
    description = "Example preset",
    languages = languages,
    pipeline = pipeline,
)

/** A provider contribution that implements [implementations] and lists [catalogOnly] without implementing them. */
internal fun contribution(
    provider: KnownTextRecognitionProvider,
    implementations: List<TextRecognitionComponent> = emptyList(),
    catalogOnly: List<KnownTextRecognitionComponent> = emptyList(),
    presets: List<TextRecognitionPreset> = emptyList(),
    order: Int = 0,
) = TextRecognitionProviderContribution(
    provider = provider,
    components = implementations.map { it.catalogEntry } + catalogOnly,
    implementations = implementations,
    presets = presets,
    order = order,
)

internal fun model(id: String) = ModelArtifactDescriptor(
    id = ModelArtifactId(id),
    revision = "r1",
    displayName = id,
    files = listOf(ModelArtifactFile("model.onnx", "https://models.example/$id", 1, "0".repeat(64))),
    license = ModelArtifactLicense("Apache-2.0", "https://license.example"),
    hosting = ModelArtifactHosting.Upstream("https://models.example"),
)

/**
 * A page whose decoded regions remember where they came from, so fake components can answer in the coordinates of
 * the bitmap they receive.
 */
internal class FakePageImage(
    override val size: ImageSize,
    override val key: ImageContentKey = ImageContentKey("page"),
) : TextRecognitionImage {
    val decoded = mutableListOf<DecodedRegion>()

    override suspend fun decodeRegion(region: ImageRect, sampleSize: Int): Bitmap {
        val bitmap = mockk<Bitmap>(relaxed = true)
        decoded += DecodedRegion(bitmap, region, sampleSize)
        return bitmap
    }

    fun regionOf(bitmap: Bitmap): DecodedRegion = decoded.single { it.bitmap === bitmap }
}

internal data class DecodedRegion(
    val bitmap: Bitmap,
    val region: ImageRect,
    val sampleSize: Int,
)

/** Reports the page's ground-truth objects that intersect each tile, clipped to it like a real detector would. */
internal class FakeDetector(
    private val page: FakePageImage,
    private val objects: List<Pair<ImageRect, DetectedTextRegionKind>>,
    val declaredModels: List<ModelArtifactDescriptor> = emptyList(),
    override val catalogEntry: KnownTextRecognitionComponent =
        knownComponent("example.detector", TextRecognitionComponentRole.Detector),
) : TextDetector {
    override fun models(language: LanguageTag) = declaredModels

    override val inputEdge: Int = 640
    override var processingRevision = 1
    var runs = 0

    override suspend fun inspectDevice(language: LanguageTag) = TextRecognitionComponentAvailability.Available

    override suspend fun detect(tile: Bitmap, models: TextRecognitionModels): List<DetectedTextRegion> {
        runs++
        val decoded = page.regionOf(tile)
        return objects.mapNotNull { (bounds, kind) ->
            val visible = bounds.intersect(decoded.region) ?: return@mapNotNull null
            DetectedTextRegion(
                bounds = ImageRect(
                    left = (visible.left - decoded.region.left) / decoded.sampleSize,
                    top = (visible.top - decoded.region.top) / decoded.sampleSize,
                    right = (visible.right - decoded.region.left) / decoded.sampleSize,
                    bottom = (visible.bottom - decoded.region.top) / decoded.sampleSize,
                ),
                kind = kind,
                confidence = visible.area.toFloat() / bounds.area,
            )
        }
    }
}

/** Reads the text of whichever labelled source rectangle the crop covers most. */
internal class FakeRecognizer(
    private val page: FakePageImage,
    private val texts: Map<ImageRect, String>,
    val declaredModels: List<ModelArtifactDescriptor> = emptyList(),
    override val catalogEntry: KnownTextRecognitionComponent =
        knownComponent("example.recognizer", TextRecognitionComponentRole.Recognizer),
) : TextRecognizer {
    override fun models(language: LanguageTag) = declaredModels

    override val inputEdge: Int = 224
    override var processingRevision = 1
    var runs = 0

    override suspend fun inspectDevice(language: LanguageTag) = TextRecognitionComponentAvailability.Available

    override suspend fun recognize(
        crop: Bitmap,
        language: LanguageTag,
        models: TextRecognitionModels,
    ): RecognizedCropText? {
        runs++
        val region = page.regionOf(crop).region
        return texts.entries
            .maxByOrNull { (bounds, _) -> bounds.intersect(region)?.area ?: 0L }
            ?.takeIf { (bounds, _) -> bounds.intersect(region) != null }
            ?.let { RecognizedCropText(it.value) }
    }
}
